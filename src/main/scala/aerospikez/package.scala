import com.aerospike.client.query.{ Filter ⇒ AFilter }
import scala.reflect.ClassTag
import scalaz.NonEmptyList

package object aerospikez {

  import internal.util.TSafe._
  import internal.util.Util._

  val Keys = NonEmptyArray
  val Bins = NonEmptyArray
  val Hosts = NonEmptyList

  object NonEmptyArray {
    def apply[A: ClassTag](h: A, t: A*): Array[A] = {
      (genericWrapArray(Array(h)) ++ t).toArray
    }
  }

  object Bin {
    def apply[V: VRestriction](binName: String, value: V)(
      implicit ctx: distinct1.type) = {

      Tuple2(binName, value)
    }

    def apply[V](binName: String, value: Option[V])(
      implicit ctx: distinct2.type, ev2: VRestriction[V]) = {

      Tuple2(binName, parseOption(value).asInstanceOf[V])
    }
  }

  object Filter {
    def equal(bin: String, value: Long): AFilter = AFilter.equal(bin, value)
    def equal(bin: String, value: String): AFilter = AFilter.equal(bin, value)
    def range(bin: String, startNum: Long, endNum: Long): AFilter = AFilter.range(bin, startNum, endNum)
  }
}
