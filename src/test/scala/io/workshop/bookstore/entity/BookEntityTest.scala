package io.workshop.bookstore.entity

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import io.workshop.bookstore.db.repositories.{ BooksH2Repository, BooksRepository }
import io.workshop.bookstore.entities.BookEntity
import io.workshop.bookstore.entities.commands._
import io.workshop.bookstore.entities.replies._
import io.workshop.bookstore.error._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import slick.jdbc.H2Profile.api._

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

class BookEntityTest
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
  var booksRepository: BooksRepository                    = _

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

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

  "Book Entity" must {
    "create a book" in {
      // setup
      val title       = "A Wizard of Earthsea"
      val entityActor = system.actorOf(BookEntity.props(booksRepository))

      // execute
      val createdBook = (entityActor ? CreateBook(title)).mapTo[BookIdReply].futureValue

      // assert
      createdBook should matchPattern { case BookIdReply(_) => }
    }

    "get a book" in {
      // setup
      val title       = "A Wizard of Earthsea"
      val entityActor = system.actorOf(BookEntity.props(booksRepository))
      val createdBook = (entityActor ? CreateBook(title)).mapTo[BookIdReply].futureValue

      // execute
      val book = (entityActor ? GetBook(createdBook.id)).mapTo[BookReply].futureValue

      // assert
      assert(book == BookReply(createdBook.id, title))
    }

    "update a book" in {
      // setup
      val title        = "A Wizard of Earthsea"
      val updatedTitle = "Brave New World"

      val entityActor = system.actorOf(BookEntity.props(booksRepository))
      val createdBook = (entityActor ? CreateBook(title)).mapTo[BookIdReply].futureValue

      // execute
      val effectedRows = (entityActor ? UpdateBook(createdBook.id, updatedTitle)).mapTo[Int].futureValue

      // assert
      assert(effectedRows == 1)
    }

    "delete a book" in {
      // setup
      val title = "A Wizard of Earthsea"

      val entityActor = system.actorOf(BookEntity.props(booksRepository))
      val createdBook = (entityActor ? CreateBook(title)).mapTo[BookIdReply].futureValue

      // execute
      val effectedRows = (entityActor ? DeleteBook(createdBook.id)).mapTo[Int].futureValue

      // assert
      assert(effectedRows == 1)
    }

    "throw error when getting a book that not exists" in {
      // setup
      val bookId      = UUID.randomUUID()
      val entityActor = system.actorOf(BookEntity.props(booksRepository))

      // execute
      val getBook = Try((entityActor ? GetBook(bookId)).futureValue)

      // assert
      getBook shouldBe a[Failure[BookNotFoundError]]
    }

    "throw error when updating a book that not exists" in {
      // setup
      val bookId      = UUID.randomUUID()
      val entityActor = system.actorOf(BookEntity.props(booksRepository))

      // execute
      val updateBookFuture = entityActor ? UpdateBook(bookId, "Some title")

      whenReady(updateBookFuture.failed) { error =>
        error shouldBe a[BookNotUpdatedError]
        assert(error.asInstanceOf[BookNotUpdatedError].getMessage == "Book could not be updated")
      }
    }

    "throw error when deleting a book that not exists" in {
      // setup
      val bookId      = UUID.randomUUID()
      val entityActor = system.actorOf(BookEntity.props(booksRepository))

      // execute
      val deleteBookFuture = entityActor ? DeleteBook(bookId)

      whenReady(deleteBookFuture.failed) { error =>
        error shouldBe a[BookNotDeletedError]
        assert(error.asInstanceOf[BookNotDeletedError].getMessage == "Book could not be deleted")
      }
    }
  }
}
