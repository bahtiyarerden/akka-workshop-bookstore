package io.workshop.bookstore.db.repositories

import io.workshop.bookstore.db.model.Book.Internal
import io.workshop.bookstore.db.model.BooksTable
import slick.basic.DatabasePublisher
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.Future

trait BooksRepository {
  def create(book: Internal): Future[Int]

  def get(id: UUID): Future[Option[Internal]]

  def getAll: DatabasePublisher[Internal]

  def update(book: Internal): Future[Int]

  def delete(id: UUID): Future[Int]

  def prepareRepository(): Future[Unit]

  def dropRepository(): Future[Unit]

}

class BooksH2Repository(db: Database) extends BooksRepository with BooksTable {
  override def create(book: Internal): Future[Int] =
    db.run(books += book)

  override def get(id: UUID): Future[Option[Internal]] =
    db.run(books.filter(_.id === id).result.headOption)

  override def getAll: DatabasePublisher[Internal] =
    db.stream(books.result)

  override def update(book: Internal): Future[Int] =
    db.run(
      books
        .filter(_.id === book.id)
        .map(b => b.title)
        .update(book.title)
    )

  override def delete(id: UUID): Future[Int] =
    db.run(books.filter(_.id === id).delete)

  override def prepareRepository(): Future[Unit] =
    db.run(books.schema.createIfNotExists)

  override def dropRepository(): Future[Unit] =
    db.run(books.schema.dropIfExists)
}
