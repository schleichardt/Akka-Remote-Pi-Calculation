import _root_.sbt._
import _root_.sbt.Build
import _root_.sbt.Keys._
import _root_.sbt.Project
import sbt._
import sbt.Keys._

object ProjectBuild extends Build {

  val akkaVersion = "2.0.2"

  lazy val root = {
    Project(
      id = "root",
      base = file("."),
      settings = Project.defaultSettings ++ Seq(
        name := "Akka Learning",
        organization := "info.schleichardt",
        version := "0.1-SNAPSHOT",
        resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
        libraryDependencies ++= Seq(
          "com.typesafe.akka" % "akka-actor" % akkaVersion,
          "com.typesafe.akka" % "akka-remote" % akkaVersion,
          "com.typesafe.akka" % "akka-kernel" % akkaVersion)
      )
    )
  }
}

