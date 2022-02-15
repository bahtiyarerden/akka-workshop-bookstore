package io.workshop.bookstore.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ as, complete, concat, entity, get, handleExceptions, path, post, _ }
import akka.http.scaladsl.server.{ Route, StandardRoute }
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.workshop.bookstore.db.model.Order
import io.workshop.bookstore.db.repositories.OrdersRepository
import io.workshop.bookstore.entities.OrderEntity
import io.workshop.bookstore.entities.commands.CreateOrder
import io.workshop.bookstore.entities.replies.OrderIdReply
import io.workshop.bookstore.error.BookStoreError.exceptionHandler
import io.workshop.bookstore.routes.auth.AuthProvider.{ authenticated, isAuthorized }
import io.workshop.bookstore.routes.requests.CreateOrderRequest
import io.workshop.bookstore.routes.responses.OrderResponse
import slick.basic.DatabasePublisher
import scala.concurrent.duration._

import scala.concurrent.{ ExecutionContextExecutor, Future }

class OrderRoutes(ordersRepository: OrdersRepository)(implicit system: ActorSystem) extends PlayJsonSupport {
  val orderEntity: ActorRef                               = system.actorOf(OrderEntity.props(ordersRepository))
  implicit lazy val timeout: Timeout                      = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def completeStreaming(getOrders: () => DatabasePublisher[Order]): StandardRoute = {
    val internalSource = Source.fromPublisher(getOrders()).map(o => OrderResponse(o.id, o.userId, o.bookId))
    complete(internalSource)
  }

  val route: Route = path("orders") {
    authenticated { token =>
      concat(
        get {
          handleExceptions(exceptionHandler) {
            if (isAuthorized(token)) {
              completeStreaming(() => ordersRepository.getAll)
            } else {
              completeStreaming(() => ordersRepository.get(token.id))
            }
          }
        },
        post {
          handleExceptions(exceptionHandler) {
            entity(as[CreateOrderRequest]) { order =>
              val reply: Future[OrderResponse] = (orderEntity ? CreateOrder(token.id, order.bookId))
                .mapTo[OrderIdReply]
                .map(r => OrderResponse(r.id, token.id, order.bookId))
              complete(StatusCodes.OK, reply)
            }
          }
        }
      )
    }
  }
}
