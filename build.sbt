name := "lobster"

version := "0.1.0"

scalaVersion := "2.11.4"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

libraryDependencies += "io.argonaut" %% "argonaut" % "6.0.4"

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.2"

libraryDependencies += "org.twitter4j" % "twitter4j-async" % "4.0.2"

// TODO: Use joda time instead
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "1.6.0"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.7"
