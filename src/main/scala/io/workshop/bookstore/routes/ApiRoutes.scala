package io.workshop.bookstore.routes

import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import io.workshop.bookstore.Application.actorSystem
import io.workshop.bookstore.db.repositories._

trait ApiRoutes {

  class ApiRoutes(
    usersRepository: UsersRepository,
    booksRepository: BooksRepository,
    ordersRepository: OrdersRepository
  ) {
    private val userRouter  = new UserRoutes(usersRepository)
    private val bookRouter  = new BookRoutes(booksRepository)
    private val orderRouter = new OrderRoutes(ordersRepository)

    def apiRoutes: Route = concat(HealthRoutes.route, userRouter.route, bookRouter.route, orderRouter.route)
  }

}
