package io.simao.lobster

import com.typesafe.scalalogging.StrictLogging
import dispatch.Http
import io.simao.db.FeedDatabase
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

class CliOpts(args: Seq[String]) extends ScallopConf(args) {
  val jdbc = opt[String]("jdbc", descr = "JDBC Connection string", default = Some("jdbc:sqlite:saved_feeds.db"))
  val days = opt[Int]("days", descr = "Only consider stories newer than this number of days", default = Some(2))
  val score = opt[Int]("score", descr = "Minimum number of votes to tweet a story", default = Some(4))
  val tweet = toggle("tweet", descrYes = "Tweet items", default = Some(true), noshort = true)
  val drop = toggle("drop", descrYes = "Drop item database. CAUTION", default = Some(false), noshort = true)
}

object Lobster extends App with StrictLogging {
  implicit val ec = ExecutionContext.global

  val lobstersUrl = "https://lobste.rs/rss"
  val opts = new CliOpts(args)

  def withDb[T](f: FeedDatabase ⇒ T): T = FeedDatabase.withConnection(opts.jdbc())(f)

  def processTweetedItem(db: FeedDatabase)(itemF: Future[FeedItem]): Future[Unit] = {
    itemF
      .map(item ⇒ db.save(item))
      .map(item ⇒ logger.info(s"Updated twitter for item: ${item.title} (${item.score.getOrElse("?")})"))
      .recover({ case t ⇒ logger.error("Error updating twitter:", t)})
  }

  def getUnsavedFeed(db: FeedDatabase): Future[Feed] = {
    RemoteFeed.fetchAll(lobstersUrl)(db.isSaved)
  }

  def tweetFeed(feed: Feed): Seq[Future[FeedItem]] =
    new BotTwitterStatus(tweetFn)
      .update(feed, DateTime.now().minusDays(opts.days()), opts.score())

  val tweetFn =
    if(opts.tweet())
      TwitterClient.tweet _
    else
      (t: String) ⇒ { logger.info(s"Not tweeting $t"); Future.successful("") }

  logger.info(s"Getting lobste.rs news newer than ${opts.days()} days with score > ${opts.score()}")

  withDb(_.setupTables(opts.drop()))

  withDb { db ⇒
    val mainF = getUnsavedFeed(db).flatMap { feed ⇒
      logger.info(s"Got feed with ${feed.size} unsaved items")

      val updatedTwitterF = tweetFeed(feed)

      Future.traverse(updatedTwitterF)(processTweetedItem(db))
    }.recover {
      case t ⇒
        logger.error("Could not fetch feed: ", t)
    }

    Await.ready(mainF, 2 minutes)
  }

  logger.info("Finished getting lobster.rs feed items")

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
