organization := "net.iakovlev"
name := "timeshape"
version := "2018d.1"

crossPaths := false
autoScalaLibrary := false
publishMavenStyle := true

libraryDependencies ++= Seq(
  "de.grundid.opendatalab" % "geojson-jackson" % "1.8",
  "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
  "org.junit.jupiter" % "junit-jupiter-api" % "5.1.0" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.1.0" % Test,
  "org.junit.vintage" % "junit-vintage-engine" % "5.1.0" % Test
)
