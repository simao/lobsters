package io.simao.lobster

import twitter4j._

import scala.concurrent.{Promise, Future}

class TwitterClientUpdateListener(val promise: Promise[Status]) extends TwitterAdapter {
  override def updatedStatus(status: Status): Unit = {
    promise.success(status)
  }

  override def onException(te: TwitterException, method: TwitterMethod): Unit = {
    promise.failure(te)
  }
}

// Understands the twitter remote service
object TwitterClient {
  val twitter = AsyncTwitterFactory.getSingleton

  def tweet(text: String): Future[Status] = {
    val p = Promise[Status]()

    twitter.addListener(new TwitterClientUpdateListener(p))

    twitter.updateStatus(text)

    p.future
  }
}
