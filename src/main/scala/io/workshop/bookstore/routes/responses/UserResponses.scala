package io.workshop.bookstore.routes.responses

import play.api.libs.json.{ Format, Json }

import java.util.UUID

sealed trait UserResponses

final case class TokenResponse(access_token: String) extends UserResponses

object TokenResponse {
  implicit val format: Format[TokenResponse] = Json.format[TokenResponse]
}

final case class UserIdResponse(id: UUID) extends UserResponses

object UserIdResponse {
  implicit val format: Format[UserIdResponse] = Json.format[UserIdResponse]
}
