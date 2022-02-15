package io.workshop.bookstore.db.model

import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

import java.util.UUID

final case class User(id: UUID, name: String, password: String, isAdmin: Boolean)

trait UsersTable {

  class Users(tag: Tag) extends Table[User](tag, "users") {

    val id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

    val name: Rep[String] = column[String]("name")

    val password: Rep[String] = column[String]("password")

    val isAdmin: Rep[Boolean] = column[Boolean]("isAdmin")

    override def * : ProvenShape[User] = (id, name, password, isAdmin) <> (User.tupled, User.unapply)

    def idxName = index("idx_name", name, unique = true)
  }

  val users = TableQuery[Users]

}
