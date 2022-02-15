package io.workshop.bookstore.db.repositories

import io.workshop.bookstore.db.model.{ Session, SessionTable }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.Future

trait SessionsRepository {

  def login(session: Session): Future[Int]

  def logout(userId: UUID): Future[Int]

  def findLoggedInUser(token: String): Future[Option[(UUID, String, String, Boolean)]]

  def prepareRepository(): Future[Unit]

  def dropRepository(): Future[Unit]

}

class SessionsH2Repository(db: Database) extends SessionsRepository with SessionTable {

  override def login(session: Session): Future[Int] =
    db.run(sessions += session)

  override def logout(userId: UUID): Future[Int] =
    db.run(sessions.filter(_.userId === userId).delete)

  override def findLoggedInUser(token: String): Future[Option[(UUID, String, String, Boolean)]] = {
    val query = for {
      user <- sessions.filter(_.token === token) join users on (_.userId === _.id)
    } yield (user._2.id, user._2.name, user._2.password, user._2.isAdmin)

    db.run(query.result.headOption)
  }

  override def prepareRepository(): Future[Unit] =
    db.run(sessions.schema.createIfNotExists)

  override def dropRepository(): Future[Unit] =
    db.run(sessions.schema.dropIfExists)
}
