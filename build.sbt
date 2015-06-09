organization := "fi.vm.sade"
name := "valintarekisteri"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.6"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blazeserver" % "0.7.0",
  "org.http4s" %% "http4s-dsl"         % "0.7.0",
  "org.http4s" %% "http4s-argonaut"    % "0.7.0",
  "org.http4s" %% "http4s-blazeclient" % "0.7.0",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "com.h2database" % "h2" % "1.3.176"



)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4",
  "org.typelevel" %% "scalaz-scalatest" % "0.2.2"
).map(_ % "test")

scalacOptions ++= Seq("-unchecked", "-deprecation")

