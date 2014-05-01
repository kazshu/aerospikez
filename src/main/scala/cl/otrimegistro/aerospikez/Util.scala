package cl.otrimegistro.aerospikez

import scala.collection.mutable.OpenHashMap

import scalaz.Free.Trampoline
import scalaz.Trampoline

import java.{ util ⇒ ju }

private[aerospikez] object Util {

  def trySome[A](a: ⇒ A): Option[A] = {
    try {
      Some(a)
    } catch {
      case _: Throwable ⇒ None
    }
  }

  class PimpAny[A](val self: A) {

    def toOption: Option[A] =
      if (self != null) Some(self) else None
  }

  implicit def pimpAny[A](a: A) = new PimpAny(a)

  class PimpJavaMap[K, V](coll: java.util.Map[K, V]) {

    private val iterator: ju.Iterator[ju.Map.Entry[K, V]] = coll.entrySet().iterator()

    def toOpenHashMap(m: OpenHashMap[K, V] = OpenHashMap.empty[K, V]): Trampoline[OpenHashMap[K, V]] = {
      lazy val entry: ju.Map.Entry[K, V] = iterator.next()

      if (iterator.hasNext()) {
        Trampoline.suspend(toOpenHashMap(
          { val value = entry.getValue; if (value != null) m.put(entry.getKey, value); m }
        ))
      } else {
        Trampoline.done(m)
      }

    }
  }

  implicit def pimpJavaMap[K, V](coll: java.util.Map[K, V]): PimpJavaMap[K, V] = new PimpJavaMap[K, V](coll)

  @annotation.implicitNotFound(msg = "Aaerospike support only String, Int, Long and Array[Byte] as Type for Key, and you provide a ${K}!")
  trait SupportKey[K]
  object SupportKey {
    implicit object string extends SupportKey[String]
    implicit object int extends SupportKey[Int]
    implicit object long extends SupportKey[Long]
    implicit object arraybyte extends SupportKey[Array[Byte]]
  }

  sealed class DefaultValueTo[T1, T2]
  trait ValueSupportTypes {
    implicit def supportAny[T1, T2] = new DefaultValueTo[T1, T2]
  }
  object DefaultValueTo extends ValueSupportTypes {
    implicit def default[T2] = new DefaultValueTo[T2, T2]
  }
}
