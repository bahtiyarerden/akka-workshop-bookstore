package io.workshop.bookstore.routes.responses

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait BookResponses
final case class BookResponse(id: UUID, title: String) extends BookResponses
object BookResponse {
  implicit val format: Format[BookResponse] = Json.format
}
