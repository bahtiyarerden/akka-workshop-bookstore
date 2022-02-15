package io.workshop.bookstore.entities

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.workshop.bookstore.db.model.Order
import io.workshop.bookstore.db.repositories.OrdersRepository
import io.workshop.bookstore.entities.commands.CreateOrder
import io.workshop.bookstore.entities.replies.OrderIdReply
import io.workshop.bookstore.error.OrderNotCreatedError

import java.util.UUID
import scala.concurrent.ExecutionContext

object OrderEntity {
  def props(ordersRepository: OrdersRepository): Props = Props(new OrderEntity(ordersRepository))
}

class OrderEntity(val ordersRepository: OrdersRepository) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = { case CreateOrder(userId, bookId) =>
    val id = UUID.randomUUID()
    ordersRepository
      .create(Order(id, userId, bookId))
      .map { effectedRows =>
        if (effectedRows == 1) OrderIdReply(id) else throw OrderNotCreatedError("Order could not be created")
      }
      .pipeTo(sender())
  }
}
