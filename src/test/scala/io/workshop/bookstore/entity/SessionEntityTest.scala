package io.workshop.bookstore.entity

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.model.User
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import io.workshop.bookstore.db.repositories._
import io.workshop.bookstore.entities._
import io.workshop.bookstore.entities.commands._
import io.workshop.bookstore.entities.replies._
import io.workshop.bookstore.error.{ UserSessionNotCreated, UserSessionNotDeleted }
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class SessionEntityTest
    extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  implicit val defaultPatience: PatienceConfig            = PatienceConfig(timeout = Span(3, Minutes))
  implicit lazy val timeout: Timeout                      = Timeout(5.seconds)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  var db: Database                          = _
  var usersRepository: UsersRepository      = _
  var sessionRepository: SessionsRepository = _

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db                = Database.forConfig("h2mem", config)
    usersRepository   = new UserH2Repository(db)
    sessionRepository = new SessionsH2Repository(db)
    usersRepository.prepareRepository().futureValue
    sessionRepository.prepareRepository().futureValue
  }

  override protected def afterEach(): Unit = {
    sessionRepository.dropRepository().futureValue
    usersRepository.dropRepository().futureValue
    db.close()
  }

  "Session Entity" must {
    "login user" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      // execute
      val effectedRows = (sessionActor ? CreateUserSession(registeredUser.id, "token")).mapTo[Int].futureValue

      // assert
      assert(effectedRows == 1)
    }

    "logout user" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      (sessionActor ? CreateUserSession(registeredUser.id, "token")).mapTo[Int].futureValue

      // execute
      val effectedRows = (sessionActor ? DeleteUserSession(registeredUser.id)).mapTo[Int].futureValue

      // assert
      assert(effectedRows == 1)
    }

    "find logged in user" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val token        = "token"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      (sessionActor ? CreateUserSession(registeredUser.id, "token")).mapTo[Int].futureValue

      // execute
      val userSession = (sessionActor ? FindUserSession(token)).mapTo[Option[User]].futureValue

      assert(userSession.get == User(registeredUser.id, userName, password, isAdmin))
    }

    "not find not logged in user" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      (sessionActor ? CreateUserSession(registeredUser.id, "token")).mapTo[Int].futureValue

      // execute
      val userSession = (sessionActor ? FindUserSession("some other token")).mapTo[Option[User]].futureValue

      assert(userSession.isEmpty)
    }

    "throws error on create session for user already logged in" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val token        = "token"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      (sessionActor ? CreateUserSession(registeredUser.id, token)).mapTo[Int].futureValue

      val notCreatedSession = sessionActor ? CreateUserSession(registeredUser.id, token)

      whenReady(notCreatedSession.failed) { error =>
        error shouldBe a[UserSessionNotCreated]
        assert(error.asInstanceOf[UserSessionNotCreated].getMessage == "User session could not be created")
      }
    }

    "throws error on delete session for user not logged in" in {
      // setup
      val userName     = "Bahtiyar"
      val password     = "SuperSecret!"
      val isAdmin      = true
      val userActor    = system.actorOf(UserEntity.props(usersRepository))
      val sessionActor = system.actorOf(SessionEntity.props(sessionRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue

      val notCreatedSession = sessionActor ? DeleteUserSession(registeredUser.id)

      whenReady(notCreatedSession.failed) { error =>
        error shouldBe a[UserSessionNotDeleted]
        assert(error.asInstanceOf[UserSessionNotDeleted].getMessage == "User session could not be deleted")
      }
    }
  }
}
