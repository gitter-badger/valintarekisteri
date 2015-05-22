organization := "fi.vm.sade"
name := "valintarekisteri"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blazeserver" % "0.7.0",
  "org.http4s" %% "http4s-dsl"         % "0.7.0",
  "org.http4s" %% "http4s-argonaut"    % "0.7.0"
)
