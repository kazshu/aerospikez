package cl.otrimegistro.aerospikez

import com.aerospike.client.policy.Policy
import com.aerospike.client.Key

import scala.collection.mutable.OpenHashMap

import scala.reflect.ClassTag

import scalaz.concurrent.Task
import Util._

object SetOf {
  def apply[K: ClassTag, V](
    namespace: Namespace = Namespace(),
    name: String = "myset")(
      implicit client: Aerospikez = Aerospikez(),
      evK: K DefaultKeyTo String,
      evV: V DefaultValueTo Any): SetOf[K, V] = {

    new SetOf(namespace, name, client)
  }
}

private[aerospikez] class SetOf[K: ClassTag, V](namespace: Namespace, setName: String, client: Aerospikez) {

  private final val setOp = new SetOps(client.async)

  private final val queryPolicy = namespace.queryConfig.getPolicy()
  private final val writePolicy = namespace.writeConfig.getPolicy()

  private final val string = implicitly[ClassTag[String]]
  private final val ignoreK: K = implicitly[ClassTag[K]] match {
    case `string`      ⇒ "".asInstanceOf[K]
    case ClassTag.Int  ⇒ 0.asInstanceOf[K]
    case ClassTag.Long ⇒ 0L.asInstanceOf[K]
    case _             ⇒ Array.empty[Byte].asInstanceOf[K]
  }

  private def getKey[K](key: K): Key = key match {
    case key: String      ⇒ new Key(namespace.name, setName, key)
    case key: Int         ⇒ new Key(namespace.name, setName, key)
    case key: Long        ⇒ new Key(namespace.name, setName, key)
    case key: Array[Byte] ⇒ new Key(namespace.name, setName, key)
  }

  def put[V2](key: K, value: V2, bin: String = ""): Task[Unit] = {
    setOp.put[V2](writePolicy, getKey[K](key), value, bin)
  }

  def putG[V2](key: K, value: V2, bin: String = ""): Task[Option[V2]] = {
    setOp.putG[V2](queryPolicy, writePolicy, getKey[K](key), value, bin)
  }

  def get[V2](key: K)(implicit ev: V2 DefaultValueTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), "")
  }

  def get[V2](key: K, bin: String)(implicit ev: V2 DefaultValueTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bin)
  }

  def get[V2](keys: Array[K])(implicit ignore: K = ignoreK, ev: V2 DefaultValueTo V): Task[OpenHashMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), "")
  }

  def get[V2](keys: Array[K], bin: String)(implicit ignore: K = ignoreK, ev: V2 DefaultValueTo V): Task[OpenHashMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), bin)
  }

  def get[V2](key: K, bins: Array[String])(implicit ev: V2 DefaultValueTo V): Task[OpenHashMap[String, V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bins)
  }

  def get[V2](keys: Array[K], bins: Array[String])(implicit ignore: K = ignoreK, ev: V2 DefaultValueTo V): Task[OpenHashMap[K, OpenHashMap[String, V2]]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), bins)
  }

  def delete(key: K): Task[Unit] = {

    setOp.delete(writePolicy, getKey[K](key))
  }

  def append(key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.append(writePolicy, getKey[K](key), value, bin)
  }

  def prepend(key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.prepend(writePolicy, getKey[K](key), value, bin)
  }

  def add(key: K, value: Int, bin: String = ""): Task[Unit] = {

    setOp.add(writePolicy, getKey[K](key), value, bin)
  }

  def exists(key: K): Task[Boolean] = {

    setOp.exists(queryPolicy, getKey[K](key))
  }

  def exists(keys: Array[K])(implicit ignore: K = ignoreK): Task[OpenHashMap[K, Boolean]] = {

    setOp.exists[K](queryPolicy, keys.map(getKey[K](_)))
  }
}
