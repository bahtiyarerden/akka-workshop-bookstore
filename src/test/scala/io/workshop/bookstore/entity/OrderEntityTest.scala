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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class OrderEntityTest
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
  var db: Database                                        = _
  var usersRepository: UsersRepository                    = _
  var booksRepository: BooksRepository                    = _
  var ordersRepository: OrdersRepository                  = _

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db               = Database.forConfig("h2mem", config)
    usersRepository  = new UserH2Repository(db)
    booksRepository  = new BooksH2Repository(db)
    ordersRepository = new OrdersH2Repository(db)
    usersRepository.prepareRepository().futureValue
    booksRepository.prepareRepository().futureValue
    ordersRepository.prepareRepository().futureValue
  }

  override protected def afterEach(): Unit = {
    ordersRepository.dropRepository().futureValue
    booksRepository.dropRepository().futureValue
    usersRepository.dropRepository().futureValue
    db.close()
  }

  "Order Entity" must {
    "create an order" in {
      // setup
      val title      = "A Wizard of Earthsea"
      val userName   = "Bahtiyar"
      val password   = "SuperSecret!"
      val isAdmin    = true
      val userActor  = system.actorOf(UserEntity.props(usersRepository))
      val bookActor  = system.actorOf(BookEntity.props(booksRepository))
      val orderActor = system.actorOf(OrderEntity.props(ordersRepository))

      val registeredUser = (userActor ? RegisterUser(userName, password, isAdmin)).mapTo[UserIdReply].futureValue
      val createdBook    = (bookActor ? CreateBook(title)).mapTo[BookIdReply].futureValue

      // execute
      val orderedBook = (orderActor ? CreateOrder(registeredUser.id, createdBook.id)).mapTo[OrderIdReply].futureValue

      // assert
      orderedBook should matchPattern { case OrderIdReply(_) => }
    }
  }
}
