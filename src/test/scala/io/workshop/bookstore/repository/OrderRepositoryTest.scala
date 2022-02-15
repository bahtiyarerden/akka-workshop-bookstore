package io.workshop.bookstore.repository

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.model.Book.Internal
import io.workshop.bookstore.db.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterEach, Inside }
import io.workshop.bookstore.db.repositories._
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

class OrderRepositoryTest extends AnyWordSpec with Matchers with Inside with BeforeAndAfterEach with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val sys: ActorSystem                        = ActorSystem("MyTest")
  implicit val mat: ExecutionContextExecutor           = sys.dispatcher

  var db: Database                       = _
  var ordersRepository: OrdersRepository = _
  var booksRepository: BooksRepository   = _
  var usersRepository: UsersRepository   = _

  val orderId: UUID           = UUID.randomUUID()
  val bookId: UUID            = UUID.randomUUID()
  val userId: UUID            = UUID.randomUUID()
  val orderForCreate: Order   = Order(orderId, userId, bookId)
  val bookForCreate: Internal = Internal(bookId, "Isaac Asimov's Foundation and Empire")
  val userForRegister: User   = User(userId, "bahtiyarerden", "MySecret!", isAdmin = true)

  override def afterEach(): Unit = {
    ordersRepository.dropRepository().futureValue
    booksRepository.dropRepository().futureValue
    usersRepository.dropRepository().futureValue
    db.close()
  }

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

  "Order repository" should {

    "create schema successfully" in {
      // execute
      val tables = db.run(MTable.getTables).futureValue

      // assert
      tables.count(_.name.name.equalsIgnoreCase("users")) should equal(1)
      tables.count(_.name.name.equalsIgnoreCase("books")) should equal(1)
      tables.count(_.name.name.equalsIgnoreCase("orders")) should equal(1)
    }

    "create order" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      booksRepository.create(bookForCreate).futureValue

      // execute
      val result = ordersRepository.create(orderForCreate).futureValue

      // assert
      result should equal(1)
    }

    "gets user orders" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      booksRepository.create(bookForCreate).futureValue
      ordersRepository.create(orderForCreate).futureValue

      // execute
      val result: Seq[Order] = Source.fromPublisher(ordersRepository.get(userId)).take(10).runWith(Sink.seq).futureValue

      // assert
      result shouldBe Seq(orderForCreate)
    }

    "get all orders" in {
      // setup
      usersRepository.register(userForRegister).futureValue
      booksRepository.create(bookForCreate).futureValue
      ordersRepository.create(orderForCreate).futureValue

      // execute
      val result: Seq[Order] = Source.fromPublisher(ordersRepository.getAll).take(10).runWith(Sink.seq).futureValue

      // assert
      result.length should equal(1)
      result.head shouldBe orderForCreate
    }
  }
}
