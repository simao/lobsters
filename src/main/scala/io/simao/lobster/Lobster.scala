package io.simao.lobster

import com.typesafe.scalalogging.StrictLogging
import dispatch.Http
import io.simao.db.FeedDatabase
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.io.Source
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._
;

class CliOpts(args: Seq[String]) extends ScallopConf(args) {
  val defaultConfigFile = "lobster.json"
  val jdbc = opt[String]("jdbc", descr = "JDBC Connection string", default = Some("jdbc:sqlite:saved_feeds.db"))
  val confFile = opt[String]("config-file", descr = "path to a config file", default = Some(defaultConfigFile))
}

// TODO: Support Print options
object Lobster extends App with StrictLogging {
  implicit val ec = ExecutionContext.global

  val lobstersUrl = "https://lobste.rs/rss"
  val opts = new CliOpts(args)
  val confFileName: String = opts.confFile.apply()

  def dbExec[T](f: FeedDatabase ⇒ T): T = FeedDatabase.withConnection(opts.jdbc())(f)

  def processTweetedItem(itemF: Future[FeedItem]): Future[Unit] = {
    itemF
      .map(item ⇒ dbExec(_.save(item)))
      .map(item ⇒ logger.info(s"Updated twitter for item: ${item.title} (${item.score})"))
      .recover({ case t ⇒ logger.error("Error updating twitter:", t)})
  }

  def getFeed: Future[Feed] = FeedDatabase.withConnection(opts.jdbc()) { db ⇒ {
    logger.info("Wat")
    RemoteFeed.fetchAll(lobstersUrl)(i ⇒ db.isSaved(i))
  }}

  // TODO: Get score and days back from conf
  def tweetFeed(feed: Feed): Seq[Future[FeedItem]] =
    new BotTwitterStatus(TwitterClient.tweet)
      .update(feed, DateTime.now().minusDays(2), 4)

  dbExec(_.setupTables())

  val mainF = getFeed.flatMap { feed ⇒
    logger.info(s"Got feed with ${feed.size} items")

    val updatedTwitterF = tweetFeed(feed)

    Future.traverse(updatedTwitterF)(processTweetedItem)
  }.recover({
    case t ⇒
      logger.error("Could not fetch feed: ", t)
  })

  Await.ready(mainF, 2 minutes)

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
