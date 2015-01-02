package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import io.simao.util.KestrelObj._

// Understands a LobstersBot twitter status
class BotTwitterStatus(tweetFn: String ⇒ Future[String]) extends LazyLogging {
  def update(feed: Feed, lastUpdate: DateTime, minScore: Int)(implicit ec: ExecutionContext): Seq[Future[FeedItem]] = {
    val tweetItem = tweetFn.compose(buildStatus)

    (for {
      item ← feed.itemsAfter(lastUpdate) if item.score.exists(_ >= minScore)
    } yield {
      tweetItem(item).map(_ ⇒ item)
    }).tap { o ⇒
      logger.info(s"Posting ${o.size} tweets")
    }
  }

  private def buildStatus(item: FeedItem): String = {
    val tags = item.tags.map(t ⇒ s"#$t").toList
    (s"${item.title} ${item.link} ${item.commentsLink}" :: tags).mkString(" ")
  }
}
