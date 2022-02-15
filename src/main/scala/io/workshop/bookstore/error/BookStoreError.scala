package io.workshop.bookstore.error

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

sealed trait BookStoreError

final case class UserNotRegisteredError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class UserNotLoggedInError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class WrongCredentialsError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class OrderNotCreatedError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class OrderNotGetError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class BookNotCreatedError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class BookNotGetError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class BookNotFoundError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class BookNotUpdatedError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class BookNotDeletedError(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class UserSessionNotCreated(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class UserSessionNotDeleted(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

final case class UserSessionFindFailure(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
    with BookStoreError

object BookStoreError {
  def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case err: BookNotFoundError =>
        extractUri { _ =>
          complete(HttpResponse(StatusCodes.NotFound, entity = err.getMessage))
        }
      case err: BookStoreError =>
        extractUri { _ =>
          complete(HttpResponse(StatusCodes.BadRequest, entity = err.getMessage))
        }
      case _ =>
        extractUri { _ =>
          complete(
            HttpResponse(
              StatusCodes.InternalServerError,
              entity = "Something went wrong. We will solve it as soon as possible"
            )
          )
        }
    }
}
