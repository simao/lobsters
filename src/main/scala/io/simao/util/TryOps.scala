package io.simao.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

object TryOps {
  implicit def eitherToTry[T](either: Either[Throwable, T]): Try[T] = either match {
    case Left(t) ⇒ Failure(t)
    case Right(v) ⇒ Success(v)
  }

  implicit def futureEitherToTry[T](futureEither: Future[Either[Throwable, T]])(implicit ec: ExecutionContext): Future[Try[T]] = {
    futureEither.map(eitherToTry)
  }
}
