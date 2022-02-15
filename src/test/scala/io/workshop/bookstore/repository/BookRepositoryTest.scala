package io.workshop.bookstore.repository

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.model.Book.Internal
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterEach, Inside }
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable
import io.workshop.bookstore.db.repositories._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

class BookRepositoryTest extends AnyWordSpec with Matchers with Inside with BeforeAndAfterEach with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))
  implicit val sys: ActorSystem                        = ActorSystem("MyTest")
  implicit val mat: ExecutionContextExecutor           = sys.dispatcher
  var db: Database                                     = _
  var booksRepository: BooksRepository                 = _

  val bookId: UUID            = UUID.randomUUID()
  val bookForCreate: Internal = Internal(bookId, "Isaac Asimov's Foundation and Empire")

  override protected def beforeEach(): Unit = {
    val config = ConfigFactory
      .load()
      .withValue("h2mem.url", ConfigValueFactory.fromAnyRef(s"jdbc:h2:mem:db-${UUID.randomUUID().toString}"))

    db              = Database.forConfig("h2mem", config)
    booksRepository = new BooksH2Repository(db)
    booksRepository.prepareRepository().futureValue
  }

  override protected def afterEach(): Unit = {
    booksRepository.dropRepository().futureValue
    db.close()
  }

  "Book repository" should {
    "create schema successfully" in {
      // execute
      val tables = db.run(MTable.getTables).futureValue

      // assert
      tables.count(_.name.name.equalsIgnoreCase("books")) should equal(1)
    }

    "create book" in {
      // execute
      val result = booksRepository.create(bookForCreate).futureValue

      // assert
      result should equal(1)
    }

    "get book by id" in {
      // setup
      booksRepository.create(bookForCreate).futureValue

      // execute
      val result = booksRepository.get(bookId).futureValue

      // assert
      inside(result.get) { case Internal(id, title) =>
        id shouldBe bookForCreate.id
        title shouldBe bookForCreate.title
      }
    }

    "update book" in {
      // setup
      val newTitle = "J.R.R Tolkien's The Lord of the Rings"
      booksRepository.create(bookForCreate).futureValue

      // execute
      val result  = booksRepository.update(Internal(bookId, newTitle)).futureValue
      val newBook = booksRepository.get(bookId).futureValue

      // assert
      result should equal(1)
      newBook should equal(Some(Internal(bookId, newTitle)))
    }

    "get all books in streaming mode" in {
      // setup
      booksRepository.create(bookForCreate).futureValue

      // execute
      val result: Seq[Internal] = Source.fromPublisher(booksRepository.getAll).take(10).runWith(Sink.seq).futureValue

      // assert
      result.length should equal(1)
      result shouldBe Seq(Internal(bookId, bookForCreate.title))
    }

    "delete book" in {
      // setup
      booksRepository.create(bookForCreate).futureValue

      // execute
      val result    = booksRepository.delete(bookId).futureValue
      val notExists = booksRepository.get(bookId).futureValue

      // assert
      result should equal(1)
      notExists shouldBe None
    }

    "not get not created book" in {
      // execute
      val notExists = booksRepository.get(bookId).futureValue

      // assert
      notExists shouldBe None
    }

    "not update not created book" in {
      // execute
      val notExists = booksRepository.update(bookForCreate).futureValue

      // assert
      notExists should equal(0)
    }

    "not delete not created book" in {
      // execute
      val notExists = booksRepository.delete(bookId).futureValue

      // assert
      notExists should equal(0)
    }
  }
}
