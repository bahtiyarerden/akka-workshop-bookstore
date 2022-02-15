package io.workshop.bookstore.entities.replies

import java.util.UUID

sealed trait OrderEntityReplies

case class OrderIdReply(id: UUID) extends OrderEntityReplies
