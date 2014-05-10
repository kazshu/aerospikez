import scala.reflect.ClassTag
import scalaz.NonEmptyList

package object aerospikez {

  import Util._

  object NonEmptyArray {
    def apply[A: ClassTag](h: A, t: A*): Array[A] = {
      (genericWrapArray(Array(h)) ++ t).toArray
    }
  }

  val Keys = NonEmptyArray
  val Bins = NonEmptyArray
  val Hosts = NonEmptyList

  case class Bin[V: SupportValue](_1: String, _2: V) extends Product2[String, V] {
    override def toString() = "(" + _1 + "," + _2 + ")"
  }
}
