import scala.sys.process._
import _root_.io.circe.parser._

val dataVersion = "2025b"
val softwareVersion = "26"
val snapshotRelease = false

val releaseType = if (snapshotRelease) "-SNAPSHOT" else ""

val jacksonVersion = "com.fasterxml.jackson.core" % "jackson-core" % "2.17.2"

val commonSettings = Seq(
  organization := "net.iakovlev",
  sonatypeProfileName := "net.iakovlev",
  version := s"$dataVersion.$softwareVersion$releaseType",
  crossPaths := false,
  autoScalaLibrary := false,
  publishMavenStyle := true
)

val `commons-compress` = Seq(
  "org.apache.commons" % "commons-compress" % "1.26.1",
  "com.github.luben" % "zstd-jni" % "1.5.5-11"
)

lazy val timeshape = (project in file("."))
  .settings(commonSettings)
  .aggregate(core, builder, testApp, `geojson-proto`, benchmarks)
  .settings(publish / skip := true)

lazy val builderArgument = settingKey[String](
  "Argument to pass to builder, either local path to source data file or version to download")

lazy val releaseTask = taskKey[Unit](
  "Publishes an artifact and optionally makes a release if version is not a snapshot")

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    builderArgument := dataVersion,
    libraryDependencies ++= Seq(
      "com.esri.geometry" % "esri-geometry-api" % "2.2.4",
      "junit" % "junit" % "4.13.1" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
        exclude ("junit", "junit-dep"),
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "net.iakovlev" % "geojson-proto" % "1.1.5"
    ) ++ `commons-compress`,
    dependencyOverrides += jacksonVersion,
    name := "timeshape",
    publishTo := sonatypePublishTo.value,
    Compile / packageBin / packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> "net.iakovlev.timeshape"),
    Compile / doc / javacOptions := Seq("-Xdoclint:none"),
    Compile / javacOptions := Seq("-source", "8", "-target", "8"),
    Compile / resourceGenerators += Def.taskDyn {
      val log = streams.value.log
      val outputPath = (Compile / resourceManaged).value
      val outputFile = outputPath / "data.tar.zstd"
      outputPath.mkdirs()
      if (!outputFile.exists()) {
        log.info("Timeshape resource doesn't exist in this host, creating it now.")
        log.info("Downloading timezone data with version: " + builderArgument.value)
        val command =
          s"${(builder / stage).value}/bin/${(builder / name).value} ${builderArgument.value} ${outputFile.getAbsolutePath}"
        log.info(s"running $command")
        command.!
      } else {
        log.info("Timeshape resource exists, skipping creation")
      }
      Def.task(Seq(outputFile))
    }.taskValue,
    releaseTask := {
      publish.value
      val buildState = state.value
      if (!isSnapshot.value) Command.process("sonatypeRelease", buildState)
    }
  )

lazy val `geojson-proto` = (project in file("geojson-proto"))
  .settings(commonSettings)
  .settings(
    publishTo := sonatypePublishTo.value,
    version := "1.1.6-SNAPSHOT",
    Compile / PB.targets := Seq(
      PB.gens.java("3.25.6") -> (Compile / sourceManaged).value
    ),
    Compile / doc / javacOptions := Seq("-Xdoclint:none"),
    Compile / javacOptions := Seq("-source", "8", "-target", "8"),
    releaseTask := {
      publish.value
      val buildState = state.value
      if (!isSnapshot.value) Command.process("sonatypeRelease", buildState)
    }
  )


lazy val builder = (project in file("builder"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      jacksonVersion,
      "de.grundid.opendatalab" % "geojson-jackson" % "1.14"
    ) ++ `commons-compress`,
    name := "timeshape-builder",
    publish / skip := true
  )
  .dependsOn(`geojson-proto`)
  .enablePlugins(JavaAppPackaging)

lazy val testApp = (project in file("test-app"))
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq("org.openjdk.jol" % "jol-core" % "0.9",
                                "ch.qos.logback" % "logback-classic" % "1.2.13")
  )
  .dependsOn(core)

lazy val benchmarks = (project in file("benchmarks"))
  .settings(commonSettings)
  .settings(publish / skip := true)
  .dependsOn(core)
  .enablePlugins(JmhPlugin)
