# aerospikez - Aerospike v3 Scala Client

aerospikez is under development, which aims to be a fast, asynchronous, concise,
composable and type safe Scala Client for [Aerospike](http://www.aerospike.com/) v3.

## Current Status [![Build Status](https://secure.travis-ci.org/otrimegistro/aerospikez.png)](http://travis-ci.org/otrimegistro/aerospikez)

Built against:
- Scala 2.10.4
- Scalaz 7.1.0-M6
- Scalaz Stream 0.4.1a

Integration test against Aerospike Server 3.2.9

## Installation

#### Latest Version

No`release available for now.

#### Snapshot Version

Add the following to your SBT build:
``` scala
resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "0.1-SNAPSHOT"
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

## Usage

### Before start

**AerospikeClient()** and **Namespace()** are consumer of configuration information, so keep in mind that if the
[config file](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf) are:
- missing, but you provide configurations as argument then these are applied.
- encounter and also you provide configurations as argument, the config file always wins.
- missing and you not provide a configuration as argument, then defaults values (from this library) are applied.

### Cluster Connection Management

#### Connecting

```scala
import aerospikez.AerospikeClient

val client = AerospikeClient()    // default by the library: localhost and 3000 port
```

Other alternatives:

- a diferent name of config file:
```scala
import com.typesafe.config.ConfigFactory
import aerospikez.AerospikeClient

val client = AerospikeClient(configFile = ConfigFactory.load("myconfig.conf"))
```

- if you want to specified hosts (remember if you use the config file, this is no necessary):
```scala
import aerospikez.{ AerospikeClient , Hosts }

val client = AerospikeClient(Hosts("203.87.169.58:3000", "203.87.169.59:3000"))
```

#### Connection check

```scala
client.isConnected
```

#### Listing active server nodes

```scala
client.getNodes
```

#### Close Connection

```scala
client.close
```

### Creating a Set

**Note:** In aerospikez all operations are associative only to a Set, your only use a client for general purpose action
(cluster connectivity, set creation and udf registers).

You need to provide the name of the namespace that are use as argument to Namespace() (please
see your namespace in `/etc/aerospike/aerospike.conf`), otherwise the `test` namespace are used.
```scala
import aerospikez.Namespace

val set = client.setOf(Namespace("test"), name = "myset")
```

**Considerations about type safety:**
In the sense of the data model of Aerospike a Set not impose any restriction on the records,
so when you read values from the database this are applied to type Any as default. You have
two alternatives to mitigate this problem:
- Pass a type parameter (for key and value) in the operation to ensure a type.
```scala
// You get the value in the record "one" as Int
scala> set.get[String, Int]("one")
res1: scalaz.concurrent.Task[Option[Int]] = scalaz.concurrent.Task@7c71b4b9
```
- Inform the type in the creation of the Set, wich is use if you are only need to work with records
that store values of specified type.
```scala
// This Set can only work with records that save values of type Int:
val set = client.setOf[Int](Namespace(), name = "myset")

// You get the value as Int (no need to pass a type parameter)
scala> set.get("one")
res2: scalaz.concurrent.Task[Option[Int]] = scalaz.concurrent.Task@47049007

scala> set.put("one", 1L)
<console>:13: error: That Set has been forced to accept only Int or Option[Int] as Value, but you provide a Long.
              set.put("one", 1L)
                     ^
```
I recommend use the last alternative as possible because is safe and clean (*also note the support for `Option[Int]`
wich will be useful to avoid `Option.get` when write composable computations that end in a write
record operations*).
The first alternative allows work with more flexibility, but do not complain if
you make a mistake in the type parameter and you receive a `ClassCastException`.

### Working with Key Value Store

All operations are a Scalaz `Task`

#### Write Record Operations

- put
```scala
// put(<key name>, <a value>)
set.put("one", 1).run                            // Unit


// put(<key name>, <a value>, <bin name>)
set.put("number", 1, "mult identity").run        // Unit


// put(<key name>, <one or more Bin(<bin name>, <a value>)>)
import aerospikez.Bin
set.put("example1", Bin("one", 1), Bin("two", 2)).run
```

- putG (*put the value and get the old value.*)
```scala
// putG(<key name>, <a value>)
set.putG("name", "Bruce").run                    // None
set.putG("name", "Chuck").run                    // Some(Bruce)


// putG(<key name>, <a value>, <bin name>)
val user_id = scala.util.Random.nextInt()
set.putG(user_id, "Bruce", "name").run           // None
set.putG(user_id, "Bruce Lee", "name").run       // Some(Bruce)
```

#### Read Record Operations

- get
```scala
// get(<key name>)
set.put("one", 1).run                            // Unit
set.get("one").run                               // Some(1)


// get(<key name>, <bin name>)
val id_user = scala.util.Random.nextInt()
set.put(
  id_user,
  Map("full name" -> "Chuck Norris", "age" -> 74),
  "personal info"
).run
set.get(id_user, "personal info").run            // Some(Map(full name -> Chuck Norris, age -> 74))


// get(Keys(<one or more key name>))
import aerospikez.Keys
set.put("two", 2).run                            // Unit
set.get(Keys("one", "two")).run                  // OpenHashMap(two -> 2, one -> 1)


// get(Keys(<one or more key name>), <bin name>)
val user_1 = scala.util.Random.nextInt()
val user_2 = user_1 + 1
set.put(user_1, "Bruce Lee", "name").run         // Unit
set.put(user_2, "Chuck Norris", "name").run      // Unit
set.get(Keys(user_1, user_2), "name").run        // OpenHashMap(1893 -> Bruce Lee, 1894 -> Chuck Norris)


// get(<key name>, Bins(<one or more bin name>))
import aerospikez.Bins
set.put("num", 0, "add id").run                  // Unit
set.put("num", 1, "mult id").run                 // Unit
set.get("num", Bins("add id", "mult id")).run    // OpenHashMap(mult id -> 1, add id -> 0)


// get(Keys(<one or more key name>), Bins(<one or more bin name>))
set.get(Keys("num"), Bins("add id")).run         // OpenHashMap(num -> OpenHashMap(add identity -> 0))
```

- getHeader
```scala
// getHeader(<key name>)
set.getHeader("two").run                         // Some(1, 139517791)

// getHeader(Keys(<one or more key name>))
set.getHeader(Keys("one", "two")).run            // OpenHashMap(two -> Some((1,141756487)), one -> Some((1,139517791)))
```

#### Existence-Check Operations

- exists
```scala
// exists(<key name>)
set.exists("one").run                            // true
set.exists("three").run                          // false


// exists(Keys(<one or more key name>)
set.exists(Keys("one", "three").run              // OpenHashMap(three -> false, one -> true)
```

#### Touch Operations

- touch
```scala
set.getHeader("key").run                         // Some((1,142093244))
// touch(<key name>)
set.touch("key").run                             // Unit
set.getHeader("key").run                         // Some((2,142093306))
```

#### Delete Operations

- delete
```scala
// delete(<key name>)
set.exists("one").run                            // true
set.delete("one").run                            // Unit
set.exists("one").run                            // false
```

#### Arithmetic Operations

- add
```scala
// add(<key name>, <a Int value>)
set.put("two", 1).run                            // Unit
set.add("two", 1).run                            // Unit
set.get("two").run                               // Some(2)
```

#### String Operations

- append
```scala
// append(<key name>, <a String value>)
set.put("example1", "hello").run                 // Unit
set.append("example1", " world!").run            // Unit
set.get("example1").run                          // Some(hello world!)
```

- prepend
```scala
// prepend(<key name>, <a String value>)
set.put("example2", "world!").run                // Unit
set.prepend("example2", "hello ").run            // Unit
set.get("example2").run                          // Some(hello world!)
```

#### Generic Database Operations

- operate
```scala
import aerospikez-Operations._

// operate(<key name>, <one or more operations>)
set.operate("numbers", put("one", 1), put("two", 2), put("three", 3), get("two")).run            // Some(2)

set.operate("names", put("agent1", "James"), put("agent2", "Jack")).run                          // Some(())
set.operate("names", append("agent1", " Bond"), append("agent2", " Bourber"), get("agent1")).run // Some("James Bond")
```
**Note:** Aerospike support only a write operation in a same bin, this will no check for this library for performance reason:
```scala
scala> set.operate("example", put("num", 1), add("num", 10)).run
com.aerospike.client.AerospikeException: Error Code 4: Parameter error
  at com.aerospike.client.async.AsyncRead.parseResult(AsyncRead.java:93)
  at com.aerospike.client.async.AsyncSingleCommand.read(AsyncSingleCommand.java:67)
  at com.aerospike.client.async.SelectorManager.processKey(SelectorManager.java:164)
  at com.aerospike.client.async.SelectorManager.runCommands(SelectorManager.java:108)
  at com.aerospike.client.async.SelectorManager.run(SelectorManager.java:69)
```

### Working with User Define Funtions

**1) Register the UDF**

