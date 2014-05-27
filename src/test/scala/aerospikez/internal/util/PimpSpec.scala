package aerospikez.internal.util

import org.specs2.mutable.Specification

class PimpSpec extends Specification {

  import Pimp._

  "toOption" should {

    "convert a null to a None and other values to Some(value)" in {

      null.asInstanceOf[Any].toOption must beNone

      "a word".toOption must beSome("a word")
    }
  }

  "toOpenHashMap" should {

    "convert a java.util.HashMap to a scala.collection.mutable.OpenHashMap" in {
      import scala.collection.mutable.OpenHashMap

      val m1 = new java.util.HashMap[String, Int]

      m1.put("one", 1)

      val m2 = m1.toOpenHashMap().run

      m2 must beAnInstanceOf[OpenHashMap[String, Int]]

      m2.get("one") must beSome(1)
    }
  }
}
