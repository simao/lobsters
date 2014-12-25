import io.simao.lobster.{FeedItem, Feed, BotTwitterStatus}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import twitter4j.Status

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


class BotTwitterStatusTest extends FunSuite with MockFactory {
  implicit val ec = ExecutionContext.global

  val item1 = FeedItem("g0", "Title 1", "link 1", "link 2", DateTime.now().minusDays(1), List("tag0", "tag1"), Some(11))
  val item2 = FeedItem("g1", "Title 1", "link 1", "link 2", DateTime.now().minusDays(2), List(), Some(11))
  val feed = Feed(List(item1, item2))

  test("returns a list of Future[FeedItem], ordered by date") {
    val updateTwitterFn = (_: String) ⇒ Future.successful("Tweeted Text")
    val subject = new BotTwitterStatus(updateTwitterFn)

    val updates = Future.sequence(subject.update(feed, DateTime.now().minusDays(10), 10))
    val result = Await.result(updates, 2.seconds)

    assert(result === List(item1, item2))
  }

  test("tags are built properly") {
    val updateTwitterFn = mockFunction[String, Future[String]]

    updateTwitterFn.expects("Title 1 link 1 link 2 #tag0 #tag1")
      .once()
      .returning(Future.successful("Tweeted Text"))

    updateTwitterFn.expects("Title 1 link 1 link 2")
      .once()
      .returning(Future.successful("Tweeted Text"))

    val subject = new BotTwitterStatus(updateTwitterFn)

    val updates = Future.sequence(subject.update(feed, DateTime.now().minusDays(10), 10))

    Await.ready(updates, 2.seconds)
  }
}
