package io.simao.lobster

import scala.util.Try
import scala.xml.XML

// TODO: joda time to te rescue
case class FeedItem(title: String, link: String, commentsLink: String, pubDate: String, tags: Seq[String])

case class Feed(items: Seq[FeedItem]) {
  def apply(i: Int) = items.apply(i)

  def size = items.size

  def lastUpdatedAt: Option[String] = items.lift(0).map(_.pubDate)
}

object Feed {
  def fromXml(xml: String): Try[Feed] = {
    Try(XML.loadString(xml)) map { feed ⇒
      for {
        item ← feed \ "channel" \ "item"
        title ← item \ "title"
        link ← item \ "link"
        commentsLink ← item \ "comments"
        pubDate ← item \ "pubDate"
        tags = (item \ "category").foldRight(List[String]())(_.text :: _)
      } yield FeedItem(title.text, link.text, commentsLink.text, pubDate.text, tags)
    } map(Feed(_))
  }
}

object Lobster extends App {
  val lobstersUrl = "https://lobste.rs/rss"

  //val svc = url(lobstersUrl)
  //val country = Http(svc OK as.String)

  //println(country())

}
