# Architecture
This document describes how the Timeshape is designed, which technologies it uses, and how to build it yourself.
This might be helpful if you want to contribute to it, or just to understand how it works.

## Overview
Timeshape is built around the idea of taking the source data with time zones in [GeoJSON](http://geojson.org/) format, 
convert it into something more efficient to store and read than GeoJSON, and package converted data together with
the code that can read and query it into a single artifact (JAR file). Efficiency is the key word here, because the
source data is quite big, and using it as is would impose too high memory and artifact size requirements on the users
of Timeshape.

Timeshape currently uses compressed [protocol buffers](https://developers.google.com/protocol-buffers/) 
(a.k.a. protobuf) as the target data format. The protobuf data is compressed using ZStandard method, which allows to reach 
relatively small artifact size: 19 MB total JAR size vs 57 MB for the source data only (GeoJSON compressed with zip). 
The biggest win in terms of size is, however, not due to efficiency of protobuf vs GeoJSON, but due to the fact that `float` 
is used instead of `double` to store geo coordinates in protobuf. 
This means, only 4+4=8 bytes are required for each point (latitude + longitude), instead of 8+8=16 bytes for `double`.
Precision of `float` is good enough for the source data.

At runtime, the code reads the packaged data and build a spatial index for querying. It uses 
[quad tree](https://en.wikipedia.org/wiki/Quadtree) for indexing, provided by the 
[Esri geometry API](https://github.com/Esri/geometry-api-java) Java library.

## Build structure
Timeshape uses [sbt](https://scala-sbt.org) as build system. The sbt build definition has 5 projects:
* geojson-protobuf
* core
* builder
* testApp
* benchmarks

Below you'll find some information about those individual projects. 
### geojson-protobuf
This project contains the protobuf definitions corresponding to GeoJSON format. 
Those definitions are in file `geojson-proto/src/main/protobuf/geojson.proto`. Java code to read and write such protobuf
messages is generated during compile time by [sbt-protoc](https://github.com/thesamet/sbt-protoc) sbt plugin.
Other projects (`core` and `builder`), which must read or write the protobuf, use those generated Java classes, and therefore
depend on `geojson-protobuf` in classpath sense.

### core
This project contains the logic to read the data into a quad tree and provide API for querying it. It's the main project
with which the library users interact, and provides the main published artifact. It uses sbt feature called
`resource generator` to create the protobuf file containing the time zone data. The code that actually generates the 
protobuf data file is in the [builder](#builder) project. The resource generator is run by sbt automatically when necessary.

### builder
This project is responsible for downloading the source data from Github and converting it from GeoJSON to protobuf format.
It's usually called from the resource generator of `core` project, but can be run independently 
(it's a standard Java application, after all).

### testApp
It's a playground, more or less. It's used to experiment with the main Timeshape artifact produced 
by the `core` project, particularly for the purpose of estimating its memory usage (see [Memory usage](#memory-usage)), 
and maybe something else in the future.

### benchmarks
This project contains JMH benchmarks. It was originally created to analyze performance of different ways to query
the polyline, and to compare performance of optimized polyline query method with querying each point in polyline
individually.

## Building
If you want to build and run the Timeshape locally, follow these steps:

1. Install sbt and JDK. It's proven to work on JDK 8, but newer versions might work too. Use latest versions, because
changes of time zones happen regularly in real world, and only the latest JDK build might reflect them. 
2. Go to the directory where the source code is checked out and run `sbt` command there.
3. When sbt finishes to load the build definition and you see sbt console, you have several options:
    * run `core/test` to execute the tests, they should pass.
    * run `testApp/run` to run the test app. It will query the data for one time zone and print the memory usage.
    * run `core/publishLocal` if you've made local modifications and want to use the modified version in your program.
    It will publish to the “local” [Ivy repository](https://www.scala-sbt.org/1.x/docs/Publishing.html#Publishing+locally).
    By default, this is at `$HOME/.ivy2/local/`.
    * run `core/publishM2` Similar to publishLocal, publishM2 task will publish the user’s Maven local repository.
    This is at the location specified by `$HOME/.m2/settings.xml` or at `$HOME/.m2/repository/` by default.
    Another sbt build would require `Resolver.mavenLocal` to resolve out of it.

Version must be set to `snapshot` for local publication to work best.

### local testing

If you want to use a modified version of timeshape in your program or perform some local testing include the local
repository in your build definition.

With sbt/scala:

```scala
resolvers += Resolver.mavenLocal
```

With gradle/java:

```groovy
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compile group: 'net.iakovlev', name: 'timeshape', version: '2023b.18-SNAPSHOT'
}
```

## Memory usage

The `testApp` project provides memory usage estimate by using [JOL](http://openjdk.java.net/projects/code-tools/jol/).
The current version's estimated footprint is roughly 128 MB of memory when the data for the whole world is loaded.
It is possible to further limit the memory usage by reducing the amount of time zones loaded. This is implemented by a call to
`TimeZoneEngine.initialize(double minlat, double minlon, double maxlat, double maxlon)`.
