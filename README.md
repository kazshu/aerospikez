# aerospikez [![Build Status](https://secure.travis-ci.org/otrimegistro/aerospikez.png)](http://travis-ci.org/otrimegistro/aerospikez)

aerospikez is under development, which aims to be a fast, asynchronous, concise,
 composable and type safe Scala Client for [Aerospike](http://www.aerospike.com/) v3.

### Installation

#### Latest Version

No release available for now (coming soon).

#### Snapshot Version

Is built against Scala 2.10.4 and 2.11.0, simple add the following to your SBT build:
``` scala
resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.otrimegistro" %% "aerospikez" % "0.1-SNAPSHOT"
```

## Library Design & Philosophy

This is a wrapper over the aerospike java client, which tries to take advantage of Scalaz Task, for
convenience reasons:
- perform asynchronous operations in a exception-safe way.
- write composable computations (because is a Monad).
- take some other advanced use with scalaz-stream.

Also the philosophy:
- Idiomatic Scala as possible, i.e. avoid dealing with nulls, instead Scala's Option type is used.
- Type safe, Aerospike support only some types, so the compiler will check this for us. Also convenient
factory data structures are offers to check (in compile time) that you not pass empty arguments.
- Avoid SDK configurations in the code that you write, but instead relies upon their presence of an configuration file (see this [reference.conf](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf) which also is use for testing).
- Concise and easily usable; simply use the operations and run, and not take care about to creating a Policy, a Key, a Bin, etc.

## Basic Usage

### Before start

**AerospikeClient()** and **Namespace()** are consumer of configuration information, so keep in mind that if the
[config file](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf) are:
- missing, but you provide configurations as argument then these are applied.
- encounter and also you provide configurations as argument, the config file always wins.
- missing and you not provide a configuration as argument, then defaults values (from this library) are applied.

### Connecting to your Aerospike Cluster

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

### Close Connection

```scala
client.close
```

### Creating a Set

You need to provide the name of the namespace that are use as argument to Namespace() (please
see `/etc/aerospike/aerospike.conf`), in case that you no pass a name, the `test` namespace are used.
```scala
import aerospikez.Namespace

val myset = client.setOf(Namespace("test"))        // or Namespace()
```

### Operations

All operations in aerospikez are a Scalaz Task, so you need to "run" the computation and get the return value.
**If you are not a Scalaz friend, you only need to know and write:** `myset.<operation>.run`, where `<operation>`
can be:

#### put

```scala
// put(<a key>, <a value>)
myset.put("one", 1).run                            // Unit


// put(<a key>, <a value>, <a bin>)
myset.put("number", 1, "mult identity").run         // Unit
```

#### putG

*put the value and get the old value.*
```scala
// putG(<a key>, <a value>)
myset.putG("name", "Bruce").run                    // None
myset.putG("name", "Chuck").run                    // Some(Bruce)


// putG(<a key>, <a value>, <a bin>)
val user_id = scala.util.Random.nextInt()
myset.putG(user_id, "Bruce", "name").run           // None
myset.putG(user_id, "Bruce Lee", "name").run       // Some(Bruce)
```

#### get

```scala
// get(<a key>)
myset.put("one", 1).run
myset.get("one").run                               // Some(1)


// get(<a key>, <a bin>)
val id_user = scala.util.Random.nextInt()
myset.put(
  id_user,
  Map("full name" -> "Chuck Norris", "age" -> 74),
  "personal info"
).run
myset.get(id_user, "personal info").run            // Some(Map(full name -> Chuck Norris, age -> 74))


// get(Keys(<one or more key>))
import aerospikez.Keys
myset.put("two", 2).run
myset.get(Keys("one", "two")).run                  // OpenHashMap(two -> 2, one -> 1)


// get(Keys(<one or more key>), <a bin>)
val user_1 = scala.util.Random.nextInt()
val user_2 = user_1 + 1
myset.put(user_1, "Bruce Lee", "name").run
myset.put(user_2, "Chuck Norris", "name").run
myset.get(Keys(user_1, user_2), "name").run        // OpenHashMap(1893 -> Bruce Lee, 1894 -> Chuck Norris)


// get(<a key>, Bins(<one or more bin>))
import aerospikez.Bins
myset.put("num", 0, "add id").run
myset.put("num", 1, "mult id").run
myset.get("num", Bins("add id", "mult id")).run    // OpenHashMap(mult id -> 1, add id -> 0)


// get(Keys(<one or more key>), Bins(<one or more bin>))
myset.get(Keys("num"), Bins("add id")).run         // OpenHashMap(num -> OpenHashMap(add identity -> 0))
```

#### delete

```scala
// delete(<a key>)
myset.exists("one").run                            // true
myset.delete("one").run
myset.exists("one").run                            // false
```

#### getHeader

```scala
// getHeader(<a key>)
myset.getHeader("two").run                         // Some(gen: 1, exp: 139517791)
```

#### exists

```scala
// exists(<a key>)
myset.exists("one").run                            // true
myset.exists("three").run                          // false


// exists(Keys(<one or more key>)
myset.exists(Keys("one", "three").run              // OpenHashMap(three -> false, one -> true)
```

#### add

```scala
// add(<a key>, <a Int or Long value>)
myset.put("two", 1).run
myset.add("two", 1).run
myset.get("two").run                               // Some(2)
```

#### append

```scala
// append(<a key>, <a String value>)
myset.put("example1", "hello").run
myset.append("example1", " world!").run
myset.get("example1").run                         // Some(hello world!)
```

#### prepend

```scala
// prepend(<a key>, <a String value>)
myset.put("example2", "world!").run
myset.prepend("example2", "hello ").run
myset.get("example2").run                           // Some(hello world!)
```

## License

Copyright 2014 Omar González otrimegistro@gmail.com

Licensed under the [MIT License](https://raw.githubusercontent.com/otrimegistro/aerospikez/master/LICENSE).
