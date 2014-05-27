import com.aerospike.client.query.{ Filter ⇒ AFilter }
import scala.reflect.ClassTag
import scalaz.NonEmptyList

package object aerospikez {

  import internal.util.TSafe._

  val Keys = NonEmptyArray
  val Bins = NonEmptyArray
  val Hosts = NonEmptyList

  object NonEmptyArray {
    def apply[A: ClassTag](h: A, t: A*): Array[A] = {
      (genericWrapArray(Array(h)) ++ t).toArray
    }
  }

  case class Bin[+V: VRestriction](_1: String, _2: V) extends Product2[String, V] {
    override def toString() = "(" + _1 + "," + _2 + ")"
  }

  object Filter {
    def equal(bin: String, value: Long): AFilter = AFilter.equal(bin, value)
    def equal(bin: String, value: String): AFilter = AFilter.equal(bin, value)
    def range(bin: String, startNum: Long, endNum: Long): AFilter = AFilter.range(bin, startNum, endNum)
  }
}
