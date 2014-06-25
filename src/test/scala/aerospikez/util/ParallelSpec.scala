package aerospikez.util

import org.specs2.mutable.Specification

import aerospikez.{ AerospikeClient, Namespace }

import scalaz.syntax.monoid._
import scalaz.std.AllInstances._
import scalaz.concurrent.Task

import scalaz._, Scalaz._

class ParallelSpec extends Specification {

  val client = AerospikeClient()
  val set = client.setOf[Int](Namespace("test"), name = "set")

  def afterTest = client.close

  def f(v1: Option[Int],
        v2: Option[Int]): Int = v1.get + v2.get

  def f(v1: Option[Int],
        v2: Option[Int],
        v3: Option[Int]): Int = f(v1, v2) + v3.get

  def f(v1: Option[Int],
        v2: Option[Int],
        v3: Option[Int],
        v4: Option[Int]): Int = f(v1, v2) + f(v3, v4)

  def f(v1: Option[Int],
        v2: Option[Int],
        v3: Option[Int],
        v4: Option[Int],
        v5: Option[Int]): Int = f(v1, v2) + f(v3, v4) + v5.get

  def f(v1: Option[Int],
        v2: Option[Int],
        v3: Option[Int],
        v4: Option[Int],
        v5: Option[Int],
        v6: Option[Int]): Int = f(v1, v2) + f(v3, v4) + f(v5, v6)

  def f(v1: Option[Int],
        v2: Option[Int],
        v3: Option[Int],
        v4: Option[Int],
        v5: Option[Int],
        v6: Option[Int],
        v7: Option[Int]): Int = f(v1, v2) + f(v3, v4) + f(v5, v6) + v7.get

  import set._

  Task.gatherUnordered(Seq(
    put("one", 1),
    put("two", 2),
    put("three", 3),
    put("four", 4),
    put("five", 5),
    put("six", 6),
    put("seven", 7)
  )).run

  "Parallel" should {
    "run various tasks (Aerospike operations) in parallel and pass each value to a function" in {

      Parallel(get("one"), get("two"))(f).run must beEqualTo(3)
      Parallel(get("one"), get("two"), get("three"))(f).run must beEqualTo(6)
      Parallel(get("one"), get("two"), get("three"), get("four"))(f).run must beEqualTo(10)
      Parallel(get("one"), get("two"), get("three"), get("four"), get("five"))(f).run must beEqualTo(15)
      Parallel(get("one"), get("two"), get("three"), get("four"), get("five"), get("six"))(f).run must beEqualTo(21)
      Parallel(get("one"), get("two"), get("three"), get("four"), get("five"), get("six"), get("seven"))(f).run must beEqualTo(28)
    }
  }

  step(afterTest)
}
