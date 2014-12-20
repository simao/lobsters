package io.simao.lobster


import com.typesafe.scalalogging.StrictLogging
import dispatch.Http
import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._


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

  // TODO: Lots of stuff to do before start tweeting

  RemoteFeed.fetchAll(lobstersUrl) match {
    case Success(f) ⇒
      logger.info(s"Got feed with ${f.size} items")

//      new BotTwitterStatus(TwitterClient.tweet).update(f, DateTime.now().minusDays(1), 4)

    case Failure(t) ⇒
      logger.error("Could not fetch feed: ", t)
  }




  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
