package io.workshop.bookstore.routes

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.workshop.bookstore.db.repositories.UsersRepository
import io.workshop.bookstore.entities.UserEntity
import io.workshop.bookstore.entities.commands.{ CreateUserSession, DeleteUserSession, LoginUser, RegisterUser }
import io.workshop.bookstore.entities.replies.{ UserAuthReply, UserIdReply }
import io.workshop.bookstore.error.BookStoreError.exceptionHandler
import io.workshop.bookstore.routes.auth.AuthProvider.{ authenticated, sessionEntity }
import io.workshop.bookstore.routes.auth.TokenGenerator
import io.workshop.bookstore.routes.responses.{ TokenResponse, UserIdResponse }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._

class UserRoutes(usersRepository: UsersRepository)(implicit system: ActorSystem) extends PlayJsonSupport {
  implicit lazy val timeout: Timeout                      = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val userEntity: ActorRef                                = system.actorOf(UserEntity.props(usersRepository))

  val route: Route = concat(
    path("register") {
      handleExceptions(exceptionHandler) {
        post {
          formFields("grant_type", "username", "password", "isAdmin".as[Boolean] ? false) {
            (_, username, password, isAdmin) =>
              val userRegistered: Future[UserIdReply] =
                (userEntity ? RegisterUser(username, password, isAdmin)).mapTo[UserIdReply]

              onSuccess(userRegistered) { registered =>
                complete(StatusCodes.Created, UserIdResponse(registered.id))
              }
          }
        }
      }
    },
    path("login") {
      handleExceptions(exceptionHandler) {
        post {
          formFields("grant_type", "username", "password") { (_, username, password) =>
            val userToken = for {
              userLoggedIn <- (userEntity ? LoginUser(username, password)).mapTo[UserAuthReply]
              token        <- Future.successful(TokenGenerator.generateSHAToken(username))
              _            <- sessionEntity ? CreateUserSession(userLoggedIn.id, token)
            } yield token

            onSuccess(userToken) { token =>
              complete(StatusCodes.OK, TokenResponse(token))
            }
          }
        }
      }
    },
    path("logout") {
      authenticated { user =>
        handleExceptions(exceptionHandler) {
          post {
            val reply = sessionEntity ? DeleteUserSession(user.id)
            onSuccess(reply) { _ =>
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }
  )
}
