resolvers += Resolver.jcenterRepo

addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
//addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.6.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.15")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0"