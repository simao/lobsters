package io.simao.lobster

import java.util.Locale

import com.typesafe.scalalogging.LazyLogging
import io.simao.lobster.RemoteFeed.HTTPResult
import dispatch._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.{Failure, Success, Try}
import scala.xml.XML

case class FeedItem(title: String, link: String, commentsLink: String, pubDate: DateTime, tags: Seq[String], score: Option[Int] = None)

// Understands a Lobste.rs feed
class Feed(private val items: Seq[FeedItem]) extends LazyLogging {
  def apply(i: Int) = items.apply(i)

  def size = items.size

  def lastUpdatedAt: Option[DateTime] = items.lift(0).map(_.pubDate)

  def withScores(fetchItemHtml: FeedItem ⇒ HTTPResult): Feed = {
    val scoredItems = itemsWithScores(fetchItemHtml)
    Feed(scoredItems)
  }

  def itemsAfter(date: DateTime): Seq[FeedItem] = {
    items.filter(_.pubDate.isAfter(date))
  }

  private def itemsWithScores(fetchItemHtml: FeedItem ⇒ HTTPResult): List[FeedItem] = {
    items.par.map { feedItem ⇒
      fetchItemHtml(feedItem).apply().map { feedHtml ⇒
        feedItem.copy(score = Feed.findScore(feedHtml))
      }
    }.foldLeft(List[FeedItem]())((acc, itemE) ⇒
      itemE match {
        case Failure(t) ⇒
          logger.error(s"Could not download item score: ", t)
          acc
        case Success(v) ⇒ v :: acc
      })
  }

  override def toString: String = items.mkString
}

object Feed {
  def apply(items: Seq[FeedItem]) = new Feed(items)

  def findScore(html: String): Option[Int] = {
    """<div class="score">(\d+)</div>""".r
      .findFirstMatchIn(html)
      .map(_.group(1))
      .map(_.toString.toInt)
  }

  def fromXml(xml: String): Try[Feed] = {
    (Try(XML.loadString(xml)) map { feed ⇒
      for {
        item ← feed \ "channel" \ "item"
        title ← item \ "title"
        link ← item \ "link"
        commentsLink ← item \ "comments"
        pubDate ← item \ "pubDate"
        tags = (item \ "category").foldRight(List[String]())(_.text :: _)
      } yield FeedItem(title.text, link.text, commentsLink.text, feedDate(pubDate.text), tags)
    }).map(Feed(_))
  }

  private def feedDate(str: String): DateTime = {
    val fmt = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z").withLocale(Locale.ENGLISH)
    fmt.parseDateTime(str)
  }
}
