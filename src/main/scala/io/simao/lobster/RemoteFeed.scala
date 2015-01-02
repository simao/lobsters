package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import dispatch.Defaults._
import dispatch.{Http, url, _}
import scala.concurrent.Future

// Understands a lobster feed hosted remotely
object RemoteFeed extends LazyLogging {
  type HTTPResult = Future[String]

  def fetchAll(feedUrl: String)(filter: FeedItem => Boolean): Future[Feed] = {
    for {
      feedXml ← fetchFeed(feedUrl)
      feed ← Future.fromTry(Feed.fromXml(feedXml))
      withScores ← feed.rejectItems(filter).withScores(fetchHtml)
    } yield withScores
  }

  private def fetchHtml(item: FeedItem): HTTPResult= {
    fetchHttp(item.commentsLink)
  }

  private def fetchFeed(feedUrl: String): HTTPResult = {
    fetchHttp(feedUrl)
  }

  private def fetchHttp(remoteUrl: String): HTTPResult = {
    val svc = url(remoteUrl)
    Http(svc OK as.String)
  }
}
