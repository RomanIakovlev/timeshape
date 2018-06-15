val dataVersion = "2018d"
val softwareVersion = "3"
val sevenZSupport = Seq(
  "org.apache.commons" % "commons-compress" % "1.14",
  "org.tukaani" % "xz" % "1.6",
)
val commonSettings = Seq(
  organization := "net.iakovlev",
  sonatypeProfileName := "net.iakovlev",
  version := s"$dataVersion.$softwareVersion",
  crossPaths := false,
  autoScalaLibrary := false,
  publishMavenStyle := true
)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
      "junit" % "junit" % "4.11" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
        exclude ("junit", "junit-dep")
    ) ++ sevenZSupport,
    name := "timeshape",
    fork := true,
    javaOptions += "-Xmx174m",
    publishTo := Some(Opts.resolver.sonatypeStaging)
  )
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
  .dependsOn(core)

lazy val testApp = (project in file("test-app"))
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("net.iakovlev.timeshape.testapp.Main"),
    skip in publish := true)
  .dependsOn(core)
