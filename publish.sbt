publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

pomExtra in Global := {
  <url>https://github.com/RomanIakovlev/timeshape</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/RomanIakovlev/timeshape.git</connection>
      <developerConnection>scm:git:git@github.com:RomanIakovlev/timeshape.git</developerConnection>
      <url>github.com/RomanIakovlev/timeshape</url>
    </scm>
    <developers>
      <developer>
        <id>Roman Iakovlev</id>
        <name>Roman Iakovlev</name>
        <url>http://github.com/RomanIakovlev</url>
      </developer>
    </developers>
}
