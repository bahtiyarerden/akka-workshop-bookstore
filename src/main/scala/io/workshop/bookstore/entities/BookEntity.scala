package io.workshop.bookstore.entities

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.workshop.bookstore.db.model.Book.Internal
import io.workshop.bookstore.db.repositories.BooksRepository
import io.workshop.bookstore.entities.commands.{ CreateBook, DeleteBook, GetBook, UpdateBook }
import io.workshop.bookstore.entities.replies.{ BookIdReply, BookReply }
import io.workshop.bookstore.error.{
  BookNotCreatedError,
  BookNotDeletedError,
  BookNotFoundError,
  BookNotGetError,
  BookNotUpdatedError
}

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

object BookEntity {
  def props(booksRepository: BooksRepository): Props = Props(new BookEntity(booksRepository))
}

class BookEntity(val booksRepository: BooksRepository) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case CreateBook(title) =>
      val id = UUID.randomUUID()
      booksRepository
        .create(Internal(id, title))
        .map { effectedRows =>
          if (effectedRows == 1) BookIdReply(id) else throw BookNotCreatedError("Book could not be created")
        }
        .recover { case ex =>
          throw BookNotCreatedError("Book could not be created", ex)
        }
        .pipeTo(sender())
    case GetBook(id) =>
      val result: Future[BookReply] = for {
        bookOption <- booksRepository.get(id)
      } yield bookOption match {
        case Some(book) => BookReply(book.id, book.title)
        case None       => throw BookNotFoundError(s"Book does not exist with id: $id")
      }
      result
        .pipeTo(sender())
    case UpdateBook(id, title) =>
      booksRepository
        .update(Internal(id, title))
        .map { effectedRows =>
          if (effectedRows == 1) effectedRows else throw BookNotUpdatedError("Book could not be updated")
        }
        .pipeTo(sender())
    case DeleteBook(id) =>
      booksRepository
        .delete(id)
        .map { effectedRows =>
          if (effectedRows == 1) effectedRows else throw BookNotDeletedError("Book could not be deleted")
        }
        .pipeTo(sender())
  }
}
