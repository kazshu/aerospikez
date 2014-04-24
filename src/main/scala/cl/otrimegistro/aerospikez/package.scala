package cl.otrimegistro

import scala.reflect.ClassTag
import scalaz.NonEmptyList

package object aerospikez {

  object NonEmptyArray {
    def apply[A: ClassTag](h: A, t: A*): Array[A] = (genericWrapArray(Array(h)) ++ t).toArray
  }

  val Keys = NonEmptyArray
  val Bins = NonEmptyArray
  val Hosts = NonEmptyList
}
