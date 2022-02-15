package io.workshop.bookstore.entities

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe
import io.workshop.bookstore.db.model.{ Session, User }
import io.workshop.bookstore.db.repositories.SessionsRepository
import io.workshop.bookstore.entities.commands.{ CreateUserSession, DeleteUserSession, FindUserSession }
import io.workshop.bookstore.error.{ UserSessionFindFailure, UserSessionNotCreated, UserSessionNotDeleted }

import java.util.UUID
import scala.concurrent.ExecutionContext

object SessionEntity {
  def props(sessionsRepository: SessionsRepository): Props = Props(new SessionEntity(sessionsRepository))
}

class SessionEntity(val sessionsRepository: SessionsRepository) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case CreateUserSession(userId: UUID, token: String) =>
      sessionsRepository
        .login(Session(userId, token))
        .map { effectedRows =>
          if (effectedRows == 1) effectedRows else throw UserSessionNotCreated("User session could not be created")
        }
        .recover { case ex => throw UserSessionNotCreated("User session could not be created", ex) }
        .pipeTo(sender())
    case DeleteUserSession(userId: UUID) =>
      sessionsRepository
        .logout(userId)
        .map { effectedRows =>
          if (effectedRows == 1) effectedRows else throw UserSessionNotDeleted("User session could not be deleted")
        }
        .recover { case ex =>
          throw UserSessionNotDeleted("User session could not be deleted", ex)
        }
        .pipeTo(sender())
    case FindUserSession(token: String) =>
      sessionsRepository
        .findLoggedInUser(token)
        .map {
          case Some((id, name, password, isAdmin)) => Some(User(id, name, password, isAdmin))
          case None                                => None
        }
        .recover { case ex =>
          throw UserSessionFindFailure("User session could not be fetch", ex)
        }
        .pipeTo(sender())
  }
}
