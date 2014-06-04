# Working with User Define Funtions

## Register the UDF

A UDF is register via client instance (so we can use from different Set):
```scala
// register(<source-file>, <path>, <language>)  where <path> and <language>
// are optional; "udf/" as default path and "LUA" as default language

client.register("record_example.lua")
client.register("stream_example.lua")
```

`$ cat record_example.lua`
```lua
function readBin(record, binName)
  return record[binName]
end
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

## Create a Secondary Index

To perform a query, queryAggregate and execute (with Filter) you must create a secondary index:
- On numeric indexes, user can run equality and range queries:
```scala
// createIndex[Int](<index name>, <bin name>)
set.createIndex[Int]("index1", "number").run
```
- On string indexes, only equality queries are available.
```scala
// createIndex[String](<index name>, <bin name>)
set.createIndex[String]("index2", "name").run
```

**Note**: When you need to remove a the Index use: `set.dropIndex(<index name>)`

## Execute

Is a Scalaz `Task`, this will execute (when you "run" the computation) the UDF againt a specified record
(if you use a key name) or to one/more records (if you use a Filter).

### execute (using a key name)

*You may expect a result from `return` of the UDF.*
```scala
import aerospikez.Bin

set.put("one", Bin("number", 1)).run

// execute(<key name>, <udf package name>, <udf function name>, <one or more function arguments>)
set.execute("one", "record_example", "readBin", "number").run                     // Some(1)
```

### execute (using a Filter)

*You not expect a result from the UDF.*

```scala
import aerospikez.{ Bin, Filter }

set.put("one", Bin("number", 1)).run
set.put("tow", Bin("number", 2)).run

// execute(<a Filter>, <udf package name>, <udf function name>, <one or more function arguments>)
set.execute(Filter.range("num", 1, 3), "record_example", "readBin", "number").run // Unit
```

## Query

Is a Scalaz Stream `Process`, this will receive as input the output from the UDF stream. Because aeropikez will
manage the query as io resource, only when you emit values from `Process` the UDF will compute (not before).
A query emit each record (wrapper in a OpenHashMap):
```scala
import aerospikez.{ Bin, Filter }

(0 to 100).map( i => put(i, Bin("number", i)).run )

// query(<a Filter>)
set.query(Filter.range("number", 1, 10)).runLog.run
// Vector(OpenHashMap(number -> 1), OpenHashMap(number -> 2), OpenHashMap(number -> 3),
// OpenHashMap(number -> 4), OpenHashMap(number -> 5), OpenHashMap(number -> 6),
// OpenHashMap(number -> 7), OpenHashMap(number -> 8), OpenHashMap(number -> 9),
// OpenHashMap(number -> 10))
```

## Aggregation

Is a Scalaz Stream `Process`, this will receive as input the output from the UDF stream. Because aeropikez will
manage the query as io resource, only when you emit values from `Process` the UDF will compute (not before).
A queryAggregate emit each values from the bin of record:

```scala
import aerospikez.{ Bin, Filter }

(0 to 100).map( i => put(i, Bin("number", i)).run )

// queryAggregate[T](<a Filter>, <package name>, <function name>, <one or more funtion arguments>)
set.queryAggregate[Long](Filter.range("number", 1, 100), "stream_example", "example", "number").runLog.run
// Vector(32, 64, 96, 1, 33, 65, 97, 2, 34, 66, 98, 3, 35, 67, 99, 4, 36, 68, 100, 5, 37, 69,
// 6, 38, 70, 7, 39, 71, 8, 40, 72, 9, 41, 73, 10, 42, 74, 11, 43, 75, 12, 44, 76, 13, 45, 77,
// 14, 46, 78, 15, 47, 79, 16, 48, 80, 17, 49, 81, 18, 50, 82, 19, 51, 83, 20, 52, 84, 21, 53,
// 85, 22, 54, 86, 23, 55, 87, 24, 56, 88, 25, 57, 89, 26, 58, 90, 27, 59, 91, 28, 60, 92, 29,
// 61, 93, 30, 62, 94, 31, 63, 95)
```
