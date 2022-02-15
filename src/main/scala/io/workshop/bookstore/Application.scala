package io.workshop.bookstore

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import io.workshop.bookstore.db.repositories._
import io.workshop.bookstore.routes.ApiRoutes
import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object Application extends LazyLogging with ApiRoutes {
  implicit val actorSystem: ActorSystem           = ActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  val log: Logger                                 = LoggerFactory.getLogger(Application.getClass)
  val db                                          = Database.forConfig("h2mem")

  val usersRepository: UsersRepository       = new UserH2Repository(db)
  val booksRepository: BooksRepository       = new BooksH2Repository(db)
  val ordersRepository: OrdersRepository     = new OrdersH2Repository(db)
  val sessionsRepository: SessionsRepository = new SessionsH2Repository(db)

  def main(args: Array[String]): Unit =
    for {
      _ <- usersRepository.prepareRepository()
      _ <- booksRepository.prepareRepository()
      _ <- ordersRepository.prepareRepository()
      _ <- sessionsRepository.prepareRepository()
    } yield {
      val routes = new ApiRoutes(usersRepository, booksRepository, ordersRepository).apiRoutes
      for {
        _ <- Http().newServerAt("0.0.0.0", 8080).bindFlow(routes).andThen {
          case Success(bind) =>
            logger.info(s"Started HTTP server on [${bind.localAddress}]")
          case Failure(err) =>
            logger.error("Could not start HTTP server", err)
            Thread.sleep(1000)
            System.exit(1)
        }
      } yield Done
    }
}
