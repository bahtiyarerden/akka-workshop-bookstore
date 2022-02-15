package io.workshop.bookstore.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ complete, path }
import akka.http.scaladsl.server.Route

object HealthRoutes {
  val route: Route = path("health")(complete(StatusCodes.OK))
}
