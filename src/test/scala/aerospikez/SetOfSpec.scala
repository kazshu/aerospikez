package aerospikez

import com.aerospike.client.AerospikeException

import org.specs2.mutable.Specification
import org.specs2.matcher.MapMatchers

import scala.collection.mutable.OpenHashMap

import Operations._

class SetOfSpec extends Specification with MapMatchers {
  sequential

  def afterTests = { client.close }

  val client = AerospikeClient()

  val set = client.setOf(Namespace("test"))

  "put(<a key>, <a value>)" should {

    "save the value in an empty bin name (\"\") into the specified key/record (return Unit if the write are success)" in {

      // this also help to test: get(<a key>)
      set.put("key", "value").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>))
      set.put("key1", "value1").run must beEqualTo(())
      set.put("key2", "value2").run must beEqualTo(())
    }
  }

  "put(<a key>, <a value>, <a bin>)" should {

    "save the value in the specified bin into record/key (return Unit if the write are success)" in {

      // this also help to test: get("key", "bin")
      set.put("key", "value", "bin").run must beEqualTo(())

      // this also help to test: get(<a key>, Bins(<one or more bin>))
      set.put("number", 1, "one").run must beEqualTo(())
      set.put("number", 2, "two").run must beEqualTo(())
      set.put("number", 3, "three").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>), <a bin>))
      set.put("key4", "value4", "bin").run must beEqualTo(())
      set.put("key5", "value5", "bin").run must beEqualTo(())
      set.put("key6", "value6", "bin").run must beEqualTo(())
    }
  }

  "putG(<a key>, <a value>)" should {

    "save the value in the specified record/key and return the old value (None if empty)" in {

      set.delete("name").run
      set.putG("name", "Omar").run must beNone
      set.putG("name", "Anibal").run must beSome("Omar")
    }
  }

  "putG(<a key>, <a value>, <a bin>)" should {

    "save the value in the specified bin into the record/key and return the old value (None if empty)" in {

      val last_user = 123056778

      set.delete(last_user).run
      set.putG(last_user, "Omar", "name").run must beNone
      set.putG(last_user, "Anibal", "name").run must beSome("Omar")
    }
  }

  "put(<a key>, <one or more Bin(<a bin>, <a value>)>" should {

    "save different Bin in the same record/key" in {

      set.put("example1", Bin("one", 1), Bin("two", 2)).run must beEqualTo(())
      set.get("example1", Bins("one", "two")).run must havePairs(("one", 1), ("two", 2))
    }
  }

  "get(<a key>)" should {

    "return a Some(<a value>) is the key exists" in {

      set.get("key").run must beSome("value")
    }

    "return a None if the key not exists" in {

      set.get("nonExistentKey").run must beNone
    }
  }

  "get(<a key>, <a bin>)" should {

    "return a Some(<a value> is the key and bin exists" in {

      set.get("key", "bin").run must beEqualTo(Some("value"))
    }

    "return None if the key or/and bin not exists" in {

      set.get("key", "nonExistentBin").run must beEqualTo(None)
      set.get("nonExistentKey", "bin").run must beEqualTo(None)
      set.get("nonExistentKey", "nonExistentBins").run must beEqualTo(None)
    }
  }

  "get(Keys(<one or more key>)" should {

    "return a OpenHashMap with only the values that exists in that records/keys" in {

      val m1 = set.get(Keys("key1", "key2")).run

      m1 must havePairs(("key1", "value1"), ("key2", "value2"))

      val m2 = set.get(Keys("key1", "nonExistentKey")).run

      m2 must havePair(("key1", "value1"))
      m2 must not haveKey ("nonExistentKey")
    }

    "return a empty OpenHashMap if all keys not exists" in {

      set.get(Keys("nonExistentKey1", "nonExistentKey2")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(Keys(<one or more key>), <a bin>)" should {

    "return a OpenHashMap with only the values that exists in the specified bin of that keys" in {

      val m1 = set.get(Keys("key4", "key5", "key6"), "bin").run
      m1 must havePairs(("key4", "value4"), ("key5", "value5"), ("key6", "value6"))

      val m2 = set.get(Keys("nonExistentKey", "key4", "key5"), "bin").run
      m2 must havePairs(("key4", "value4"), ("key5", "value5"))
      m2 must not haveKey ("nonExistenKey")
    }

    "return a empty OpenHashMap if the keys or bin or none exists" in {

      set.get(Keys("nonExistentKey"), "bin2").run must beEqualTo(OpenHashMap.empty)

      set.get(Keys("key2"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)

      set.get(Keys("nonExistentKey"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(key, Bins(<one or more bin>))" should {

    "return a OpenHashMap with only the values that exists in that keys with specified bins" in {

      val m1 = set.get("number", Bins("one", "two", "three")).run

      m1 must havePairs(("one", 1), ("two", 2), ("three", 3))

      val m2 = set.get("number", Bins("two", "nonExistentBin")).run

      m2 must havePair(("two", 2))
      m2 must not haveKey ("nonExistenBin")
    }

    "return a empty OpenHashMap if the key or bins or none exists" in {

      set.get("nonExistentKey", Bins("one", "two")).run must beEqualTo(OpenHashMap.empty)

      set.get("number", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)

      set.get("nonExistentKey", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(Keys(<one or more key>), Bins(<one or more bin>))" should {

    "return a OpenHashMap with an OpenHashMap as value (this contain the bin/value)" in {

      val m1 = set.get(Keys("key4", "key5", "key6"), Bins("bin")).run

      m1 must haveKeys("key4", "key5", "key6")

      m1 must havePairs(
        ("key4", OpenHashMap("bin" -> "value4")),
        ("key5", OpenHashMap("bin" -> "value5")),
        ("key6", OpenHashMap("bin" -> "value6"))
      )

      m1.get("key4") must beSome(haveKey("bin"))
      m1.get("key5") must beSome(haveKey("bin"))
      m1.get("key6") must beSome(haveKey("bin"))

      val m2 = set.get(Keys("key4", "key5", "nonExistentKey"), Bins("bin")).run

      m2 must haveKeys("key4", "key5")

      m2 must havePairs(
        ("key4", OpenHashMap("bin" -> "value4")),
        ("key5", OpenHashMap("bin" -> "value5"))
      )

      m2.get("nonExistentKey") must beNone
      m2.get("key4") must beSome(haveKey("bin"))
      m2.get("key5") must beSome(haveKey("bin"))
    }

    "return a empty OpenHashMap if all keys or/and all bins not exists (respectively)" in {

      set.get(Keys("nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      set.get(Keys("key4", "nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      set.get(Keys("nonExistentKey1", "nonExistentKey2"), Bins("bin")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "delete(<key>)" should {

    "remove a existing record/key" in {

      set.get("key").run must beSome
      set.delete("key").run must beEqualTo(())
      set.get("key").run must beNone
    }

    "do nothing if the record/key not exists" in {

      set.get("key").run must beNone
      set.delete("key").run must beEqualTo(())
      set.get("key").run must beNone
    }
  }

  "append(<a key>, <new string value>)" should {

    "concatenate the exists string value with the new string value (add after)" in {

      set.put("key", "value").run
      set.append("key", " new value").run must beEqualTo(())
      set.get("key").run must beSome("value new value")
    }

    "add the string value if none value exists in that record/key" in {

      set.delete("key").run
      set.append("key", " new value").run must beEqualTo(())
      set.get("key").run must beSome(" new value")
    }
  }

  "append(<a key>, <new string value>, <a bin>)" should {

    "concatenate the exists string value with the new string value (add after)" in {

      set.put("key", "value", "bin").run
      set.append("key", " new value", "bin").run must beEqualTo(())
      set.get("key", "bin").run must beSome("value new value")
    }

    "add the string value if none value exists in the bin from that record" in {

      set.delete("key").run
      set.append("key", " new value", "bin").run must beEqualTo(())
      set.get("key", "bin").run must beSome(" new value")
    }
  }

  "prepend(<a key>, <new string value>)" should {

    "concatenate the new string value (add before) with the exists string value" in {

      set.put("key", "value").run
      set.prepend("key", "new value ").run must beEqualTo(())
      set.get("key").run must beSome("new value value")
    }

    "add the string value if none value exists in that record/key" in {

      set.delete("key").run
      set.prepend("key", "new value ").run must beEqualTo(())
      set.get("key").run must beSome("new value ")
    }
  }

  "prepend(<a key>, <new string value>, <a bin>)" should {

    "concatenate the new string value (add before) with the exists string value" in {

      set.put("key", "value", "bin").run
      set.prepend("key", "new value ", "bin").run must beEqualTo(())
      set.get("key", "bin").run must beSome("new value value")
    }

    "add the string value if none value exists in the bin from that record" in {

      set.delete("key").run
      set.prepend("key", "new value ", "bin").run must beEqualTo(())
      set.get("key", "bin").run must beSome("new value ")
    }
  }

  "add(<a key>, <num to sum>)" should {

    "sum the exists number in the record/key with the number passed as second argument" in {

      set.put("num", 10).run
      set.add("num", 2).run must beEqualTo(())
      set.get("num").run must beSome(12)
    }

    "put the number in the record/key if not exists a number" in {

      set.delete("num").run
      set.add("num", 10).run must beEqualTo(())
      set.get("num").run must beSome(10)
    }
  }

  "add(<a key>, <num to sum>, <a bin>)" should {

    "sum the exists number in the specified bin into record/key with the <num to sum>" in {

      set.put("num", 10, "positive").run
      set.add("num", 2, "positive").run must beEqualTo(())
      set.get("num", "positive").run must beSome(12)
    }

    "put the <num to sum> in the specified bin into record/key if not exists a number" in {

      set.delete("num").run
      set.add("num", 10, "positive").run must beEqualTo(())
      set.get("num", "positive").run must beSome(10)
    }
  }

  "exists(<a key>)" should {

    "return true or false if the record/key exists or not" in {

      set.put("key", "value").run
      set.exists("key").run must beTrue

      set.delete("key").run
      set.exists("key").run must beFalse
    }
  }

  "exists(Keys(<one or more key>))" should {

    "return a OpenHashMap with the specified keys exists or not" in {

      set.put("key1", "value1").run
      set.put("key2", "value2").run
      set.exists(Keys("key1", "key2")).run must havePairs(
        ("key1", true),
        ("key2", true)
      )

      set.delete("key1").run
      set.exists(Keys("key1", "key2")).run must havePairs(
        ("key1", false),
        ("key2", true)
      )

      set.delete("key2").run
      set.exists(Keys("key1", "key2")).run must havePairs(
        ("key1", false),
        ("key2", false)
      )
    }
  }

  "getHeader(<a key>)" should {

    "return the record (if exists) generation and expiration only for specified key" in {
      set.delete("new key").run
      set.put("new key", "new value").run
      set.getHeader("new key").run must contain("gen: 1, exp: ")
    }

    "return a string message if the record not exists" in {

      set.delete("new key").run
      set.getHeader("new key").run must contain("record no found")
    }
  }

  /*"operate(<a key>, <one or more Operations>)" should {

    "return the value of last operation that are applied" in {

      val op1 = set.operate("num", Put(10), Add(2), Get()).run
      val op2 = set.operate("name", Put("Chuck"), Append(" Norris"), Get()).run

      op1 must beSome(12)
      op2 must beSome("Chuck Norris")
    }
  }*/

  step(afterTests)
}
