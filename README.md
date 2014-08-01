# aerospikez - Aerospike v3 Scala Client

aerospikez is a fast, asynchronous, concise, composable and type safe Scala Client for [Aerospike](http://www.aerospike.com/) v3.

## Installation

### Release Version

[Build](https://travis-ci.org/otrimegistro/aerospikez/builds/31360577) against:
- Scala 2.10.4
- Scalaz 7.1.0-M6
- Scalaz Stream 0.4.1a

```scala
resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "0.1"
```

### Snapshot Version

[![Build Status](https://secure.travis-ci.org/otrimegistro/aerospikez.png)](http://travis-ci.org/otrimegistro/aerospikez)
[![Stories in Ready](https://badge.waffle.io/otrimegistro/aerospikez.png?label=Ready)](https://waffle.io/otrimegistro/aerospikez)

```scala
resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Scalaz Bintray Repo"    at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "0.2-SNAPSHOT"
```

## Library Design & Philosophy

This is a wrapper over the aerospike java client, which tries to take advantage from Scalaz & Scalaz Stream
for convenience reasons:
- asynchronous operations in a exception-safe way.
- perform operations with execution control.
- implement/execute parallel operations.
- guarantee about resource safety.
- write composable computations.

Also the philosophy:
- Type safe, Aerospike support only some types, so the compiler will check this for us.
- Idiomatic Scala as possible, e.g. avoid dealing with nulls, instead Scala's Option type is used.
- Offer wrapper structures that are also check in compile time to prevent possible runtime exceptions.
- Avoid configurations in the code that you write, but instead relies upon their presence of an configuration file (see this [reference.conf](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf)).
- Concise & easily usable, i.e not take care about to creating a Key/Bin/Policy/Statement, etc that main result in boilerplate code.

## What can you do? quick example of data analytics

I will use the [flights-analytics](https://github.com/aerospike/flights-analytics) example (please see
the readme to get the idea).

- Clone the repo (we need only the udf and data):
`git clone https://github.com/aerospike/flights-analytics`

- In `sbt console` copy this in the `:paste` mode (but wait, change the `path` value!):
```scala
import aerospikez.{ AerospikeClient, Namespace, Filter, Bin }
import scalaz.stream.io

val client = AerospikeClient()
val flight = client.setOf(Namespace(), "flights")
val format = new java.text.SimpleDateFormat("yyyy/MM/dd")
val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
val startDate = sdf.parse("2012-01-15").getTime/1000
val endDate = sdf.parse("2012-01-15").getTime/1000
val path = "/home/otrimegistro/flights-analytics/"      // CHANGE THIS WHERE THE REPO WAS CLONE

val preparation = client.register("simple_aggregation.lua", path + "udf").map(_ =>
  (new java.io.File(path + "data")).listFiles.map(file =>
    io.linesR(file.toString).map(_.split(',')).evalMap(a =>
      flight.put(a(0).trim,
        Bin("YEAR", a(1).trim.toInt),
        Bin("DAY_OF_MONTH", a(2).trim.toInt),
        Bin("FL_DATE", format.parse(a(3).trim).getTime/1000),
        Bin("AIRLINE_ID", a(4).trim.toInt),
        Bin("CARRIER", a(5).trim),
        Bin("FL_NUM", a(6).trim.toInt),
        Bin("ORI_AIRPORT_ID", a(7).trim.toInt),
        Bin("ORIGIN", a(8).trim),
        Bin("ORI_CITY_NAME", a(9).trim),
        Bin("ORI_STATE_ABR", a(10).trim),
        Bin("DEST", a(11).trim),
        Bin("DEST_CITY_NAME", a(12).trim),
        Bin("DEST_STATE_ABR", a(13).trim),
        Bin("DEP_TIME", a(14).trim.toInt),
        Bin("ARR_TIME", a(15).trim.toInt),
        Bin("ELAPSED_TIME", a(16).trim.toInt),
        Bin("AIR_TIME", a(17).trim.toInt),
        Bin("DISTANCE", a(18).trim.toInt))
    ).drain.run.run
)).flatMap(_  => flight.createIndex[Int]("flight_date", "FL_DATE"))

val analytics = flight.queryAggregate[Map[String, java.util.HashMap[String,Long]]](
  Filter.range("FL_DATE", startDate, endDate), "simple_aggregation", "late_flights_by_airline"
)
```
- Now is time to register the udf, load the data (about 104 mb, so this will take some time) and create the secondary index:
```scala
scala> preparation.attemptRun
// res0: scalaz.\/[Throwable,Unit] = \/-(())
```
- Finally get the analytics:
```scala
scala> analytics.runLog.run
// res1: scala.collection.immutable.IndexedSeq[Map[String,java.util.HashMap[String,Long]]] = Vector(Map(DL -> {percent=26, late=838, flights=3188}, F9 -> {percent=45, late=194, flights=426}, US -> {percent=21, late=482, flights=2200}, OO -> {percent=19, late=604, flights=3100}, B6 -> {percent=17, late=303, flights=1779}, AA -> {percent=31, late=1334, flights=4200}, YV -> {percent=20, late=160, flights=776}, EV -> {percent=20, late=686, flights=3384}, FL -> {percent=16, late=200, flights=1222}, UA -> {percent=36, late=966, flights=2654}, MQ -> {percent=20, late=490, flights=2362}, WN -> {percent=22, late=1234, flights=5376}, AS -> {percent=18, late=192, flights=1041}, VX -> {percent=33, late=88, flights=260}, HA -> {percent=2, late=10, flights=378}))
```

## Documentation

- [Before Start](guide/before_start.md)
- [Cluster Connection Management](guide/connection_management.md)
  - [Connecting](guide/connection_management.md#connecting)
  - [Connection Check](guide/connection_management.md#connection-check)
  - [Listing active server nodes](guide/connection_management.md#listing-active-server-nodes)
  - [Close Connection](guide/connection_management.md#close-connection)
- [Set Creation](guide/creating_set.md)
- [Key Value Store](guide/key_value_store.md)
  - [Write Record](guide/key_value_store.md#write-record-operations)
  - [Read Record](guide/key_value_store.md#read-record-operations)
  - [Existence-Check](guide/key_value_store.md#existence-check-operations)
  - [Touch Record](guide/key_value_store.md#touch-operations)
  - [Delete Record](guide/key_value_store.md#delete-operations)
  - [Arithmetic Operations](guide/key_value_store.md#arithmetic-operations)
  - [String Operations](guide/key_value_store.md#string-operations)
  - [Generic Database Operations](guide/key_value_store.md#generic-database-operations)
- [Working with Lua UDF](guide/user_define_function.md)
  - [Register the UDF](guide/user_define_function.md#register-the-udf)
  - [Create a Index](guide/user_define_function.md#create-a-secondary-index)
  - [Execute](guide/user_define_function.md#execute)
  - [Query](guide/user_define_function.md#query)
  - [Aggregation](guide/user_define_function.md#aggregation)

## License

Copyright 2014 Omar González otrimegistro@gmail.com

Licensed under the [MIT License](https://raw.githubusercontent.com/otrimegistro/aerospikez/master/LICENSE).
