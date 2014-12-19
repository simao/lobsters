package io.simao.lobster

import argonaut._, Argonaut._
import argonaut.{Parse, CodecJson}

import scala.io.Source
import scalaz.{-\/, \/-}

case class Config(lastUpdated: Option[String])

object Config {
  implicit def jsonConfigCodecJson: CodecJson[Config] =
    casecodec1(Config.apply, Config.unapply)("last_updated")

  def fromSource(s: Source): Config = from(s.mkString)

  def from(content: String): Config = {
    Parse.decodeEither[Config](content) match {
      case \/-(config) => config
      case -\/(msg) => throw new Exception("Error parsing config file: " + msg)
    }
  }
}
