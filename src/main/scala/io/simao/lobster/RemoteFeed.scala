package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import dispatch.Defaults._
import dispatch.{Http, url, _}
import scala.concurrent.Future

// Understands a lobster feed hosted remotely
object RemoteFeed extends LazyLogging {
  type HTTPResult = Future[Either[Throwable, String]]
  type FeedResult = Either[Throwable, Feed]

  def fetchHtml(item: FeedItem): HTTPResult= {
    val svc = url(item.commentsLink)
    Http(svc OK as.String).either
  }

  def fetchFeed(feedUrl: String): HTTPResult = {
    val svc = url(feedUrl)
    Http(svc OK as.String).either
  }

  def fetchAll(feedUrl: String): FeedResult = {
    val feed = fetchFeed(feedUrl).apply().right.flatMap(Feed.fromXml).right

    val scoredItems = feed.map {
      _.items.par.map { feedItem ⇒
        fetchHtml(feedItem).apply().right.map { feedHtml ⇒
          feedItem.copy(score = Feed.findScore(feedHtml))
        }
      }.foldLeft(List[FeedItem]())((acc, itemE) ⇒
        itemE match {
          case Left(t) ⇒
            logger.error("Could not download item score: ", t)
            acc
          case Right(v) ⇒ v :: acc
        })
    }

    scoredItems.right.flatMap(l ⇒ feed.map(_.copy(items = l)))
  }
}
