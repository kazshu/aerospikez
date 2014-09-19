# aerospikez - Aerospike v3 Scala Client

A fast, asynchronous, composable and type safe Scala client for [Aerospike](http://www.aerospike.com/) v3.

### Key Points

- Wrapper over java library, to take advantage from Scalaz Stream.
- Type safe, Aerospike support only some types, so the compiler will check this for us.
- Idiomatic Scala as possible, e.g. avoid dealing with nulls, instead Scala's Option type is used.
- Avoid database configurations in the code that you write, this will relies in an file (see this [reference.conf](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf)).
- Concise & Usable, i.e not take care about creating a Key/Bin/Policy/Statement, etc that main result in boilerplate code.

## Installation

| Release Version | Scala compatible| Scalaz-Stream compatible | More Info         |
|-----------------|-----------------|--------------------------|-------------------|
| 0.2             | 2.10/2.11       | 0.5a                     | [notes](https://github.com/otrimegistro/aerospikez/blob/master/notes/0.2.md) / [guide](https://github.com/otrimegistro/aerospikez#guide) |
| 0.1             | 2.10            | 0.4.1a                   | [notes](https://github.com/otrimegistro/aerospikez/blob/master/notes/0.1.md) / [guide](https://github.com/otrimegistro/aerospikez/tree/v0.1#guide) |

```scala
resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "release-version"
```

##### Snapshot Version [![Build Status](https://secure.travis-ci.org/otrimegistro/aerospikez.png)](http://travis-ci.org/otrimegistro/aerospikez) [![Coverage Status](https://coveralls.io/repos/otrimegistro/aerospikez/badge.png?branch=master)](https://coveralls.io/r/otrimegistro/aerospikez?branch=master) [![Stories in Ready](https://badge.waffle.io/otrimegistro/aerospikez.png?label=Ready)](https://waffle.io/otrimegistro/aerospikez)

```scala
resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Scalaz Bintray Repo"    at "http://dl.bintray.com/scalaz/releases"
)

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "0.3-SNAPSHOT"
```

## What can you do?

To give an example we can use the [flights-analytics](https://github.com/aerospike/flights-analytics) (see the readme to get a basic idea):

- Clone the repo (we need only the udf and data):
`git clone https://github.com/aerospike/flights-analytics`

- In `sbt console` copy this code in the `:paste` mode (but wait, change the `path` value!):
```scala
import aerospikez.{ AerospikeClient, Namespace, Filter, Bin }
import scalaz.stream.io

val client = AerospikeClient()
val flight = client.setOf(Namespace("test"), "flights")
val format = new java.text.SimpleDateFormat("yyyy/MM/dd")
val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
val startDate = sdf.parse("2012-01-15").getTime/1000
val endDate = sdf.parse("2012-01-15").getTime/1000
val path = "/home/otrimegistro/flights-analytics/" // Change this where the repo was clone

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
    ).drain.run.runAsync(_ => ())
)).flatMap(_  => flight.createIndex[Int]("flight_date", "FL_DATE"))

val analytics = flight.queryAggregate[Map[String, Map[String,Long]]](
  Filter.range("FL_DATE", startDate, endDate), "simple_aggregation", "late_flights_by_airline"
)
```

- Now this will register the udf, create the secondary index and loading the data. This 3 step
are compose to ensure the requeriment of processing:
```scala
scala> preparation.runAsync(_ => println("Done!, the data is loading ..."))
```

- The previous task, simulate a flow of incomming data into your system, wich we can analyze in realtime:
```scala
scala> analytics.runLog.runAsync(println)
\/-(Vector(Map(OO -> Map(percent -> 22, late -> 20, flights -> 87), B6 -> Map(percent -> 18, late -> 27, flights -> 144), AA -> Map(percent -> 36, late -> 48, flights -> 131), YV -> Map(percent -> 26, late -> 11, flights -> 41), EV -> Map(percent -> 18, late -> 21, flights -> 115), UA -> Map(percent -> 46, late -> 23, flights -> 49), MQ -> Map(percent -> 22, late -> 17, flights -> 75), VX -> Map(percent -> 6, late -> 2, flights -> 30), HA -> Map(percent -> 0, late -> 0, flights -> 10)))
```

- Also we can consume repetitively, to manage each/time result against more data are arrive:
```scala
scala> val result = analytics.repeat.take(3).runLog.run
// ...
scala> result(0)
res1: Map[String,Map[String,Long]] = Map(US -> Map(percent -> 36, late -> 58, flights -> 159), OO -> Map(percent -> 22, late -> 40, flights -> 174), B6 -> Map(percent -> 18, late -> 27, flights -> 144), AA -> Map(percent -> 32, late -> 88, flights -> 268), YV -> Map(percent -> 26, late -> 11, flights -> 41), EV -> Map(percent -> 15, late -> 35, flights -> 220), UA -> Map(percent -> 40, late -> 34, flights -> 83), MQ -> Map(percent -> 22, late -> 34, flights -> 153), VX -> Map(percent -> 6, late -> 2, flights -> 30), HA -> Map(percent -> 0, late -> 0, flights -> 19))

scala> result(1)
res2: Map[String,Map[String,Long]] = Map(US -> Map(percent -> 38, late -> 83, flights -> 214), OO -> Map(percent -> 23, late -> 45, flights -> 193), B6 -> Map(percent -> 18, late -> 27, flights -> 144), AA -> Map(percent -> 34, late -> 102, flights -> 299), YV -> Map(percent -> 26, late -> 11, flights -> 41), EV -> Map(percent -> 17, late -> 41, flights -> 239), UA -> Map(percent -> 41, late -> 36, flights -> 87), MQ -> Map(percent -> 20, late -> 34, flights -> 164), VX -> Map(percent -> 6, late -> 2, flights -> 30), HA -> Map(percent -> 0, late -> 0, flights -> 20))

scala> result(2)
res3: Map[String,Map[String,Long]] = Map(US -> Map(percent -> 38, late -> 96, flights -> 248), OO -> Map(percent -> 22, late -> 45, flights -> 204), B6 -> Map(percent -> 18, late -> 27, flights -> 144), AA -> Map(percent -> 33, late -> 104, flights -> 314), YV -> Map(percent -> 26, late -> 11, flights -> 41), EV -> Map(percent -> 17, late -> 45, flights -> 252), UA -> Map(percent -> 41, late -> 39, flights -> 94), MQ -> Map(percent -> 20, late -> 34, flights -> 170), VX -> Map(percent -> 6, late -> 2, flights -> 30), HA -> Map(percent -> 0, late -> 0, flights -> 21))
```

## Guide

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
