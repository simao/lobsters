package io.simao.lobster

import twitter4j._

import scala.concurrent.{Promise, Future}

// Understands the twitter remote service
object TwitterClient {
  val twitterFactory = new AsyncTwitterFactory

  def tweet(text: String): Future[String] = {
    val p = Promise[String]()

    val twitter = twitterFactory.getInstance

    twitter.addListener(new TwitterClientUpdateListener(p))

    twitter.updateStatus(text)

    p.future
  }
}

class TwitterClientUpdateListener(val promise: Promise[String]) extends TwitterAdapter {
  override def updatedStatus(status: Status): Unit = {
    promise.success(status.getText)
  }

  override def onException(te: TwitterException, method: TwitterMethod): Unit = {
    promise.failure(te)
  }
}
