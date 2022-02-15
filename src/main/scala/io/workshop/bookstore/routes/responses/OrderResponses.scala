package io.workshop.bookstore.routes.responses

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait OrderResponses

final case class OrderResponse(id: UUID, userId: UUID, bookId: UUID) extends OrderResponses

object OrderResponse {
  implicit val format: Format[OrderResponse] = Json.format
}

final case class OrderIdResponse(id: UUID) extends OrderResponses

object OrderIdResponse {
  implicit val format: Format[OrderIdResponse] = Json.format
}
