package cl.otrimegistro.aerospikez

import com.aerospike.client.async.AsyncClient
import com.aerospike.client.policy.Policy
import com.aerospike.client.Key

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }

import scalaz.concurrent.Task
import Util._

private[aerospikez] class SetOf[V](namespace: Namespace, setName: String, async: AsyncClient) {

  private final val setOp = new SetOps(async)

  private final val queryPolicy = namespace.queryConfig.getPolicy()
  private final val writePolicy = namespace.writeConfig.getPolicy()

  private def getKey[K](key: K): Key = key match {
    case key: String      ⇒ new Key(namespace.name, setName, key)
    case key: Int         ⇒ new Key(namespace.name, setName, key)
    case key: Long        ⇒ new Key(namespace.name, setName, key)
    case key: Array[Byte] ⇒ new Key(namespace.name, setName, key)
  }

  object distinctKey1 { implicit val distinct: distinctKey1.type = this }
  object distinctKey2 { implicit val distinct: distinctKey2.type = this }
  object distinctKey3 { implicit val distinct: distinctKey3.type = this }

  def put[K: SupportKey, V2](key: K, value: V2, bin: String = ""): Task[Unit] = {

    setOp.put[V2](writePolicy, getKey[K](key), value, bin)
  }

  def putG[K: SupportKey, V2](key: K, value: V2, bin: String = ""): Task[Option[V2]] = {

    setOp.putG[V2](queryPolicy, writePolicy, getKey[K](key), value, bin)
  }

  def get[K: SupportKey, V2](key: K)(
    implicit ev: V2 DefaultValueTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), "")
  }

  def get[K: SupportKey, V2](key: K, bin: String)(
    implicit ev: V2 DefaultValueTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bin)
  }

  def get[K: SupportKey, V2](keys: Array[K])(
    implicit k: distinctKey1.type, ev: V2 DefaultValueTo V): Task[OHMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), "")
  }

  def get[K: SupportKey, V2](keys: Array[K], bin: String)(
    implicit k: distinctKey2.type, ev: V2 DefaultValueTo V): Task[OHMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), bin)
  }

  def get[K: SupportKey, V2](key: K, bins: Array[String])(
    implicit ev: V2 DefaultValueTo V): Task[OHMap[String, V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bins)
  }

  def get[K: SupportKey, V2](keys: Array[K], bins: Array[String])(
    implicit k: distinctKey3.type, ev: V2 DefaultValueTo V): Task[OHMap[K, OHMap[String, V]]] = {

    setOp.get[K, V](queryPolicy, keys.map(getKey[K](_)), bins)
  }

  def delete[K: SupportKey](key: K): Task[Unit] = {

    setOp.delete(writePolicy, getKey[K](key))
  }

  def append[K: SupportKey](key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.append(writePolicy, getKey[K](key), value, bin)
  }

  def prepend[K: SupportKey](key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.prepend(writePolicy, getKey[K](key), value, bin)
  }

  def add[K: SupportKey](key: K, value: Int, bin: String = ""): Task[Unit] = {

    setOp.add(writePolicy, getKey[K](key), value, bin)
  }

  def exists[K: SupportKey](key: K): Task[Boolean] = {

    setOp.exists(queryPolicy, getKey[K](key))
  }

  def exists[K: SupportKey](keys: Array[K])(
    implicit k: distinctKey1.type): Task[OHMap[K, Boolean]] = {

    setOp.exists[K](queryPolicy, keys.map(getKey[K](_)))
  }
}
