package cl.otrimegistro.aerospikez

import org.specs2.mutable.Specification
import org.specs2.matcher.MapMatchers

import scala.collection.mutable.OpenHashMap

class SetOfSpec extends Specification with MapMatchers {
  sequential

  val myset = SetOf(Namespace("test"))

  "put(<a key>, <a value>)" should {

    "save the value in an empty bin name (\"\") into the specified record/key (return Unit if the write are success)" in {

      // this also help to test: get(<a key>)
      myset.put("key", "value").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>))
      myset.put("key1", "value1").run must beEqualTo(())
      myset.put("key2", "value2").run must beEqualTo(())
    }
  }

  "put(<a key>, <a value>, <a bin>)" should {

    "save the value in the specified bin, record/key into record/key (return Unit if the write are success)" in {

      // this also help to test: get("key", "bin")
      myset.put("key", "value", "bin").run must beEqualTo(())

      // this also help to test: get(<a key>, Bins(<one or more bin>))
      myset.put("number", 1, "one").run must beEqualTo(())
      myset.put("number", 2, "two").run must beEqualTo(())
      myset.put("number", 3, "three").run must beEqualTo(())

      // this also help to test: get(Keys(<one or more key>), <a bin>))
      myset.put("key4", "value4", "bin").run must beEqualTo(())
      myset.put("key5", "value5", "bin").run must beEqualTo(())
      myset.put("key6", "value6", "bin").run must beEqualTo(())
    }
  }

  "get(<a key>)" should {

    "return a Some(<a value>) is the key exists" in {

      myset.get("key").run must beSome("value")
    }

    "return a None if the key not exists" in {

      myset.get("nonExistentKey").run must beNone
    }
  }

  "get(<a key>, <a bin>)" should {

    "return a Some(<a value> is the key and bin exists" in {

      myset.get("key", "bin").run must beEqualTo(Some("value"))
    }

    "return None if the key or/and bin not exists" in {

      myset.get("key", "nonExistentBin").run must beEqualTo(None)
      myset.get("nonExistentKey", "bin").run must beEqualTo(None)
      myset.get("nonExistentKey", "nonExistentBins").run must beEqualTo(None)
    }
  }

  "get(Keys(<one or more key>)" should {

    "return a OpenHashMap with only the values that exists in that records/keys" in {

      val m1 = myset.get(Keys("key1", "key2")).run

      m1 must havePairs(("key1", "value1"), ("key2", "value2"))

      val m2 = myset.get(Keys("key1", "nonExistentKey")).run

      m2 must havePair(("key1", "value1"))
      m2 must not haveKey ("nonExistentKey")
    }

    "return a empty OpenHashMap if all keys not exists" in {

      myset.get(Keys("nonExistentKey1", "nonExistentKey2")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(Keys(<one or more key>), <a bin>)" should {

    "return a OpenHashMap with only the values that exists in the specified bin of that keys" in {

      val m1 = myset.get(Keys("key4", "key5", "key6"), "bin").run
      m1 must havePairs(("key4", "value4"), ("key5", "value5"), ("key6", "value6"))

      val m2 = myset.get(Keys("nonExistentKey", "key4", "key5"), "bin").run
      m2 must havePairs(("key4", "value4"), ("key5", "value5"))
      m2 must not haveKey ("nonExistenKey")
    }

    "return a empty OpenHashMap if the keys or bin or none exists" in {

      myset.get(Keys("nonExistentKey"), "bin2").run must beEqualTo(OpenHashMap.empty)

      myset.get(Keys("key2"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)

      myset.get(Keys("nonExistentKey"), "nonExistentBin").run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(key, Bins(<one or more bin>))" should {

    "return a OpenHashMap with only the values that exists in that keys with specified bins" in {

      val m1 = myset.get("number", Bins("one", "two", "three")).run

      m1 must havePairs(("one", 1), ("two", 2), ("three", 3))

      val m2 = myset.get("number", Bins("two", "nonExistentBin")).run

      m2 must havePair(("two", 2))
      m2 must not haveKey ("nonExistenBin")
    }

    "return a empty OpenHashMap if the key or bins or none exists" in {

      myset.get("nonExistentKey", Bins("one", "two")).run must beEqualTo(OpenHashMap.empty)

      myset.get("number", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)

      myset.get("nonExistentKey", Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "get(Keys(<one or more key>), Bins(<one or more bin>))" should {

    "return a OpenHashMap with an OpenHashMap as value (this contain the bin/value)" in {

      val m1 = myset.get(Keys("key4", "key5", "key6"), Bins("bin")).run

      m1 must haveKeys("key4", "key5", "key6")

      m1 must havePairs(
        ("key4", OpenHashMap("bin" -> "value4")),
        ("key5", OpenHashMap("bin" -> "value5")),
        ("key6", OpenHashMap("bin" -> "value6"))
      )

      m1.get("key4") must beSome(haveKey("bin"))
      m1.get("key5") must beSome(haveKey("bin"))
      m1.get("key6") must beSome(haveKey("bin"))

      val m2 = myset.get(Keys("key4", "key5", "nonExistentKey"), Bins("bin")).run

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

      myset.get(Keys("nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      myset.get(Keys("key4", "nonExistentKey"), Bins("nonExistentBin")).run must beEqualTo(OpenHashMap.empty)
      myset.get(Keys("nonExistentKey1", "nonExistentKey2"), Bins("bin")).run must beEqualTo(OpenHashMap.empty)
    }
  }

  "delete(<key>)" should {

    "remove a existing record/key" in {

      myset.get("key").run must beSome
      myset.delete("key").run must beEqualTo(())
      myset.get("key").run must beNone
    }

    "do nothing if the record/key not exists" in {

      myset.get("key").run must beNone
      myset.delete("key").run must beEqualTo(())
      myset.get("key").run must beNone
    }
  }

  "append(<a key>, <new string value>)" should {

    "concatenate the exists string value with the new string value (add after)" in {

      myset.put("key", "value").run
      myset.append("key", " new value").run must beEqualTo(())
      myset.get("key").run must beSome("value new value")
    }

    "add the string value if none value exists in that record/key" in {

      myset.delete("key").run
      myset.append("key", " new value").run must beEqualTo(())
      myset.get("key").run must beSome(" new value")
    }
  }

  "append(<a key>, <new string value>, <a bin>)" should {

    "concatenate the exists string value with the new string value (add after)" in {

      myset.put("key", "value", "bin").run
      myset.append("key", " new value", "bin").run must beEqualTo(())
      myset.get("key", "bin").run must beSome("value new value")
    }

    "add the string value if none value exists in the bin from that record" in {

      myset.delete("key").run
      myset.append("key", " new value", "bin").run must beEqualTo(())
      myset.get("key", "bin").run must beSome(" new value")
    }
  }

  "prepend(<a key>, <new string value>)" should {

    "concatenate the new string value (add before) with the exists string value" in {

      myset.put("key", "value").run
      myset.prepend("key", "new value ").run must beEqualTo(())
      myset.get("key").run must beSome("new value value")
    }

    "add the string value if none value exists in that record/key" in {

      myset.delete("key").run
      myset.prepend("key", "new value ").run must beEqualTo(())
      myset.get("key").run must beSome("new value ")
    }
  }

  "prepend(<a key>, <new string value>, <a bin>)" should {

    "concatenate the new string value (add before) with the exists string value" in {

      myset.put("key", "value", "bin").run
      myset.prepend("key", "new value ", "bin").run must beEqualTo(())
      myset.get("key", "bin").run must beSome("new value value")
    }

    "add the string value if none value exists in the bin from that record" in {

      myset.delete("key").run
      myset.prepend("key", "new value ", "bin").run must beEqualTo(())
      myset.get("key", "bin").run must beSome("new value ")
    }
  }
}
