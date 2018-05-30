val commonSettings = Seq(
  organization := "net.iakovlev",
  version := "2018d.2-SNAPSHOT",
  crossPaths := false,
  autoScalaLibrary := false,
  publishMavenStyle := true
)

lazy val core = (project in file("core"))
  .enablePlugins(JvmPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies := Seq(
      "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
      "org.apache.commons" % "commons-compress" % "1.14",
      "org.tukaani" % "xz" % "1.6",
      "junit" % "junit" % "4.11" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
        exclude ("junit", "junit-dep")
    ),
    name := "timeshape",
    fork := true,
    javaOptions += "-Xmx174m"
  )
  .dependsOn(protostuff)

lazy val builder = (project in file("builder"))
  .settings(commonSettings)
  .settings(
    libraryDependencies := Seq(
      "de.grundid.opendatalab" % "geojson-jackson" % "1.8",
      "org.apache.commons" % "commons-compress" % "1.14",
      "org.tukaani" % "xz" % "1.6"
    ),
    name := "timeshape-builder"
  )
  .dependsOn(protostuff)

lazy val protostuff = (project in file("protostuff"))
  .settings(commonSettings)
  .enablePlugins(ProtobufPlugin)

lazy val testApp = (project in file("test-app"))
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("net.iakovlev.timeshape.testapp.Main"))
  .dependsOn(core)
