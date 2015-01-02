package io.simao.util

class KestrelObj[T](value: T) {
  def tap(f: T ⇒ Unit): T = {
    f(value)
    value
  }
}

object KestrelObj {
  implicit def toKestrelObj[T](v: T): KestrelObj[T] = new KestrelObj(v)
}

