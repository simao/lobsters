package io.simao.lobster

import com.typesafe.scalalogging.StrictLogging
import dispatch.Http
import io.simao.db.FeedDatabase
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._

object LobsterApp extends App with StrictLogging {
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

  def updateBotStatus(feed: Feed): Seq[Future[FeedItem]] =
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

      val updatedTwitterF = updateBotStatus(feed)

      Future.traverse(updatedTwitterF)(processTweetedItem(db))
    }.recover {
      case t ⇒
        logger.error("An error occurred: ", t)
    }

    Await.ready(mainF, 2 minutes)
  }

  logger.info("Finished getting lobster.rs feed items")

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
