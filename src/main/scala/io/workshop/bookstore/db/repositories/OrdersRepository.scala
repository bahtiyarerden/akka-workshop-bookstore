package io.workshop.bookstore.db.repositories

import io.workshop.bookstore.db.model.{ Order, OrdersTable }
import slick.basic.DatabasePublisher
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.Future

trait OrdersRepository {
  def create(order: Order): Future[Int]

  def get(userId: UUID): DatabasePublisher[Order]

  def getAll: DatabasePublisher[Order]

  def prepareRepository(): Future[Unit]

  def dropRepository(): Future[Unit]

}

class OrdersH2Repository(db: Database) extends OrdersRepository with OrdersTable {
  override def create(order: Order): Future[Int] =
    db.run(orders += order)

  override def get(userId: UUID): DatabasePublisher[Order] =
    db.stream(orders.filter(_.userId === userId).result)

  override def getAll: DatabasePublisher[Order] =
    db.stream(orders.result)

  override def prepareRepository(): Future[Unit] =
    db.run(orders.schema.createIfNotExists)

  override def dropRepository(): Future[Unit] =
    db.run(orders.schema.dropIfExists)
}
