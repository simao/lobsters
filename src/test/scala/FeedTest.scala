import io.simao.lobster.Feed
import org.scalatest.FunSuite

import scala.io.Source

class FeedTest extends FunSuite {
  val xmlStr = Source
    .fromInputStream(getClass.getResourceAsStream("testFeed.xml"))
    .mkString

  def parsedFeed = Feed.fromXml(xmlStr)

  def parsedItem = parsedFeed.right.get(0)

  test("parses an item's attributes") {
    val p = parsedItem
    assert(p.title === "ssh chat.shazow.net")
    assert(p.link === "https://lobste.rs/s/lwmnsg/ssh_chat_shazow_net")
    assert(p.commentsLink === "https://lobste.rs/s/lwmnsg/ssh_chat_shazow_net")
    assert(p.pubDate === "Fri, 12 Dec 2014 19:19:46 -0600")
    assert(p.tags === List("show"))
  }

  test("multiple tags are parsed into a list") {
    val p = parsedFeed.right.get(1)
    assert(p.tags === List("browsers", "security", "web"))
  }
  
  test("a feed's lastUpdatedAt is the pubDate of the first feed item") {
    assert(parsedFeed.right.map(_.lastUpdatedAt) === Right(Some("Fri, 12 Dec 2014 19:19:46 -0600")))
  }

  test("parses a list with the size equal to the number of items in the feed") {
    assert(parsedFeed.right.map(_.size) === Right(25))
  }

  test("finds a score inside an item's html") {
    val itemHtml = Source
      .fromInputStream(getClass.getResourceAsStream("testItem.html"))
      .mkString

    assert(Feed.findScore(itemHtml) === Some(17))
  }
}
