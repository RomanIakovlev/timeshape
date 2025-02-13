resolvers += Resolver.jcenterRepo

addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.13")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
libraryDependencies += "io.circe" %% "circe-parser" % "0.12.2"
