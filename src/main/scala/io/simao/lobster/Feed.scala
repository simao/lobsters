package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import io.simao.lobster.RemoteFeed.HTTPResult
import dispatch._
import scala.util.{Failure, Success, Try}
import scala.xml.XML

case class FeedItem(title: String, link: String, commentsLink: String, pubDate: String, tags: Seq[String], score: Option[Int] = None)

class Feed(private val items: Seq[FeedItem]) extends LazyLogging {
  def apply(i: Int) = items.apply(i)

  def size = items.size

  def lastUpdatedAt: Option[String] = items.lift(0).map(_.pubDate)

  def withScores(fetchItemHtml: FeedItem ⇒ HTTPResult): Feed = {
    val scoredItems = itemsWithScores(fetchItemHtml)
    Feed(scoredItems)
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
      } yield FeedItem(title.text, link.text, commentsLink.text, pubDate.text, tags)
    }).map(Feed(_))
  }
}
