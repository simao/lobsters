package io.simao.lobster

import scala.util.{Failure, Success, Try}
import scala.xml.XML

case class FeedItem(title: String, link: String, commentsLink: String, pubDate: String, tags: Seq[String], score: Option[Int] = None)

case class Feed(items: Seq[FeedItem]) {
  def apply(i: Int) = items.apply(i)

  def size = items.size

  def lastUpdatedAt: Option[String] = items.lift(0).map(_.pubDate)
}

object Feed {
  def findScore(html: String): Option[Int] = {
    """<div class="score">(\d+)</div>""".r
      .findFirstMatchIn(html)
      .map(_.group(1))
      .map(_.toString.toInt)
  }

  def fromXml(xml: String): Either[Throwable, Feed] = {
    Try(XML.loadString(xml)) map { feed ⇒
      for {
        item ← feed \ "channel" \ "item"
        title ← item \ "title"
        link ← item \ "link"
        commentsLink ← item \ "comments"
        pubDate ← item \ "pubDate"
        tags = (item \ "category").foldRight(List[String]())(_.text :: _)
      } yield FeedItem(title.text, link.text, commentsLink.text, pubDate.text, tags)
    } match {
      case Success(r) ⇒ Right(Feed(r))
      case Failure(t) ⇒ Left(t)
    }
  }
}