A UDF is register via client instance (so we can use from different Set):
```scala
// register(<source-file>, <path>, <language>)
client.register("record_example.lua", "src/main/udf")  // "Lua" as default language

client.register("record_example.lua")                  // "udf/" as default path and "Lua" as default language
```

`$ cat record_example.lua`
```lua
function readBin(record, binName)
  return record[binName]
end
```

**2) Create a Secondary Index**

To perform a query and execute (with Filter) you must create a secondary index:
- On numeric indexes, user can run equality and range queries:
```scala
// createIndex[Int](<index name>, <bin name>)
set.createIndex[Int]("index1", "number")
```
- On string indexes, only equality queries are available.
```scala
// createIndex[String](<index name>, <bin name>)
set.createIndex[String]("index2", "name")
```

**Note**: When you need to remove a Index use `set.dropIndex(<index name>)`

### Execute

Is a Scalaz `Task`, this will execute (when you "run" the computation) the UDF againt a specified record
(if you use a key name) or to one/more records (if you use a Filter).

- **execute (using a key name):** you may expect a result from `return` of the UDF.
```scala
set.put("one", Bin("number", 1)).run
// execute(<key name>, <udf package name>, <udf function name>, <one or more function arguments>)
set.execute("one", "record_example", "readBin", "number").run                     // Some(1)
```

