package io.workshop.bookstore.routes.auth

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ Cause, CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.SecurityDirectives.authorize
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive0, Directive1 }
import akka.pattern.ask
import akka.util.Timeout
import io.workshop.bookstore.db.model.User
import io.workshop.bookstore.entities.SessionEntity
import io.workshop.bookstore.entities.commands.FindUserSession
import io.workshop.bookstore.Application.{ actorSystem, sessionsRepository }
import scala.concurrent.duration._

object AuthProvider {
  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  val sessionEntity: ActorRef        = actorSystem.actorOf(SessionEntity.props(sessionsRepository))

  def authorized(user: User): Directive0 =
    authorize(user.isAdmin)

  def authenticated: Directive1[User] =
    for {
      credentials <- extractCredentials
      result <- {
        credentials match {
          case Some(c) if c.scheme.equalsIgnoreCase("Bearer") => authenticate(c.token)
          case _                                              => rejectUnauthenticated(CredentialsMissing)
        }
      }
    } yield result

  private def authenticate(token: String): Directive1[User] = {
    val result = (sessionEntity ? FindUserSession(token)).mapTo[Option[User]]
    onSuccess(result).flatMap {
      case Some(user) => provide(user)
      case None       => rejectUnauthenticated(CredentialsRejected)
    }
  }

  private def rejectUnauthenticated(cause: Cause): Directive1[User] =
    reject(AuthenticationFailedRejection(cause, HttpChallenge("Bearer", "book-store")))

  def isAuthorized(user: User): Boolean =
    user.isAdmin

}
