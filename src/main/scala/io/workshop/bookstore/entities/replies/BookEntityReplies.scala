package io.workshop.bookstore.entities.replies

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait BookEntityReplies
case class BookIdReply(id: UUID) extends BookEntityReplies

object BookIdReply {
  implicit val format: Format[BookIdReply] = Json.format
}

case class BookReply(id: UUID, title: String) extends BookEntityReplies

object BookReply {
  implicit val format: Format[BookReply] = Json.format
}
