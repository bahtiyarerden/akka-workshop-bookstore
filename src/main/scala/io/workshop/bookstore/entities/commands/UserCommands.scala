package io.workshop.bookstore.entities.commands

sealed trait UserCommands
final case class RegisterUser(username: String, password: String, isAdmin: Boolean) extends UserCommands

final case class LoginUser(username: String, password: String) extends UserCommands
