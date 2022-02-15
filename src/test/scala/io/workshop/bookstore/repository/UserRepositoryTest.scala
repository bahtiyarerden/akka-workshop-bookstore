package io.workshop.bookstore.repository

import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.model.User
import io.workshop.bookstore.db.repositories.{ UserH2Repository, UsersRepository }
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import java.util.UUID
import scala.util.Success

class UserRepositoryTest extends AnyWordSpec with Matchers with Inside with BeforeAndAfterEach with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  var db: Database                     = _
  var usersRepository: UsersRepository = _

  val userId: UUID          = UUID.randomUUID()
  val userForRegister: User = User(userId, "bahtiyarerden", "MySecret!", isAdmin = true)

  override def afterEach(): Unit = {
    usersRepository.dropRepository().futureValue
    db.close()
  }

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db              = Database.forConfig("h2mem", config)
    usersRepository = new UserH2Repository(db)
    usersRepository.prepareRepository().futureValue
  }

  "User repository" should {

    "create schema successfully" in {
      // execute
      val tables = db.run(MTable.getTables).futureValue

      // assert
      tables.count(_.name.name.equalsIgnoreCase("users")) should equal(1)
    }

    "register user" in {
      // execute
      val result = usersRepository.register(userForRegister).futureValue
      // assert
      result should equal(1)
    }

    "not give permission to register user with same username" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      val userForRegisterSameName = User(UUID.randomUUID(), "bahtiyarerden", "MySecret!", isAdmin = true)

      // execute
      val result = usersRepository.register(userForRegisterSameName).failed

      // assert
      whenReady(result) { e =>
        e shouldBe an[Exception]
      }
    }

    "give registered user info by id" in {
      // setup
      usersRepository.register(userForRegister).futureValue

      // execute
      val user = usersRepository.get(userId).futureValue

      // assert
      user.isDefined shouldBe true
      inside(user.get) { case User(id, name, password, isAdmin) =>
        id shouldBe userForRegister.id
        name shouldBe userForRegister.name
        password shouldBe userForRegister.password
        isAdmin shouldBe userForRegister.isAdmin
      }
    }

    "give registered user info by username" in {
      // setup
      usersRepository.register(userForRegister).futureValue

      // execute
      val result = usersRepository.getByName(userForRegister.name).futureValue

      // assert
      result.isDefined shouldBe true
      inside(result.get) { case User(id, name, password, isAdmin) =>
        id shouldBe userForRegister.id
        name shouldBe userForRegister.name
        password shouldBe userForRegister.password
        isAdmin shouldBe userForRegister.isAdmin
      }
    }

    "not find unregistered user info by id" in {
      // execute
      val result = usersRepository.get(UUID.randomUUID()).futureValue

      // assert
      result shouldBe None
    }

    "not find unregistered user info by username" in {
      // execute
      val result = usersRepository.getByName("Non-user").futureValue

      // assert
      result shouldBe None
    }
  }
}
