package io.workshop.bookstore.db.model

import slick.jdbc.H2Profile.api._
import slick.lifted.{ ForeignKeyQuery, ProvenShape }

import java.util.UUID

final case class Order(id: UUID, bookId: UUID, userId: UUID)

trait OrdersTable extends UsersTable with BooksTable {

  class Orders(tag: Tag) extends Table[Order](tag, "orders") {

    val id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

    val userId: Rep[UUID] = column[UUID]("userId")

    val bookId: Rep[UUID] = column[UUID]("bookId")

    override def * : ProvenShape[Order] = (id, userId, bookId) <> (Order.tupled, Order.unapply)

    def user: ForeignKeyQuery[Users, User] = foreignKey("user_fk", userId, users)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def book: ForeignKeyQuery[Books, Book.Internal] = foreignKey("book_fk", bookId, books)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

  }

  val orders = TableQuery[Orders]

}
