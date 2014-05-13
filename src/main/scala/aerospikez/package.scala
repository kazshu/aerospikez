import scala.reflect.ClassTag
import scalaz.NonEmptyList

package object aerospikez {

  import internal.util.TSafe._

  object NonEmptyArray {
    def apply[A: ClassTag](h: A, t: A*): Array[A] = {
      (genericWrapArray(Array(h)) ++ t).toArray
    }
  }

  case class Bin[V: SupportValue](_1: String, _2: V) extends Product2[String, V] {
    override def toString() = "(" + _1 + "," + _2 + ")"
  }

  val Keys = NonEmptyArray
  val Bins = NonEmptyArray
  val Hosts = NonEmptyList
}
