package io.workshop.bookstore.entity

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.repositories._
import io.workshop.bookstore.entities._
import io.workshop.bookstore.entities.commands._
import io.workshop.bookstore.entities.replies._
import io.workshop.bookstore.error._
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, TryValues }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

class UserEntityTest
    extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with TryValues {

  implicit val defaultPatience: PatienceConfig            = PatienceConfig(timeout = Span(3, Minutes))
  implicit lazy val timeout: Timeout                      = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  var db: Database                                        = _
  var usersRepository: UsersRepository                    = _

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db              = Database.forConfig("h2mem", config)
    usersRepository = new UserH2Repository(db)
    usersRepository.prepareRepository().futureValue
  }

  override protected def afterEach(): Unit = {
    usersRepository.dropRepository().futureValue
    db.close()
  }

  "User Entity" must {
    "register the user" in {
      // setup
      val userName    = "Bahtiyar"
      val password    = "SuperSecret!"
      val isAdmin     = true
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      // execute
      val registeredUser = (entityActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      registeredUser should matchPattern { case UserIdReply(_) => }
    }

    "login the user" in {
      // setup
      val userName    = "Bahtiyar"
      val password    = "SuperSecret!"
      val isAdmin     = true
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      // execute
      val registeredUser: UserIdReply =
        (entityActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      val loggedInUser: UserAuthReply = (entityActor ? LoginUser(userName, password)).mapTo[UserAuthReply].futureValue

      assert(loggedInUser == UserAuthReply(registeredUser.id, isAdmin))
    }

    "throw exception on failed register" in {
      // setup
      val userName    = "Bahtiyar"
      val password    = "SuperSecret!"
      val isAdmin     = true
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      (entityActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      // execute
      val registeredUser = entityActor ? RegisterUser(userName, password, isAdmin)

      whenReady(registeredUser.failed) { error =>
        error shouldBe a[UserNotRegisteredError]
        assert(error.asInstanceOf[UserNotRegisteredError].getMessage == "User could not be created")
      }
    }

    "throw exception on failed login on empty repository" in {
      // setup
      val userName    = "Bahtiyar"
      val password    = "SuperSecret!"
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      // execute
      val loginUserFuture = entityActor ? LoginUser(userName, password)

      whenReady(loginUserFuture.failed) { error =>
        error shouldBe a[WrongCredentialsError]
        assert(
          error
            .asInstanceOf[WrongCredentialsError]
            .getMessage == s"We could not find your account with username: $userName"
        )
      }
    }

    "throw exception on login with wrong credentials" in {
      // setup
      val userName    = "Bahtiyar"
      val password    = "SuperSecret!"
      val isAdmin     = true
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      (entityActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      // execute

      recoverToSucceededIf[WrongCredentialsError] {
        entityActor ? LoginUser(userName, "Not my pass")
      }
    }

    "throw exception on login on username not match" in {
      // setup
      val userName    = "Bahtiyar"
      val notUserName = "NotUser"
      val password    = "SuperSecret!"
      val isAdmin     = true
      val entityActor = system.actorOf(UserEntity.props(usersRepository))

      (entityActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      // execute
      recoverToSucceededIf[WrongCredentialsError] {
        entityActor ? LoginUser(notUserName, "Not my pass")
      }
    }
  }
}
