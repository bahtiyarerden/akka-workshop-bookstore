package io.workshop.bookstore.routes

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.scaladsl.{ Merge, Source }
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.workshop.bookstore.db.repositories.BooksRepository
import io.workshop.bookstore.entities.BookEntity
import io.workshop.bookstore.entities.commands.{ CreateBook, DeleteBook, GetBook, UpdateBook }
import io.workshop.bookstore.entities.replies.{ BookIdReply, BookReply }
import io.workshop.bookstore.error.BookStoreError.exceptionHandler
import io.workshop.bookstore.routes.auth.AuthProvider.{ authenticated, authorized }
import io.workshop.bookstore.routes.requests.{ CreateBookRequest, UpdateBookRequest }
import io.workshop.bookstore.routes.responses.BookResponse
import io.workshop.bookstore.services.StorageService

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

class BookRoutes(booksRepository: BooksRepository)(implicit system: ActorSystem) extends PlayJsonSupport {
  implicit lazy val timeout: Timeout                      = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val bookEntity: ActorRef                                = system.actorOf(BookEntity.props(booksRepository))

  val route: Route =
    pathPrefix("books") {
      authenticated { token =>
        concat(
          pathEnd {
            concat(
              get {
                handleExceptions(exceptionHandler) {
                  val internalSource =
                    Source.fromPublisher(booksRepository.getAll).map(b => BookResponse(b.id, b.title))
                  val storage1Source = StorageService.getBooksFromStorage1.map(b => BookResponse(b.id, b.title))
                  val storage2Source = Source
                    .future(StorageService.getBooksFromStorage2)
                    .mapConcat(identity)
                    .map(b => BookResponse(b.id, b.title))
                  val combined: Source[BookResponse, NotUsed] =
                    Source.combine(internalSource, storage1Source, storage2Source)(Merge(_))
                  complete(combined)
                }
              },
              post {
                authorized(token) {
                  handleExceptions(exceptionHandler) {
                    entity(as[CreateBookRequest]) { book =>
                      val updateBook: Future[BookResponse] = (bookEntity ? CreateBook(book.title))
                        .mapTo[BookIdReply]
                        .map(b => BookResponse(b.id, book.title))
                      complete(StatusCodes.OK, updateBook)
                    }
                  }
                }
              }
            )
          },
          path(JavaUUID) { id =>
            concat(
              get {
                handleExceptions(exceptionHandler) {
                  val getBook: Future[BookResponse] =
                    (bookEntity ? GetBook(id)).mapTo[BookReply].map(b => BookResponse(b.id, b.title))
                  complete(StatusCodes.OK, getBook)
                }
              },
              put {
                authorized(token) {
                  handleExceptions(exceptionHandler) {
                    entity(as[UpdateBookRequest]) { book =>
                      val updateBook: Future[BookResponse] =
                        (bookEntity ? UpdateBook(id, book.title)).mapTo[Int].map(_ => BookResponse(id, book.title))
                      complete(StatusCodes.OK, updateBook)
                    }
                  }
                }
              },
              delete {
                authorized(token) {
                  handleExceptions(exceptionHandler) {
                    val deleteBook: Future[Int] = (bookEntity ? DeleteBook(id)).mapTo[Int]
                    complete(StatusCodes.OK, deleteBook)
                  }
                }
              }
            )
          }
        )
      }
    }
}
