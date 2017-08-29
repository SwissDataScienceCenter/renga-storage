organization := "ch.datascience"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
name := "renga-storage"

lazy val root = (project in file("."))
  .enablePlugins(
    PlayScala
  )

resolvers += "jitpack" at "https://jitpack.io"
resolvers += "Oracle Released Java Packages" at "http://download.oracle.com/maven"
resolvers += "SDSC Snapshots" at "https://testing.datascience.ch:18081/repository/maven-snapshots/"

lazy val renga_version = "0.1.0-SNAPSHOT"
libraryDependencies += "ch.datascience" %% "renga-graph-core" % renga_version
libraryDependencies += "ch.datascience" %% "renga-commons" % renga_version

lazy val janusgraph_version = "0.1.0"

libraryDependencies += filters
libraryDependencies += "org.janusgraph" % "janusgraph-cassandra" % janusgraph_version //% Runtime
libraryDependencies += "io.minio" % "minio" % "3.0.3"
libraryDependencies += "org.javaswift" % "joss" % "0.9.7"
libraryDependencies += cache
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "2.8.47" % Test


import com.typesafe.sbt.packager.docker._
lazy val newEntrypoint = "bin/docker-entrypoint.sh"
mappings in Docker += (file(".") / "docker-entrypoint.sh") -> ((defaultLinuxInstallLocation in Docker).value + s"/$newEntrypoint")
dockerBaseImage := "openjdk:8-jre-alpine"
dockerExposedVolumes += "/data"
dockerCommands := Seq(
  Cmd("FROM", dockerBaseImage.value),
  ExecCmd("RUN", "apk", "add", "--no-cache", "bash"),
  Cmd("WORKDIR", (defaultLinuxInstallLocation in Docker).value),
  Cmd("ADD", "opt", "/opt"),
  ExecCmd("RUN", "chown", "-R", "daemon:daemon", "."),
  ExecCmd("RUN", "mkdir" +: "-p" +: dockerExposedVolumes.value: _*),
  ExecCmd("RUN", "chown" +: "-R" +: "daemon:daemon" +: dockerExposedVolumes.value: _*),
  ExecCmd("VOLUME", dockerExposedVolumes.value: _*),
  ExecCmd("RUN", "chmod", "+x", newEntrypoint),
  ExecCmd("ENTRYPOINT", newEntrypoint +: dockerEntrypoint.value: _*),
  ExecCmd("CMD")
)


// Source code formatting
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

val preferences =
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference( AlignArguments,                               true  )
    .setPreference( AlignParameters,                              true  )
    .setPreference( AlignSingleLineCaseStatements,                true  )
    .setPreference( AlignSingleLineCaseStatements.MaxArrowIndent, 40    )
    .setPreference( CompactControlReadability,                    true  )
    .setPreference( CompactStringConcatenation,                   false )
    .setPreference( DanglingCloseParenthesis,                     Force )
    .setPreference( DoubleIndentConstructorArguments,             true  )
    .setPreference( DoubleIndentMethodDeclaration,                true  )
    .setPreference( FirstArgumentOnNewline,                       Force )
    .setPreference( FirstParameterOnNewline,                      Force )
    .setPreference( FormatXml,                                    true  )
    .setPreference( IndentPackageBlocks,                          true  )
    .setPreference( IndentSpaces,                                 2     )
    .setPreference( IndentWithTabs,                               false )
    .setPreference( MultilineScaladocCommentsStartOnFirstLine,    false )
    .setPreference( NewlineAtEndOfFile,                           true  )
    .setPreference( PlaceScaladocAsterisksBeneathSecondAsterisk,  false )
    .setPreference( PreserveSpaceBeforeArguments,                 false )
    .setPreference( RewriteArrowSymbols,                          false )
    .setPreference( SpaceBeforeColon,                             false )
    .setPreference( SpaceBeforeContextColon,                      true  )
    .setPreference( SpaceInsideBrackets,                          false )
    .setPreference( SpaceInsideParentheses,                       true  )
    .setPreference( SpacesAroundMultiImports,                     true  )
    .setPreference( SpacesWithinPatternBinders,                   false )

SbtScalariform.scalariformSettings ++ Seq(preferences)

// Publishing
publishTo := {
  val nexus = "https://testing.datascience.ch:18081/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "repository/maven-snapshots/")
  else
    None //TODO
}
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
