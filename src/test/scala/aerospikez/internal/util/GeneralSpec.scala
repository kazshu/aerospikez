package aerospikez.internal.util

import org.specs2.mutable.Specification

class GeneralSpec extends Specification {

  import General._

  "trySome" should {

    "convert a throw exception to a None and other values to Some(value)" in {

      def f(a: Int) = if (a == 0) throw new IllegalArgumentException(s"not null") else a

      trySome(f(2)) must beSome(2)
      trySome(f(0)) must beNone
    }
  }
}
