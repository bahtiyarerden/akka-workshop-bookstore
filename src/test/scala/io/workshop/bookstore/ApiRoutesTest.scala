package io.workshop.bookstore

import akka.http.scaladsl.model.StatusCodes.{ NotFound, OK }
import akka.http.scaladsl.model.headers.{ HttpChallenge, OAuth2BearerToken }
import akka.http.scaladsl.model.{ FormData, StatusCodes }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, AuthorizationFailedRejection, Route }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.testkit.TestDuration
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.workshop.bookstore.db.repositories._
import io.workshop.bookstore.routes.ApiRoutes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfter, OptionValues }
import play.api.libs.json._
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.duration.DurationInt

class ApiRoutesTest
    extends AnyWordSpec
    with ScalatestRouteTest
    with Matchers
    with BeforeAndAfter
    with ApiRoutes
    with OptionValues {

  import PlayJsonSupport._

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(6.seconds.dilated)

  var db: Database                          = _
  var ordersRepository: OrdersRepository    = _
  var booksRepository: BooksRepository      = _
  var usersRepository: UsersRepository      = _
  var sessionRepository: SessionsRepository = _

  var apiRoutes: Route = _

  private val httpChallenge       = HttpChallenge("Bearer", "book-store")
  private val missingCredentials  = AuthenticationFailedRejection(CredentialsMissing, httpChallenge)
  private val rejectedCredentials = AuthenticationFailedRejection(CredentialsRejected, httpChallenge)

  private var userToken     = ""
  private var adminToken    = ""
  private var createdBookId = ""

  private val titleToCreate = "New book"
  private val bookToCreate  = JsObject(Map("title" -> JsString(titleToCreate)))
  private val titleToUpdate = "Updated book"
  private val bookToUpdate  = JsObject(Map("title" -> JsString(titleToUpdate)))
  private val unknownId     = UUID.randomUUID()

  private val userToCreate = FormData("grant_type" -> "password", "username" -> "user", "password" -> "user")
  private val adminToCreate =
    FormData("grant_type" -> "password", "username" -> "admin", "password" -> "admin", "isAdmin" -> "true")

  private lazy val orderToCreate = JsObject(Map("bookId" -> JsString(createdBookId)))

  before {
    db                = Database.forConfig("h2mem")
    ordersRepository  = new OrdersH2Repository(db)
    booksRepository   = new BooksH2Repository(db)
    usersRepository   = new UserH2Repository(db)
    sessionRepository = new SessionsH2Repository(db)

    usersRepository.prepareRepository()
    booksRepository.prepareRepository()
    ordersRepository.prepareRepository()
    sessionRepository.prepareRepository()

    apiRoutes = new ApiRoutes(usersRepository, booksRepository, ordersRepository).apiRoutes
  }

  override protected def afterAll(): Unit = {
    usersRepository.dropRepository()
    booksRepository.dropRepository()
    ordersRepository.dropRepository()
    sessionRepository.dropRepository()
    db.close()
  }

  "health check" in {
    Get("/health") ~> apiRoutes ~> check {
      response.status shouldEqual StatusCodes.OK
    }
  }

  "api should work well" in {
    Get("/books") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Get(s"/books/$unknownId") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Post("/books", JsObject.empty) ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Put(s"/books/$unknownId", JsObject.empty) ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Delete(s"/books/$unknownId") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Post(
      "/login",
      FormData("grant_type" -> "password", "username" -> "user", "password" -> "user")
    ) ~> apiRoutes ~> check {
      userToken = (responseAs[JsValue] \ "access_token").as[String]
      status shouldEqual OK
    }

    Post(
      "/login",
      FormData("grant_type" -> "password", "username" -> "admin", "password" -> "admin")
    ) ~> apiRoutes ~> check {
      adminToken = (responseAs[JsValue] \ "access_token").as[String]
      status shouldEqual OK
    }

    Get("/books") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      responseAs[JsArray].value.nonEmpty shouldBe true
      status shouldBe OK
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Put(s"/books/$unknownId", JsObject.empty) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Delete(s"/books/$unknownId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToCreate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Put(s"/books/$createdBookId", bookToUpdate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToUpdate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Get(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "id").as[String] shouldBe createdBookId
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToUpdate

      status shouldBe OK
    }

    Delete(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      status shouldEqual OK
    }

    Get(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      status shouldBe NotFound
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToCreate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Get("/orders") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }
    Post("/orders") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Get("/orders") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      responseAs[JsArray].value.isEmpty shouldBe true
      status shouldBe OK
    }

    Post("/orders", orderToCreate) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "bookId").as[String] shouldBe createdBookId
      status shouldBe OK
    }

    Post("/logout") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      status shouldEqual OK
    }

    Get("/orders") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe rejectedCredentials
    }
  }
}
