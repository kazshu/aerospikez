package cl.otrimegistro.aerospikez

import com.aerospike.client.policy.Policy
import com.aerospike.client.Key

import scala.collection.mutable.OpenHashMap
import scala.reflect.ClassTag

import scalaz.concurrent.Task

object SetOf {
  def apply(namespace: Namespace = Namespace(),
            name: String = "myset")(implicit client: Aerospikez = Aerospikez()): SetOf = {
    new SetOf(namespace, name, client)
  }
}

private[aerospikez] class SetOf(namespace: Namespace, setName: String, client: Aerospikez) {

  private final val setOp = new SetOps(client.async)

  private final val queryPolicy = namespace.queryConfig.getPolicy()
  private final val writePolicy = namespace.writeConfig.getPolicy()

  private def getKey[K](key: K): Key = key match {
    case key: String      ⇒ new Key(namespace.name, setName, key)
    case key: Int         ⇒ new Key(namespace.name, setName, key)
    case key: Long        ⇒ new Key(namespace.name, setName, key)
    case key: Array[Byte] ⇒ new Key(namespace.name, setName, key)
  }

  def put[K, V](key: K, value: V, bin: String = ""): Task[Unit] = {
    setOp.put[V](writePolicy, getKey[K](key), value, bin)
  }

  def get[K, V](key: K): Task[Option[V]] = {
    setOp.get[V](queryPolicy, getKey[K](key), "")
  }

  def get[K: ClassTag, V](keys: Array[K]): Task[OpenHashMap[K, V]] = {
    setOp.get[K, V](queryPolicy, keys.map(getKey[K](_)), "")
  }

  def get[K: ClassTag, V](keys: Array[K], bin: String): Task[OpenHashMap[K, V]] = {
    setOp.get[K, V](queryPolicy, keys.map(getKey[K](_)), bin)
  }

  def get[K, V](key: K, bin: String): Task[Option[V]] = {
    setOp.get[V](queryPolicy, getKey[K](key), bin)
  }

  def get[K, V](key: K, bins: Array[String]): Task[OpenHashMap[String, V]] = {
    setOp.get[V](queryPolicy, getKey[K](key), bins)
  }

  def get[K: ClassTag, V](keys: Array[K], bins: Array[String]): Task[OpenHashMap[K, OpenHashMap[String, V]]] = {
    setOp.get[K, V](queryPolicy, keys.map(getKey[K](_)), bins)
  }

  def delete[K](key: K): Task[Unit] = {
    setOp.delete(writePolicy, getKey[K](key))
  }

  def append[K](key: K, value: String, bin: String = ""): Task[Unit] = {
    setOp.append(writePolicy, getKey[K](key), value, bin)
  }

  def prepend[K](key: K, value: String, bin: String = ""): Task[Unit] = {
    setOp.prepend(writePolicy, getKey[K](key), value, bin)
  }
}
