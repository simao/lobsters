package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import dispatch.Defaults._
import dispatch.{Http, url, _}
import io.simao.util.TryOps._
import scala.concurrent.Future
import scala.util.Try


// Understands a lobster feed hosted remotely
object RemoteFeed extends LazyLogging {
  // TODO: This can be just Future[String] and we use Future.onError/recover to get errors
  type HTTPResult = Future[Try[String]]

  def fetchAll(feedUrl: String): Try[Feed] = {
    fetchFeed(feedUrl).apply()
      .flatMap(Feed.fromXml)
      .map(_.withScores(fetchHtml))
  }

  private def fetchHtml(item: FeedItem): HTTPResult= {
    fetchHttp(item.commentsLink)
  }

  private def fetchFeed(feedUrl: String): HTTPResult = {
    fetchHttp(feedUrl)
  }

  private def fetchHttp(remoteUrl: String): HTTPResult = {
    val svc = url(remoteUrl)
    Http(svc OK as.String).either
  }
}
