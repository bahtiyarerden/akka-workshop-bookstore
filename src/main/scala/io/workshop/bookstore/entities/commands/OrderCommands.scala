package io.workshop.bookstore.entities.commands

import java.util.UUID

sealed trait OrderCommands

final case class CreateOrder(userId: UUID, bookId: UUID) extends OrderCommands
final case class GetOrders(userId: UUID) extends OrderCommands
