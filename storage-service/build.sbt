name := """storage-service"""
organization := "ch.datascience"

version := "0.1.0-SNAPSHOT"

lazy val root = Project(
  id   = "storage-service",
  base = file(".")
).dependsOn(
  core,
  mutationClient,
  serviceCommons
).enablePlugins(PlayScala)


lazy val core = RootProject(file("../graph-core"))
lazy val mutationClient = RootProject(file("../graph-mutation-client"))
lazy val serviceCommons = RootProject(file("../service-commons"))

scalaVersion := "2.11.8"
lazy val janusgraph_version = "0.1.0"

libraryDependencies += filters
libraryDependencies ++= Seq(
  "org.janusgraph" % "janusgraph-cassandra" % janusgraph_version, //% Runtime
  "io.minio" % "minio" % "3.0.3",
  "org.javaswift" % "joss" % "0.9.7",
  cache,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
)

resolvers ++= Seq(
  DefaultMavenRepository,
  Resolver.mavenLocal
)

import com.typesafe.sbt.packager.docker._

dockerBaseImage := "openjdk:8-jre-alpine"
//dockerBaseImage := "openjdk:8-jre"

dockerCommands ~= { cmds => cmds.head +: ExecCmd("RUN", "apk", "add", "--no-cache", "bash") +: cmds.tail }

