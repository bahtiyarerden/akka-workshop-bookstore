package io.workshop.bookstore.db.model

import io.workshop.bookstore.db.model.Book.Internal
import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

import java.util.UUID

sealed trait Book

object Book {

  final case class Internal(id: UUID, title: String) extends Book

  final case class External(id: UUID, title: String, storage: String) extends Book

}

trait BooksTable {

  class Books(tag: Tag) extends Table[Internal](tag, "books") {

    val id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

    val title: Rep[String] = column[String]("title")

    override def * : ProvenShape[Internal] = (id, title) <> (Internal.tupled, Internal.unapply)
  }

  val books = TableQuery[Books]

}
