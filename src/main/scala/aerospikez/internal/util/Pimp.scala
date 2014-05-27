package aerospikez.internal.util

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }
import scala.concurrent.duration._

import scalaz.Free.Trampoline
import scalaz.concurrent.{ Future, Task }
import scalaz.{ -\/, \/, \/- }
import scalaz.Trampoline

import java.{ util ⇒ ju }

private[aerospikez] object Pimp {

  implicit class PimpAny[A](val self: A) {

    def toOption: Option[A] =
      if (self != null) Some(self) else None
  }

  implicit class PimpJavaMap[K, V](coll: java.util.Map[K, V]) {

    private val iterator: ju.Iterator[ju.Map.Entry[K, V]] = coll.entrySet().iterator()

    def toOpenHashMap(m: OHMap[K, V] = OHMap.empty[K, V]): Trampoline[OHMap[K, V]] = {
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
}
