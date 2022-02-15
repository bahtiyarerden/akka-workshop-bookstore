package io.workshop.bookstore.entities.replies

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait UserEntityReplies

case class UserIdReply(id: UUID) extends UserEntityReplies

object UserIdReply {
  implicit val format: Format[UserIdReply] = Json.format
}

case class UserAuthReply(id: UUID, isAdmin: Boolean) extends UserEntityReplies

object UserAuthReply {
  implicit val format: Format[UserAuthReply] = Json.format
}
