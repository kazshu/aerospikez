package aerospikez

import com.aerospike.client.AerospikeException

import org.specs2.mutable.Specification
import org.specs2.matcher.MapMatchers

import scala.collection.mutable.OpenHashMap

import Operations._

class SetOfSpec extends Specification with MapMatchers {
  sequential

  def afterTests = client.close

  val client = AerospikeClient()
  client.register("record_example.lua", "src/test/resources/udf")
  client.register("sum_example.lua", "src/test/resources/udf")

  val set = client.setOf(Namespace("test"), name = "set")

  "set operations" >> {

    import set._

    "put(<key name>, <value>)" >> {
      // this also help to test: get(<key>)
      put("key", "value").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>))
      put("key1", "value1").run must beEqualTo(())
      put("key2", "value2").run must beEqualTo(())
    }

    "put(<key name>, <value>, <bin name>)" >> {
      // this also help to test: get("key", "bin")
      put("key", "value", "bin").run must beEqualTo(())

      // this also help to test: get(<key>, Bins(<one or more bin name>))
      put("number", 1, "one").run must beEqualTo(())
      put("number", 2, "two").run must beEqualTo(())
      put("number", 3, "three").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>), <a bin>))
      put("key4", "value4", "bin").run must beEqualTo(())
      put("key5", "value5", "bin").run must beEqualTo(())
      put("key6", "value6", "bin").run must beEqualTo(())
    }

    "putG(<key name>, <value>)" >> {
      delete("name").run
      putG("name", "James").run must beNone
      putG("name", "Bob").run must beSome("James")
    }

    "putG(<key name>, <value>, <bin name>)" >> {
      val last_user = 123056778

      delete(last_user).run
      putG(last_user, "James", "name").run must beNone
      putG(last_user, "Bob", "name").run must beSome("James")
    }

    "put(<key name>, <one or more Bin(<bin name>, <value>)>)" >> {
      put("example1", Bin("one", 1), Bin("two", 2)).run must beEqualTo(())
      get("example1", Bins("one", "two")).run must havePairs(("one", 1), ("two", 2))
    }

    "get(<key name>)" >> {
      get("key").run must beSome("value")
      get("nonExistentKey").run must beNone
    }

    "get(<key name>, <bin name>)" >> {
      get("key", "bin").run must beEqualTo(Some("value"))
      get("key", "nonExistentBin").run must beEqualTo(None)
      get("nonExistentKey", "bin").run must beEqualTo(None)
      get("nonExistentKey", "nonExistentBins").run must beEqualTo(None)
    }

    "get(Keys(<one or more key name>)" >> {
      val m1 = get(Keys("key1", "key2")).run
      m1 must havePairs(("key1", "value1"), ("key2", "value2"))

      val m2 = get(Keys("key1", "nonExistentKey")).run
      m2 must havePair(("key1", "value1"))
      m2 must not haveKey ("nonExistentKey")

      set.get(Keys("nonExistentKey1", "nonExistentKey2")).run must beEqualTo(OpenHashMap.empty)
    }

    "get(Keys(<one or more key name>), <bin name>)" >> {
      val m1 = get(Keys("key4", "key5", "key6"), "bin").run
      m1 must havePairs(("key4", "value4"), ("key5", "value5"), ("key6", "value6"))

      val m2 = get(Keys("nonExistentKey", "key4", "key5"), "bin").run
      m2 must havePairs(("key4", "value4"), ("key5", "value5"))
      m2 must not haveKey ("nonExistenKey")

      get(Keys("nonExistentKey"), "bin2").run must beEqualTo(OpenHashMap.empty)
      get(Keys("key2"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)
      get(Keys("nonExistentKey"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)
    }

    "get(<key name>, Bins(<one or more bin name>))" >> {
      val m1 = get("number", Bins("one", "two", "three")).run

      m1 must havePairs(("one", 1), ("two", 2), ("three", 3))

      val m2 = get("number", Bins("two", "nonExistentBin")).run
      m2 must havePair(("two", 2))
      m2 must not haveKey ("nonExistenBin")

      get("nonExistentKey", Bins("one", "two")).run must beEqualTo(OpenHashMap.empty)
      get("number", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      get("nonExistentKey", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
    }

    "get(Keys(<one or more key name>), Bins(<one or more bin name>))" >> {
      val m1 = get(Keys("key4", "key5", "key6"), Bins("bin")).run

      m1 must haveKeys("key4", "key5", "key6")
      m1 must havePairs(
        ("key4", OpenHashMap("bin" -> "value4")),
        ("key5", OpenHashMap("bin" -> "value5")),
        ("key6", OpenHashMap("bin" -> "value6"))
      )
      m1.get("key4") must beSome(haveKey("bin"))
      m1.get("key5") must beSome(haveKey("bin"))
      m1.get("key6") must beSome(haveKey("bin"))

      val m2 = get(Keys("key4", "key5", "nonExistentKey"), Bins("bin")).run

      m2 must haveKeys("key4", "key5")
      m2 must havePairs(
        ("key4", OpenHashMap("bin" -> "value4")),
        ("key5", OpenHashMap("bin" -> "value5"))
      )
      m2.get("nonExistentKey") must beNone
      m2.get("key4") must beSome(haveKey("bin"))
      m2.get("key5") must beSome(haveKey("bin"))

      get(Keys("nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      get(Keys("key4", "nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      get(Keys("nonExistentKey1", "nonExistentKey2"), Bins("bin")).run must beEqualTo(OpenHashMap.empty)
    }

    "delete(<key name>)" >> {
      get("key").run must beSome
      delete("key").run must beEqualTo(())
      get("key").run must beNone

      get("key").run must beNone
      delete("key").run must beEqualTo(())
      get("key").run must beNone
    }

    "touch(<key name>)" >> {
      delete("key").run
      put("key", "value").run
      val (gen1, exp1) = getHeader("key").run.get
      touch("key").run
      val (gen2, exp2) = getHeader("key").run.get
      gen2 must beEqualTo(gen1 + 1)
      exp2 must beGreaterThanOrEqualTo(exp1)
    }

    "append(<key name>, <new string value>)" >> {
      put("key", "value").run
      append("key", " new value").run must beEqualTo(())
      get("key").run must beSome("value new value")

      delete("key").run
      append("key", " new value").run must beEqualTo(())
      get("key").run must beSome(" new value")
    }

    "append(<key name>, <new string value>, <bin name>)" >> {
      put("key", "value", "bin").run
      append("key", " new value", "bin").run must beEqualTo(())
      get("key", "bin").run must beSome("value new value")

      delete("key").run
      append("key", " new value", "bin").run must beEqualTo(())
      get("key", "bin").run must beSome(" new value")
    }

    "prepend(<key name>, <new string value>)" >> {
      put("key", "value").run
      prepend("key", "new value ").run must beEqualTo(())
      get("key").run must beSome("new value value")

      delete("key").run
      prepend("key", "new value ").run must beEqualTo(())
      get("key").run must beSome("new value ")
    }

    "prepend(<key name>, <new string value>, <bin name>)" >> {
      put("key", "value", "bin").run
      prepend("key", "new value ", "bin").run must beEqualTo(())
      get("key", "bin").run must beSome("new value value")

      delete("key").run
      prepend("key", "new value ", "bin").run must beEqualTo(())
      get("key", "bin").run must beSome("new value ")
    }

    "add(<key name>, <num to sum>)" >> {
      put("num", 10).run
      add("num", 2).run must beEqualTo(())
      get("num").run must beSome(12)

      delete("num").run
      add("num", 10).run must beEqualTo(())
      get("num").run must beSome(10)
    }

    "add(<key name>, <num to sum>, <bin name>)" >> {
      put("num", 10, "positive").run
      add("num", 2, "positive").run must beEqualTo(())
      get("num", "positive").run must beSome(12)

      delete("num").run
      add("num", 10, "positive").run must beEqualTo(())
      get("num", "positive").run must beSome(10)
    }

    "exists(<key name>)" >> {
      put("key", "value").run
      exists("key").run must beTrue

      delete("key").run
      exists("key").run must beFalse
    }

    "exists(Keys(<one or more key name>))" >> {
      put("key1", "value1").run
      put("key2", "value2").run
      exists(Keys("key1", "key2")).run must havePairs(
        ("key1", true),
        ("key2", true)
      )

      delete("key1").run
      exists(Keys("key1", "key2")).run must havePairs(
        ("key1", false),
        ("key2", true)
      )

      delete("key2").run
      exists(Keys("key1", "key2")).run must havePairs(
        ("key1", false),
        ("key2", false)
      )
    }

    "getHeader(<key name>)" >> {
      delete("new key").run
      put("new key", "new value").run
      getHeader("new key").run must beSome.like {
        case t: Tuple2[Int, Int] ⇒ t._1 == 1
      }

      delete("new key").run
      getHeader("new key").run must beNone
    }

    "getHeader(Keys(<one or more key name>)" >> {
      put("one", 1).run
      put("two", 2).run
      val result = getHeader(Keys("one", "two")).run
      result must haveKeys("one", "two")
    }

    "operate(<key name>, <write & read Operations>)" >> {
      operate("num", Put(10), Get()).run must beSome(10)
      operate("num", Add(2), Get()).run must beSome(12)
      delete("name").run
      operate("name", Put("Bruce"), GetHeader()).run must beSome.like {
        case t: Tuple2[Int, Int] ⇒ t._1 == 1
      }
      operate("name", Append(" Lee"), Get()).run must beSome("Bruce Lee")
    }

    "createIndex[Type](<index name>, <bin name>)" >> {
      createIndex[Int]("index1", "num").run must beEqualTo(())
      createIndex[String]("index2", "name").run must beEqualTo(())
    }

    "execute(<key name>, <package name>, <function name>, <function arguments>)" >> {
      put("one", Bin("num", 1)).run
      execute("one", "record_example", "readBin", "num").run must beSome(1)
    }

    "execute(<a Filter>, <package name>, <function name>, <function arguments>)" >> {
      execute(Filter.equal("num", 1), "record_example", "readBin", "num").run must beEqualTo(())
    }

    "query(<a Filter>)" >> {
      put("person1", Bin("name", "Bob"), Bin("age", 27)).run
      put("person2", Bin("name", "Ana"), Bin("age", 24)).run

      query(
        Filter.equal("name", "Bob")
      ).runLog.run must contain((m: OpenHashMap[String, Any]) ⇒
          m must havePairs(
            ("name", "Bob"),
            ("age" -> 27)
          )
        )

      query(
        Filter.equal("name", "Ana")
      ).runLog.run must contain((m: OpenHashMap[String, Any]) ⇒
          m must havePairs(
            ("name", "Ana"),
            ("age" -> 24)
          )
        )
    }

    "queryAggregate(<a Filter>, <package name>, <function name>, <function arguments>)" >> {
      put("one", Bin("num", 1)).run
      put("two", Bin("num", 2)).run
      queryAggregate(
        Filter.range("num", 1, 3), "sum_example", "sum_single_bin", "num"
      ).runLog.run must contain(3)
    }

    "dropIndex(<index name>)" >> {
      dropIndex("index1").run must beEqualTo(())
      dropIndex("index2").run must beEqualTo(())
    }
  }

  step(afterTests)
}
