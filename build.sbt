val commonSettings = Seq(
  organization := "net.iakovlev",
  version := "2018d.1",
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
        exclude("junit", "junit-dep")
    ),
    name := "timeshape"
  )
  .dependsOn(protostuff)

lazy val builder = (project in file("builder"))
  .settings(commonSettings)
  .settings(
    libraryDependencies := Seq(
      "de.grundid.opendatalab" % "geojson-jackson" % "1.8",
      "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
      "org.apache.commons" % "commons-compress" % "1.14",
      "org.tukaani" % "xz" % "1.6"
    ),
    name := "timeshape-builder"
  )
  .dependsOn(protostuff)

lazy val protostuff = (project in file("protostuff"))
  .settings(commonSettings)
  .enablePlugins(ProtobufPlugin)
