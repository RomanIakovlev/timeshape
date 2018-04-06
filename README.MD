# Timeshape

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.iakovlev/timeshape/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.iakovlev/timeshape/)

## Quote
> “Time is an illusion.”
>
> ― **Albert Einstein**

Timeshape is a Java library that can be used to determine to which time zone a given geo coordinate belongs.
It's based on data published at [https://github.com/evansiroky/timezone-boundary-builder/releases](https://github.com/evansiroky/timezone-boundary-builder/releases).

## Getting started

Timeshape is published on Maven Central. The coordinates are the following:

```xml
<dependency>
    <groupId>net.iakovlev</groupId>
    <artifactId>timeshape</artifactId>
    <version>2018d.1</version>
</dependency>
``` 

## Using the library

The user API of library is in `net.iakovlev.TimeZoneEngine` class. To use it, follow these steps:

* Initialize the class:

```java
import net.iakovlev.TimeZoneEngine;
TimeZoneEngine engine = TimeZoneEngine.initialize();
```
During initialization, the data is read from resource and the index is built. Initialization takes a significant amount of time (several seconds), so do it only once in program lifetime.

* Query for `java.time.ZoneId`:
Once initialization is completed, you can query the `ZoneId` based on latitude and longitude:

```java
import java.util.Optional;
import java.time.ZoneId;
Optional<ZoneId> maybeZoneId = engine.query(52.52, 13.40);
``` 

## Versioning
Version of Timeshape consist of data version and software version, divided by a '.' symbol. 
Data version is as specified at [https://github.com/evansiroky/timezone-boundary-builder/releases](https://github.com/evansiroky/timezone-boundary-builder/releases). 
Software version is an integer, starting from 1 and incrementing for each published artifact.

## Licenses

The code of the library is licensed under the [MIT License](https://opensource.org/licenses/MIT).

The time zone data contained in library is licensed under the [Open Data Commons Open Database License (ODbL)](http://opendatacommons.org/licenses/odbl/).
