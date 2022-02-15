package io.workshop.bookstore.services

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.workshop.bookstore.db.model.Book
import io.workshop.bookstore.db.model.Book.External

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

object StorageService {
  private val books1: List[External] =
    (1 to 3).map(x => Book.External(UUID.randomUUID(), s"Book$x", "Storage1")).toList
  private val books2: List[Book.External] =
    (4 to 6).map(x => Book.External(UUID.randomUUID(), s"Book$x", "Storage2")).toList

  def getBooksFromStorage1: Source[Book.External, NotUsed] = Source(books1)
  def getBooksFromStorage2(implicit executionContext: ExecutionContext): Future[List[Book.External]] =
    Future {
      Thread.sleep(5000)
      books2
    }

}
