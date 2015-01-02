package io.simao.lobster

import org.rogach.scallop.ScallopConf

class CliOpts(args: Seq[String]) extends ScallopConf(args) {
  val jdbc = opt[String]("jdbc", descr = "JDBC Connection string", default = Some("jdbc:sqlite:saved_feeds.db"))
  val days = opt[Int]("days", descr = "Only consider stories newer than this number of days", default = Some(2))
  val score = opt[Int]("score", descr = "Minimum number of votes to tweet a story", default = Some(10))
  val tweet = toggle("tweet", descrYes = "Tweet items", default = Some(true), noshort = true)
  val drop = toggle("drop", descrYes = "Drop item database. CAUTION: Causes retweet of seen items", default = Some(false), noshort = true)
}

