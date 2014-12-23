import io.simao.lobster.RemoteFeed.HTTPResult
import io.simao.lobster.{FeedItem, Feed}
import org.scalatest.FunSuite

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

import scala.concurrent.duration._

class FeedTest extends FunSuite {
  implicit val ec = ExecutionContext.global

  val xmlStr = Source
    .fromInputStream(getClass.getResourceAsStream("testFeed.xml"))
    .mkString

  val itemHtml = Source
    .fromInputStream(getClass.getResourceAsStream("testItem.html"))
    .mkString

  def parsedFeed = Feed.fromXml(xmlStr).get

  def parsedItem = parsedFeed(0)

  test("parses an item's attributes") {
    val p = parsedItem
    assert(p.title === "ssh chat.shazow.net")
    assert(p.link === "https://lobste.rs/s/lwmnsg/ssh_chat_shazow_net")
    assert(p.guid === "https://lobste.rs/s/lwmnsg")
    assert(p.commentsLink === "https://lobste.rs/s/lwmnsg/ssh_chat_shazow_net")
    assert(p.pubDate.toString === "2014-12-13T01:19:46.000Z")
    assert(p.tags === List("show"))
  }

  test("multiple tags are parsed into a list") {
    val p = parsedFeed(1)
    assert(p.tags === List("browsers", "security", "web"))
  }
  
  test("a feed's lastUpdatedAt is the pubDate of the first feed item") {
    assert(parsedFeed.lastUpdatedAt.map(_.toString) === Some("2014-12-13T01:19:46.000Z"))
  }

  test("parses a list with the size equal to the number of items in the feed") {
    assert(parsedFeed.size === 25)
  }

  test("finds a score inside an item's html") {
    assert(Feed.findScore(itemHtml) === Some(17))
  }

  test("adds items scores when found in HTML") {
    def f: (FeedItem ⇒ HTTPResult) = { _ ⇒ Future.successful(itemHtml) }

    val result = Await.result(parsedFeed.withScores(f), 5.seconds)

    assert(result.apply(0).score === Some(17))
  }

  test("returns an empty list when item is not found in HTML") {
    def f: (FeedItem ⇒ HTTPResult) = { _ ⇒ Future.failed(new Exception("Could not get Score")) }
    val result = Await.result(parsedFeed.withScores(f), 5.seconds)
    assert(result.size === 0)
  }
}
