## Creating a Set

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