- **execute (using a Filter):** you not expect a result from the UDF.
```scala
import aerospikez.Filter

set.put("one", Bin("number", 1)).run
set.put("tow", Bin("number", 2)).run

// execute(<a Filter>, <udf package name>, <udf function name>, <one or more function arguments>)
set.execute(Filter.range("num", 1, 3), "record_example", "readBin", "number").run // Unit
```

### Query

Is a Scalaz Stream `Process`, this will receive as input the output from the UDF stream. Because aeropikez will manage
the query as io resource, only when you emit values from `Process` the UDF will compute (not before).

Details for query/queryAggregate examples:
```scala
import aerospikez.Filter

(0 to 100).map( i => put(i, Bin("number", i)).run )
```
`$ cat stream_example.lua`
```lua
function example(stream, binName)
  local function readBin(record)
    return record[binName]
  end

  return stream : map(readBin)
end
```

- **query:** this emit each record (as OpenHashMap)

```scala
// query(<a Filter>)
set.query(Filter.range("number", 1, 10)).runLog.run

// Vector(OpenHashMap(number -> 1), OpenHashMap(number -> 2), OpenHashMap(number -> 3), OpenHashMap(number -> 4),
// OpenHashMap(number -> 5), OpenHashMap(number -> 6), OpenHashMap(number -> 7), OpenHashMap(number -> 8),
// OpenHashMap(number -> 9), OpenHashMap(number -> 10))
```

- **queryAggregate:** this emit each values from the bin of record

```scala
// queryAggregate[T](<a Filter>, <package name>, <function name>, <one or more funtion arguments>)
set.queryAggregate[Long](Filter.range("number", 1, 100), "stream_example", "example", "number").runLog.run

// Vector(32, 64, 96, 1, 33, 65, 97, 2, 34, 66, 98, 3, 35, 67, 99, 4, 36, 68, 100, 5, 37, 69, 6, 38, 70, 7,
// 39, 71, 8, 40, 72, 9, 41, 73, 10, 42, 74, 11, 43, 75, 12, 44, 76, 13, 45, 77, 14, 46, 78, 15, 47, 79, 16,
// 48, 80, 17, 49, 81, 18, 50, 82, 19, 51, 83, 20, 52, 84, 21, 53, 85, 22, 54, 86, 23, 55, 87, 24, 56, 88, 25,
// 57, 89, 26, 58, 90, 27, 59, 91, 28, 60, 92, 29, 61, 93, 30, 62, 94, 31, 63, 95)
```

## License

Copyright 2014 Omar González otrimegistro@gmail.com

Licensed under the [MIT License](https://raw.githubusercontent.com/otrimegistro/aerospikez/master/LICENSE).
