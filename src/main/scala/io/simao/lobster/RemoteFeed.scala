package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import dispatch.Defaults._
import dispatch.{Http, url, _}

import scala.concurrent.Future

// Understands a lobster feed hosted remotely
object RemoteFeed extends LazyLogging {
  type HTTPResult = Future[Either[Throwable, String]]
  type FeedResult = Either[Throwable, List[FeedItem]]

  def addScore(item: FeedItem, html: String): FeedItem = {
    item.copy(score = Feed.findScore(html))
  }

  def fetchHtml(item: FeedItem): HTTPResult= {
    val svc = url(item.commentsLink)
    Http(svc OK as.String).either
  }

  def fetchFeed(feedUrl: String): HTTPResult = {
    val svc = url(feedUrl)
    Http(svc OK as.String).either
  }

  def fetchAll(feedUrl: String): FeedResult = {
    val xmlFuture = fetchFeed(feedUrl)

    xmlFuture.apply().right.flatMap { xml ⇒
      Feed.fromXml(xml).right.flatMap { parsedFeed ⇒
        // TODO: This can be done in parallel
        parsedFeed.items.par.map { item ⇒
          fetchHtml(item).apply().right.map { itemHtml ⇒
            logger.info("Getting feed for item " + item.title)
            addScore(item, itemHtml)
          }
        }.foldLeft(Right(List()): FeedResult)((acc, itemEither) ⇒ {
          acc.right.flatMap(l ⇒ itemEither match {
            case Left(t) ⇒ Left(t)
            case Right(i) ⇒ Right(i :: l)
          })
        })
      }
    }
  }
}
