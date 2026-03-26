# Timeshape

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.iakovlev/timeshape/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.iakovlev/timeshape/)
![Build status](https://github.com/RomanIakovlev/timeshape/actions/workflows/release.yml/badge.svg)
[![Gitter](https://badges.gitter.im/timeshape/community.svg)](https://gitter.im/timeshape/community)

Timeshape is a Java library that can be used to determine to which time zone a given geo coordinate belongs.
It's based on data published at
[https://github.com/evansiroky/timezone-boundary-builder/releases](https://github.com/evansiroky/timezone-boundary-builder/releases),
which itself is inherited from the OpenStreetMap data.

But what if knowing just time zone for a geo coordinate is not enough? Wouldn't it be nice to know more, like
administrative area or city neighborhood? Check out [GeoBundle](https://geobundle.com), now there's a Java library for that, too!

## Quote

> “Time is an illusion.”
>
> ― **Albert Einstein**

## Getting started

Timeshape is published on Maven Central. The coordinates are the following:

```xml
<dependency>
    <groupId>net.iakovlev</groupId>
    <artifactId>timeshape</artifactId>
    <version>2025b.26</version>
</dependency>
```

Starting from release 2023b.20, Timeshape re-introduces support for Java 8. It was inadevrtently dropped in one of the previous releases,
but now it's brought back.

## Android

Android developers should add Timeshape to the app level gradle.build as follows:

```gradle
implementation('net.iakovlev:timeshape:2025b.26') {
  // Exclude standard compression library
  exclude group: 'com.github.luben', module: 'zstd-jni'
}
// Import aar for native component compilation
implementation 'com.github.luben:zstd-jni:1.5.5-11@aar'
```

## Adopters

Are you using Timeshape? Please consider opening a pull request to list your organization here:

 * Name and website of your organization
 * [AirPing](https://airping.app/)
 * [Hopper](https://hopper.com/)
 * [Natural Light](https://play.google.com/store/apps/details?id=com.blackholeofphotography.naturallight)

## Using the library

The user API of library is in `net.iakovlev.timeshape.TimeZoneEngine` class. To use it, follow these steps:

#### Initialization

Initialize the class with the data for the whole world:

```java
import net.iakovlev.timeshape.TimeZoneEngine;
TimeZoneEngine engine = TimeZoneEngine.initialize();
```

Or, alternatively, initialize it with some bounding box only, to reduce memory usage:

```java
TimeZoneEngine engine = TimeZoneEngine.initialize(47.0599, 4.8237, 55.3300, 15.2486);
```

It is important to mention that for the time zone to be loaded by the second method,
it must be covered by the bounding box completely, not just intersect with it.

if you have difficulty determining the bounding box, you may prefer to use a list of ZoneIds instead.
```Java
Set<ZoneId> timeZones = new HashSet<> ();
timeZones.add(ZoneId.of("Europe/Berlin"));
timeZones.add(ZoneId.of("America/Detroit"));
TimeZoneEngine engine = TimeZoneEngine.initialize (timeZones, true);

```

During initialization, the data is read from resource and the index is built.
Initialization takes a significant amount of time (approximately 1 second), so do it only once in program lifetime.

Data can also be read from an external file, see overloads of `TimeZoneEngine.initialize` that accept
`TarArchiveInputStream`. The file format should be same as produced by running `builder` sbt project
(see [here](doc/Architecture.md#builder)).
It is responsibility of caller to close input stream passed to `TimeZoneEngine.initialize`.

There is an overload of `TimeZoneEngine.initialize` method that accepts a boolean parameter called
`accelerateGeometry`. If that parameter is equals to `true`, Timeshape will build a bitmap index
for each polygon it loads. This requires some extra memory and takes time at initialization, hence
that parameter is being `false` by default, however it speeds up the typical point-in-polygon query
approximately 2.5 times. See performance benchmarks in `AcceleratedGeometryBenchmark.java`, and
here is the result:
```
[info] Benchmark                                               Mode  Cnt   Score   Error  Units
[info] AcceleratedGeometryBenchmark.testAcceleratedEngine     thrpt   10  38.142 ± 0.448  ops/s
[info] AcceleratedGeometryBenchmark.testNonAcceleratedEngine  thrpt   10  15.289 ± 0.121  ops/s
```


##### Improving start up time by using serialization

Initialization on slow devices such as Android phones can take up to 20 seconds. This can be improved by
serializing an instance of the `TimeZoneEngine` and deserializing on subsequent runs of your program.
An example of serializing to a file can be found in `TimeZoneEngineSerializationTest.java` in the
unit tests. Serialization is especially useful if you can limit `TimeZoneEngine` to a portion of the
data set via a user preference.


#### Query for `java.time.ZoneId`:

Once initialization is completed, you can query the `ZoneId` based on latitude and longitude:

```java
import java.util.Optional;
import java.time.ZoneId;
Optional<ZoneId> maybeZoneId = engine.query(52.52, 13.40);
```

#### Multiple time zones for single geo point

Starting from release 2019b.7, the data source from which Timeshape is built contains overlapping geometries.
In other words, some geo points can simultaneously belong to multiple time zones. To accommodate this,
Timeshape has made the following change. It's now possible to query all the time zones, to which given
geo point belongs:

```java
List<ZoneId> allZones = engine.queryAll(52.52, 13.40);
```

The `Optional<ZoneId> query(double latitude, double longitude)` method is still there, and it still returns
maximum one `ZoneId`. If given geo point belongs to multiple time zones, only single one will be returned.
Which of multiple zones will be returned is entirely arbitrary. Because of this, method
`Optional<ZoneId> query(double latitude, double longitude)` is not suitable for use cases where such choice must
be deliberate. In such cases use `List<ZoneId> queryAll(double latitude, double longitude)` method and apply further
business logic to its output to choose the right time zone. Consult file
https://raw.githubusercontent.com/evansiroky/timezone-boundary-builder/master/expectedZoneOverlaps.json
for information about areas of the world where multiple time zones are to be expected.

#### Querying polyline

Timeshape supports querying of multiple sequential geo points (a polyline, e.g. a GPS trace) in an optimized way using method
`List<SameZoneSpan> queryPolyline(double[] points)`. Performance tests (see `net.iakovlev.timeshape.PolylineQueryBenchmark`)
show significant speedup of using this method for querying a polyline, comparing to separately querying each point from polyline
using `Optional<ZoneId> query(double latitude, double longitude)` method:

```
Benchmark                                  Mode  Cnt  Score   Error  Units
PolylineQueryBenchmark.testQueryPoints    thrpt    5  2,188 ▒ 0,044  ops/s
PolylineQueryBenchmark.testQueryPolyline  thrpt    5  4,073 ▒ 0,017  ops/s
```

### Architecture

See [dedicated document](doc/Architecture.md) for description of Timeshape internals.

### Versioning

Version of Timeshape consist of data version and software version, divided by a '.' symbol.
Data version is as specified at [https://github.com/evansiroky/timezone-boundary-builder/releases](https://github.com/evansiroky/timezone-boundary-builder/releases).
Software version is an integer, starting from 1 and incrementing for each published artifact.

## Licenses

The code of the library is licensed under the [MIT License](https://opensource.org/licenses/MIT).

The time zone data contained in library is licensed under the [Open Data Commons Open Database License (ODbL)](http://opendatacommons.org/licenses/odbl/).

## Endorsements

Timeshape uses YourKit.

<img src="https://www.yourkit.com/images/yklogo.png">

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
