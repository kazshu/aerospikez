package aerospikez

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }

import com.aerospike.client.async.AsyncClient
import com.aerospike.client.query.Statement
import com.aerospike.client.policy.Policy
import com.aerospike.client.{ Value, Key }

import scala.collection.JavaConversions._
import scalaz.concurrent.Task

import internal.util.General._
import internal.util.TSafe._
import internal.util.Pimp._
import internal.SetOps
import internal.Ops

private[aerospikez] class SetOf[@specialized(Int, Long) V](
    namespace: Namespace,
    setName: String,
    async: AsyncClient,
    generalPolicy: Policy) {

  private val setOp = new SetOps(async)
  private val queryPolicy = namespace.queryConfig.policy
  private val writePolicy = namespace.writeConfig.policy

  object distinctKey1 { implicit val distinct: distinctKey1.type = this }
  object distinctKey2 { implicit val distinct: distinctKey2.type = this }
  object distinctKey3 { implicit val distinct: distinctKey3.type = this }

  private def getKey[K](key: K): Key = {

    key match {
      case key: String      ⇒ new Key(namespace.name, setName, key)
      case key: Int         ⇒ new Key(namespace.name, setName, key)
      case key: Long        ⇒ new Key(namespace.name, setName, key)
      case key: Array[Byte] ⇒ new Key(namespace.name, setName, key)
    }
  }

  private def getValue[V](value: V): Value = {

    value match {
      case value: Int         ⇒ new Value.IntegerValue(value)
      case value: String      ⇒ new Value.StringValue(value)
      case value: Array[Byte] ⇒ new Value.BytesValue(value)
      case value: Long        ⇒ new Value.LongValue(value)
      case value: List[_]     ⇒ new Value.ListValue(value)
      case value: Map[_, _]   ⇒ new Value.MapValue(value)
      case None               ⇒ new Value.NullValue()
    }
  }

  def execute[K: SupportKey, V, R](
    key: K,
    packageName: String,
    functionName: String,
    functionArgs: V*)(implicit ev: R DefaultTypeTo Any): Task[Option[R]] = {

    Task(
      trySome(
        async.execute(
          generalPolicy,
          getKey[K](key),
          packageName,
          functionName,
          functionArgs.map(getValue(_)): _*)
      ).asInstanceOf[Option[R]]
    )
  }

  def put[K: SupportKey, V2: SupportValue](
    key: K,
    value: V2,
    bin: String = "")(implicit ev: V2 SubTypeOf V): Task[Unit] = {

    setOp.put[V2](writePolicy, getKey[K](key), value, bin)
  }

  def putG[K: SupportKey, V2: SupportValue](
    key: K,
    value: V2,
    bin: String = "")(implicit ev: V2 SubTypeOf V): Task[Option[V2]] = {

    setOp.putG[V2](queryPolicy, writePolicy, getKey[K](key), value, bin)
  }

  def put[K: SupportKey, V2: SupportValue](
    key: K,
    bins: Bin[V2]*)(implicit ev: V2 SubTypeOf V): Task[Unit] = {

    setOp.put[V2](writePolicy, getKey[K](key), bins)
  }

  def get[K: SupportKey, V2](
    key: K)(implicit ev: V2 DefaultTypeTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), "")
  }

  def get[K: SupportKey, V2](
    key: K, bin: String)(implicit ev: V2 DefaultTypeTo V): Task[Option[V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bin)
  }

  def get[K: SupportKey, V2](
    keys: Array[K])(implicit k: distinctKey1.type, ev: V2 DefaultTypeTo V): Task[OHMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), "")
  }

  def get[K: SupportKey, V2](
    keys: Array[K],
    bin: String)(implicit k: distinctKey2.type, ev: V2 DefaultTypeTo V): Task[OHMap[K, V2]] = {

    setOp.get[K, V2](queryPolicy, keys.map(getKey[K](_)), bin)
  }

  def get[K: SupportKey, V2](
    key: K,
    bins: Array[String])(implicit ev: V2 DefaultTypeTo V): Task[OHMap[String, V2]] = {

    setOp.get[V2](queryPolicy, getKey[K](key), bins)
  }

  def get[K: SupportKey, V2](
    keys: Array[K],
    bins: Array[String])(implicit k: distinctKey3.type, ev: V2 DefaultTypeTo V): Task[OHMap[K, OHMap[String, V]]] = {

    setOp.get[K, V](queryPolicy, keys.map(getKey[K](_)), bins)
  }

  def delete[@specialized(Int, Long) K: SupportKey](key: K): Task[Unit] = {

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

  def getHeader[K: SupportKey](key: K): Task[String] = {

    setOp.getHeader(queryPolicy, getKey[K](key))
  }

  def operate[K: SupportKey, V2](key: K, operations: Ops*)(
    implicit ev: V2 DefaultTypeTo V): Task[Option[V2]] = {

    setOp.operate[V2](writePolicy, getKey[K](key), operations: _*)
  }
}
