package io.workshop.bookstore.db.model

import slick.lifted.ForeignKeyQuery

import java.util.UUID

final case class Session(userId: UUID, token: String)

import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

trait SessionTable extends UsersTable {

  class Sessions(tag: Tag) extends Table[Session](tag, "sessions") {

    val userId: Rep[UUID] = column[UUID]("userId", O.PrimaryKey)

    val token: Rep[String] = column[String]("token")

    override def * : ProvenShape[Session] = (userId, token) <> (Session.tupled, Session.unapply)

    def user: ForeignKeyQuery[Users, User] = foreignKey("user_fk", userId, users)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def idxToken = index("idx_token", token, unique = true)

  }

  val sessions = TableQuery[Sessions]
}
