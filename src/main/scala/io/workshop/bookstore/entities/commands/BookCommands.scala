package io.workshop.bookstore.entities.commands

import java.util.UUID

sealed trait BookCommands

final case class CreateBook(title: String) extends BookCommands

final case class UpdateBook(id: UUID, title: String) extends BookCommands

final case class DeleteBook(id: UUID) extends BookCommands

final case class GetBook(id: UUID) extends BookCommands
