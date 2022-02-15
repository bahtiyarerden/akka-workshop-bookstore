package io.workshop.bookstore.entities.commands

import java.util.UUID

sealed trait SessionCommands

case class CreateUserSession(userId: UUID, token: String) extends SessionCommands

case class DeleteUserSession(userId: UUID) extends SessionCommands

case class FindUserSession(token: String) extends SessionCommands
