package io.workshop.bookstore.db.repositories

import io.workshop.bookstore.db.model.{ User, UsersTable }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.Future

trait UsersRepository {
  def register(user: User): Future[Int]

  def get(id: UUID): Future[Option[User]]

  def getByName(name: String): Future[Option[User]]

  def prepareRepository(): Future[Unit]

  def dropRepository(): Future[Unit]
}

class UserH2Repository(db: Database) extends UsersRepository with UsersTable {
  override def register(user: User): Future[Int] =
    db.run(users += user)

  override def get(id: UUID): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  override def getByName(name: String): Future[Option[User]] =
    db.run(users.filter(_.name === name).result.headOption)

  override def prepareRepository(): Future[Unit] = {
    val setup = DBIO.seq(
      users.schema.createIfNotExists,
      users ++= Seq(
        User(UUID.randomUUID(), "user", "user", isAdmin   = false),
        User(UUID.randomUUID(), "admin", "admin", isAdmin = true)
      )
    )

    db.run(setup)
  }

  override def dropRepository(): Future[Unit] =
    db.run(users.schema.dropIfExists)
}
