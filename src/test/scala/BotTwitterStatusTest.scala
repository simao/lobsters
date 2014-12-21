import io.simao.lobster.{FeedItem, Feed, BotTwitterStatus}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import twitter4j.Status

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


class BotTwitterStatusTest extends FunSuite with MockFactory {
  implicit val ec = ExecutionContext.global

  // TODO: Should be tested somewhere else
  //  test("does not tweet if last updated is None") (pending)

  def updateTwitterFn(s: String): Future[Status] = Future.successful(stub[Status])

  val subject = new BotTwitterStatus(updateTwitterFn)

  test("returns a list of Future[FeedItem], ordered by date") {
    val item1 = FeedItem("Title 1", "link 1", "link 2", DateTime.now().minusDays(1), List(), Some(11))
    val item2 = FeedItem("Title 1", "link 1", "link 2", DateTime.now().minusDays(2), List(), Some(11))

    val feed = Feed(List(item2, item1))

    val updates = Future.sequence(subject.update(feed, DateTime.now().minusDays(10), 10))
    val result = Await.result(updates, 5.seconds)

    assert(result === List(item1, item2))
  }

  test("tags are built properly") (pending)

  test("tags are built properly when no tags are available") (pending)
}
