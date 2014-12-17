package io.simao.lobster

import dispatch.Http

object Lobster extends App {
  val lobstersUrl = "https://lobste.rs/rss"

  val v = RemoteFeed
    .fetchAll(lobstersUrl)

  println(v)

  // TODO: See https://github.com/dispatch/reboot/issues/99
  Http.shutdown()
}
