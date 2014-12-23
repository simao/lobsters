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
  val confFile = opt[String]("config-file", descr = "path to a config file", default = Some(defaultConfigFile))
}


// TODO: Support Print options
object Lobster extends App with StrictLogging {
  implicit val ec = ExecutionContext.global

  val lobstersUrl = "https://lobste.rs/rss"
  val opts = new CliOpts(args)
  val confFileName: String = opts.confFile.apply()

  val conf = Config.fromSource(Source.fromFile(confFileName))

  val mainF = RemoteFeed.fetchAll(lobstersUrl).flatMap { f ⇒
    logger.info(s"Got feed with ${f.size} items")

    // TODO: Should save feed to db
    def traverseF(itemF: Future[FeedItem]): Future[Unit] = {
      itemF
        .map(item ⇒ logger.info(s"Updated twitter for item: ${item.title} (${item.score})"))
        .recover({ case t ⇒ logger.error("Error updating twitter:", t)})
    }

    val updatedTwitterF =
      new BotTwitterStatus(TwitterClient.tweet)
        .update(f, DateTime.now().minusDays(2), 4)

    Future.traverse(updatedTwitterF)(traverseF)

  }.recover({
    case t ⇒
      logger.error("Could not fetch feed: ", t)
  })

  // TODO: Maybe instead of lastUpdate we can save an id for all twitted items
  // How to deal with truncation?

  Await.ready(mainF, 2 minutes)

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
