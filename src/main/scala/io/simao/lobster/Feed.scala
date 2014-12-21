package io.simao.lobster

import java.util.Locale

import com.typesafe.scalalogging.LazyLogging
import io.simao.lobster.RemoteFeed.HTTPResult
import dispatch._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.xml.XML

case class FeedItem(title: String, link: String, commentsLink: String, pubDate: DateTime, tags: Seq[String], score: Option[Int] = None)

// Understands a Lobste.rs feed
class Feed(private val items: Seq[FeedItem]) extends LazyLogging {
  def apply(i: Int) = items.apply(i)

  def size = items.size

  def lastUpdatedAt: Option[DateTime] = items.lift(0).map(_.pubDate)

  def withScores(fetchItemHtml: FeedItem ⇒ HTTPResult)(implicit ec: ExecutionContext): Future[Feed] = {
    itemsWithScores(fetchItemHtml).map(Feed(_))
  }

  def itemsAfter(date: DateTime): Seq[FeedItem] = {
    items.filter(_.pubDate.isAfter(date)).sortBy(-_.pubDate.getMillis)
  }

  private def itemsWithScores(fetchItemHtml: FeedItem ⇒ HTTPResult)(implicit ec: ExecutionContext): Future[List[FeedItem]] = {
    items.par.map { feedItem ⇒
      fetchItemHtml(feedItem).map { feedHtml ⇒
        feedItem.copy(score = Feed.findScore(feedHtml))
      }
    }.foldLeft(Future.successful(List[FeedItem]()))((acc, itemE) ⇒
      acc.flatMap(l ⇒
        itemE
          .map(v ⇒ v :: l)
          .recoverWith({ case t ⇒
            logger.error("Could not fetch feed contents for item", t)
            acc
        })
      ))
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
