package io.simao.lobster


import dispatch.Http
import org.rogach.scallop.ScallopConf
import scala.io.Source

class CliOpts(args: Seq[String]) extends ScallopConf(args) {
  val defaultConfigFile = "lobster.json"
  val confFile = opt[String]("config-file", descr = "path to a config file", default = Some(defaultConfigFile))
}


// TODO: Support Print options
object Lobster extends App {
  val lobstersUrl = "https://lobste.rs/rss"
  val opts = new CliOpts(args)
  val confFileName: String = opts.confFile.apply()

  val conf = Config.fromSource(Source.fromFile(confFileName))

  println(conf)

  val feed = RemoteFeed.fetchAll(lobstersUrl)

  println(feed)

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
