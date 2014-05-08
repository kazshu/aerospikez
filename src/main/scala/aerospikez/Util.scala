package aerospikez

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

  implicit def pimpAny[A](a: A): PimpAny[A] = new PimpAny(a)

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

  @annotation.implicitNotFound(msg = "That Set has been forced to accept only ${V} as Value, but you provide a ${V2}!")
  sealed class SubTypeOf[V2, V]
  object SubTypeOf {
    implicit def f[V2, V](implicit ev: V2 <:< V): SubTypeOf[V2, V] = new SubTypeOf[V2, V]
  }

  @annotation.implicitNotFound(msg = "Aaerospike support only String, Int, Long and Array[Byte] as Key, but you provide a ${K}!")
  trait SupportKey[@specialized(Int, Long) K]
  object SupportKey {
    implicit object string extends SupportKey[String]
    implicit object int extends SupportKey[Int]
    implicit object long extends SupportKey[Long]
    implicit object arraybyte extends SupportKey[Array[Byte]]
  }

  @annotation.implicitNotFound(msg = "Aaerospike support only String, Int, Long, Map and List as Value, but you provide a ${V}!")
  trait SupportValue[@specialized(Int, Long) V]
  object SupportValue {
    implicit object string extends SupportValue[String]
    implicit object int extends SupportValue[Int]
    implicit object long extends SupportValue[Long]
    implicit object list extends SupportValue[List[_]]
    implicit object map extends SupportValue[Map[_, _]]
  }

  sealed class DefaultValueTo[V2, V]
  trait DefaultToAny {
    implicit def defaultAny[V2, V]: DefaultValueTo[V2, V] = new DefaultValueTo[V2, V]
  }
  object DefaultValueTo extends DefaultToAny {
    implicit def defaultType[V]: DefaultValueTo[V, V] = new DefaultValueTo[V, V]
  }
}
