package io.simao.lobster

import dispatch.Http

object Lobster extends App {
  val lobstersUrl = "https://lobste.rs/rss"

  val feed = RemoteFeed.fetchAll(lobstersUrl)

  println(feed)

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
