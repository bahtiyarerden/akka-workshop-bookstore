package io.workshop.bookstore.routes.requests

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait OrderRequests

case class CreateOrderRequest(bookId: UUID) extends OrderRequests

object CreateOrderRequest {
  implicit val format: Format[CreateOrderRequest] = Json.format
}
