package io.workshop.bookstore.routes.requests

import play.api.libs.json.{ Format, Json }

sealed trait BookRequests

case class CreateBookRequest(title: String) extends BookRequests

object CreateBookRequest {
  implicit val format: Format[CreateBookRequest] = Json.format
}

case class UpdateBookRequest(title: String) extends BookRequests

object UpdateBookRequest {
  implicit val format: Format[UpdateBookRequest] = Json.format
}
