package io.workshop.bookstore.repository

import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.model.{ Session, User }
import io.workshop.bookstore.db.repositories._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterEach, Inside }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.util.Success

class SessionRepositoryTest extends AnyWordSpec with Matchers with Inside with BeforeAndAfterEach with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  var db: Database                           = _
  var sessionsRepository: SessionsRepository = _
  var usersRepository: UsersRepository       = _

  val userId: UUID             = UUID.randomUUID()
  val superSecretToken: String = UUID.randomUUID().toString
  val userForRegister: User    = User(userId, "bahtiyarerden", "MySecret!", isAdmin = true)
  val sessionForUser: Session  = Session(userId, superSecretToken)

  override def afterEach(): Unit = {
    sessionsRepository.dropRepository().futureValue
    usersRepository.dropRepository().futureValue
    db.close()
  }

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db                 = Database.forConfig("h2mem", config)
    usersRepository    = new UserH2Repository(db)
    sessionsRepository = new SessionsH2Repository(db)
    usersRepository.prepareRepository().futureValue
    sessionsRepository.prepareRepository().futureValue
  }

  "Session repository" should {

    "create user session successfully" in {
      // setup
      val userForRegister: User   = User(userId, "bahtiyarerden", "MySecret!", isAdmin = true)
      val sessionForUser: Session = Session(userId, superSecretToken)
      usersRepository.register(userForRegister).futureValue

      // execute
      val result: Int = sessionsRepository.login(sessionForUser).futureValue

      // assert
      result should equal(1)
    }

    "find session successfully" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      sessionsRepository.login(sessionForUser).futureValue

      // execute
      val result = sessionsRepository.findLoggedInUser(superSecretToken).futureValue

      // assert
      result shouldBe Some((userId, userForRegister.name, userForRegister.password, userForRegister.isAdmin))
    }

    "create delete session successfully" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      sessionsRepository.login(sessionForUser).futureValue

      // execute
      val result = sessionsRepository.logout(userId).futureValue

      // assert
      result should equal(1)
    }

    "not find deleted user session" in {
      // execute
      val result: Option[(UUID, String, String, Boolean)] =
        sessionsRepository.findLoggedInUser(superSecretToken).futureValue

      // assert
      result shouldBe None
    }
  }
}
