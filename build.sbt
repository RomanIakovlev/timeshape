import scala.sys.process._

val dataVersion = "2018d"
val softwareVersion = "4"
val sevenZSupport = Seq(
  "org.apache.commons" % "commons-compress" % "1.14",
  "org.tukaani" % "xz" % "1.6"
)
val commonSettings = Seq(
  organization := "net.iakovlev",
  sonatypeProfileName := "net.iakovlev",
  version := s"$dataVersion.$softwareVersion",
  crossPaths := false,
  autoScalaLibrary := false,
  publishMavenStyle := true
)

lazy val timeshape = (project in file("."))
  .settings(commonSettings)
  .aggregate(core, builder, testApp, `geojson-proto`)
  .settings(skip in publish := true)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
      "junit" % "junit" % "4.11" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
        exclude ("junit", "junit-dep"),
      "org.slf4j" % "slf4j-api" % "1.7.25"
    ) ++ sevenZSupport,
    name := "timeshape",
    publishTo := sonatypePublishTo.value,
    resourceGenerators in Compile += Def.taskDyn {
      val log = streams.value.log
      val outputPath = (resourceManaged in Compile).value
      val outputFile = outputPath / "output.pb.7z"
      outputPath.mkdirs()
      if (!outputFile.exists()) {
        log.info("Timeshape resource doesn't exist, creating")
        val command =
          s"java -jar ${(builder / assembly).value} $dataVersion ${outputFile.getAbsolutePath}"
        log.info(s"running $command")
        command.!
      } else {
        log.info("Timeshape resource exists, skipping creation")
      }
      Def.task(Seq(outputFile))
    }.taskValue
  )
  .dependsOn(`geojson-proto`)

lazy val `geojson-proto` = (project in file("geojson-proto"))
  .settings(commonSettings)
  .settings(publishTo := sonatypePublishTo.value, version := "1.0.0")
  .enablePlugins(ProtobufPlugin)

lazy val builder = (project in file("builder"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "de.grundid.opendatalab" % "geojson-jackson" % "1.8"
    ) ++ sevenZSupport,
    name := "timeshape-builder",
    skip in publish := true
  )
  .dependsOn(`geojson-proto`)

lazy val testApp = (project in file("test-app"))
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("net.iakovlev.timeshape.testapp.Main"),
    skip in publish := true,
    libraryDependencies ++= Seq("org.openjdk.jol" % "jol-core" % "0.9",
                                "ch.qos.logback" % "logback-classic" % "1.2.3")
  )
  .dependsOn(core)
