package io.simao.lobster

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import twitter4j.Status

import scala.concurrent.{ExecutionContext, Future}

// Understands a LobstersBot twitter status
class BotTwitterStatus(tweetFn: String ⇒ Future[Status]) extends LazyLogging {

  def update(feed: Feed, lastUpdate: DateTime, minScore: Int)(implicit ec: ExecutionContext): Seq[Future[FeedItem]] = {
    val tweet = tweetFn.compose(buildStatus)

    val statuses = for {
      item ← feed.itemsAfter(lastUpdate)
      if item.score.exists(_ >= minScore)
    } yield tweet(item).map(_ ⇒ item)

    logger.info(s"Posting ${statuses.size} tweets")

    statuses
  }

  private def buildStatus(item: FeedItem): String = {
    val tags = item.tags.map(t ⇒ s"#$t").mkString(" ")
    s"${item.title} ${item.link} ${item.commentsLink} $tags"
  }
}

object BotTwitterStatus {

}
