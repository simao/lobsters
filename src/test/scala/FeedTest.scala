import io.simao.lobster.RemoteFeed.HTTPResult
import io.simao.lobster.{FeedItem, Feed}
import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

class FeedTest extends FunSuite {
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
    assert(p.commentsLink === "https://lobste.rs/s/lwmnsg/ssh_chat_shazow_net")
    assert(p.pubDate === "Fri, 12 Dec 2014 19:19:46 -0600")
    assert(p.tags === List("show"))
  }

  test("multiple tags are parsed into a list") {
    val p = parsedFeed(1)
    assert(p.tags === List("browsers", "security", "web"))
  }
  
  test("a feed's lastUpdatedAt is the pubDate of the first feed item") {
    assert(parsedFeed.lastUpdatedAt === Some("Fri, 12 Dec 2014 19:19:46 -0600"))
  }

  test("parses a list with the size equal to the number of items in the feed") {
    assert(parsedFeed.size === 25)
  }

  test("finds a score inside an item's html") {
    assert(Feed.findScore(itemHtml) === Some(17))
  }

  test("adds items scores when found in HTML") {
    def f: (FeedItem ⇒ HTTPResult) = { _ ⇒ Future.successful(Success(itemHtml)) }

    assert(parsedFeed.withScores(f).apply(0).score === Some(17))
  }

  test("returns an empty list when item is not found in HTML") {
    def f: (FeedItem ⇒ HTTPResult) = { _ ⇒ Future.successful(Failure(new Exception("Could not get Score"))) }
    assert(parsedFeed.withScores(f).size === 0)
  }
}
