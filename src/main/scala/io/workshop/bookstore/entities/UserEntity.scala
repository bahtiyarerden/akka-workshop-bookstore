package io.workshop.bookstore.entities

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.workshop.bookstore.db.model.User
import io.workshop.bookstore.db.repositories.UsersRepository
import io.workshop.bookstore.entities.commands.{ LoginUser, RegisterUser }
import io.workshop.bookstore.entities.replies.{ UserAuthReply, UserIdReply }
import io.workshop.bookstore.error.{ UserNotLoggedInError, UserNotRegisteredError, WrongCredentialsError }

import java.util.UUID
import scala.concurrent.ExecutionContext

object UserEntity {
  def props(usersRepository: UsersRepository): Props = Props(new UserEntity(usersRepository))
}

class UserEntity(val usersRepository: UsersRepository) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case RegisterUser(username, password, isAdmin) =>
      val id = UUID.randomUUID()
      usersRepository
        .register(User(id, username, password, isAdmin))
        .map { effectedRows =>
          if (effectedRows == 1) UserIdReply(id) else throw UserNotRegisteredError("User could not be created")
        }
        .recover { case ex =>
          throw UserNotRegisteredError("User could not be created", ex)
        }
        .pipeTo(sender())
    case LoginUser(username, password) =>
      usersRepository
        .getByName(username)
        .map {
          case Some(foundUser) =>
            if (foundUser.password == password) UserAuthReply(foundUser.id, foundUser.isAdmin)
            else throw WrongCredentialsError("Name and password does not match")
          case None => throw WrongCredentialsError(s"We could not find your account with username: $username")
        }
        .pipeTo(sender())
  }
}
