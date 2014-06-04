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

## Guide

[Before Start](guide/before_start.md)
[Connection Management](guide/connection_management.md)
[Creating a Set](creating_set.md)
[Working with Key Value Store](key_value_store.md)
[Working with User Define Funtions](user_define_function.md)

## License

Copyright 2014 Omar González otrimegistro@gmail.com

Licensed under the [MIT License](https://raw.githubusercontent.com/otrimegistro/aerospikez/master/LICENSE).
