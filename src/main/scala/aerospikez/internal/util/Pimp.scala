package aerospikez.internal.util

import scala.collection.mutable.{ Map ⇒ _ }
import java.{ util ⇒ ju }

private[aerospikez] object Pimp {

  implicit class PimpAny[A](val self: A) {

    def toOption: Option[A] =
      if (self != null) Some(self) else None
  }

  implicit class PimpJavaMap[K, V](coll: ju.Map[K, V]) {

    private final val iterator: ju.Iterator[ju.Map.Entry[K, V]] = coll.entrySet().iterator()

    def toMapWithNotNull: Map[K, V] = {
      val b = Map.newBuilder[K, V]

      while (iterator.hasNext) {
        val entry: ju.Map.Entry[K, V] = iterator.next()
        val value = entry.getValue
        if (value != null) b += ((entry.getKey, value))
      }
      b.result
    }
  }
}
