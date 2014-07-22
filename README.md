# aerospikez - Aerospike v3 Scala Client

aerospikez is under development, which aims to be a fast, asynchronous, concise,
composable and type safe Scala Client for [Aerospike](http://www.aerospike.com/) v3.

## Current Status [![Build Status](https://secure.travis-ci.org/otrimegistro/aerospikez.png)](http://travis-ci.org/otrimegistro/aerospikez)

Built against:
- Scala 2.10.4
- Scalaz 7.1.0-M6
- Scalaz Stream 0.4.1a

Integration test against Aerospike Server 3.3.5

## Installation

Add the following to your SBT build:
``` scala
resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Scalaz Bintray Repo"    at "http://dl.bintray.com/scalaz/releases"
)

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
