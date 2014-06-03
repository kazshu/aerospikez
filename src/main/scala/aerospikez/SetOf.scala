package aerospikez

import com.aerospike.client.query.{ Statement, ResultSet, IndexType, RecordSet, Filter ⇒ AFilter }
import com.aerospike.client.task.{ IndexTask, ExecuteTask, RegisterTask }
import com.aerospike.client.async.AsyncClient
import com.aerospike.client.policy.Policy
import com.aerospike.client.{ Value, Key }

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import scalaz.concurrent.Task
import scalaz.Free.Trampoline
import scalaz.Trampoline

import scalaz.stream.Process

import internal.util.TSafe._
import internal.util.Pimp._
import internal.SetOps

private[aerospikez] class SetOf[@specialized(Int, Long) SetV](
    namespace: Namespace,
    setName: String,
    asyncClient: AsyncClient,
    generalPolicy: Policy) {

  private val queryPolicy = namespace.queryConfig.policy
  private val writePolicy = namespace.writeConfig.policy
  private val setOp = new SetOps(asyncClient)

  object distinct1 { implicit val distinct: distinct1.type = this }
  object distinct2 { implicit val distinct: distinct2.type = this }
  object distinct3 { implicit val distinct: distinct3.type = this }

  private def createStmt(filter: AFilter): Statement = {

    val stmt: Statement = new Statement()
    stmt.setNamespace(namespace.name)
    stmt.setSetName(setName)
    stmt.setFilters(filter)

    stmt
  }

  private def parseKey[K](key: K): Key = {

    key match {
      case key: Int         ⇒ new Key(namespace.name, setName, key)
      case key: Long        ⇒ new Key(namespace.name, setName, key)
      case key: String      ⇒ new Key(namespace.name, setName, key)
      case key: Array[Byte] ⇒ new Key(namespace.name, setName, key)
    }
  }

  private def parseValue[V](value: V): Value = {

    value match {
      case value: Int         ⇒ new Value.IntegerValue(value)
      case value: String      ⇒ new Value.StringValue(value)
      case value: Array[Byte] ⇒ new Value.BytesValue(value)
      case value: Long        ⇒ new Value.LongValue(value)
      case value: List[_]     ⇒ new Value.ListValue(value)
      case value: Map[_, _]   ⇒ new Value.MapValue(value)
      case None | null        ⇒ new Value.NullValue()
      case v                  ⇒ new Value.BlobValue(v)
    }
  }

  private def parseOption[V](value: V) = {

    value match {
      case Some(v) ⇒ v.asInstanceOf[V]
      case None    ⇒ null.asInstanceOf[V]
      case _       ⇒ value.asInstanceOf[V]
    }
  }

  def put[K: KRestriction, V: VRestriction](key: K, value: V, bin: String = "")(
    implicit ev: V TypeOf SetV): Task[Unit] = {

    setOp.put[V](writePolicy, parseKey[K](key), parseOption[V](value), bin)
  }

  def putG[K: KRestriction, V: VRestriction](key: K, value: V, bin: String = "")(
    implicit ev: V TypeOf SetV): Task[Option[V]] = {

    setOp.putG[V](queryPolicy, writePolicy, parseKey[K](key), parseOption[V](value), bin)
  }

  def put[K: KRestriction, V](key: K, bin: Bin[V], bins: Bin[V]*)(
    implicit ev: V TypeOf SetV): Task[Unit] = {

    setOp.put[V](writePolicy, parseKey[K](key), Seq(bin) ++ bins)
  }

  def get[K: KRestriction, V](key: K)(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V]): Task[Option[V]] = {

    setOp.get[V](queryPolicy, parseKey[K](key), "")
  }

  def get[K: KRestriction, V](key: K, bin: ⇒ String)(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V]): Task[Option[V]] = {

    setOp.get[V](queryPolicy, parseKey[K](key), bin)
  }

  def get[K: KRestriction, V](key: K, bins: Array[String])(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V]): Task[OHMap[String, V]] = {

    setOp.get[V](queryPolicy, parseKey[K](key), bins)
  }

  def get[K: KRestriction, V](keys: Array[K])(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V], ctx: distinct1.type): Task[OHMap[K, V]] = {

    setOp.get[K, V](queryPolicy, keys.map(parseKey[K](_)), "")
  }

  def get[K: KRestriction, V](keys: Array[K], bin: ⇒ String)(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V], ctx: distinct2.type): Task[OHMap[K, V]] = {

    setOp.get[K, V](queryPolicy, keys.map(parseKey[K](_)), bin)
  }

  def get[K: KRestriction, V](keys: Array[K], bins: Array[String])(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V], ctx: distinct3.type): Task[OHMap[K, OHMap[String, V]]] = {

    setOp.get[K, V](queryPolicy, keys.map(parseKey[K](_)), bins)
  }

  def operate[K: KRestriction, V](key: K, operations: Ops*)(
    implicit ev1: V DefaultTypeTo SetV, ev2: VRestriction[V]): Task[Option[V]] = {

    setOp.operate[V](writePolicy, parseKey[K](key), operations: _*)
  }

  def delete[K: KRestriction](key: K): Task[Unit] = {

    setOp.delete(writePolicy, parseKey[K](key))
  }

  def touch[K: KRestriction](key: K): Task[Unit] = {

    setOp.touch(writePolicy, parseKey[K](key))
  }

  def exists[K: KRestriction](key: K): Task[Boolean] = {

    setOp.exists(queryPolicy, parseKey[K](key))
  }

  def exists[K: KRestriction](keys: Array[K])(
    implicit ctx: distinct1.type): Task[OHMap[K, Boolean]] = {

    setOp.exists[K](queryPolicy, keys.map(parseKey[K](_)))
  }

  def add[K: KRestriction](key: K, value: Int, bin: String = ""): Task[Unit] = {

    setOp.add(writePolicy, parseKey[K](key), value, bin)
  }

  def append[K: KRestriction](key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.append(writePolicy, parseKey[K](key), value, bin)
  }

  def prepend[K: KRestriction](key: K, value: String, bin: String = ""): Task[Unit] = {

    setOp.prepend(writePolicy, parseKey[K](key), value, bin)
  }

  def getHeader[K: KRestriction](key: K): Task[Option[Tuple2[Int, Int]]] = {

    setOp.getHeader(queryPolicy, parseKey[K](key))
  }

  def getHeader[K: KRestriction](keys: Array[K])(implicit ctx: distinct1.type): Task[OHMap[K, Tuple2[Int, Int]]] = {

    setOp.getHeader[K](queryPolicy, keys.map(parseKey[K](_)))
  }

  def execute[K: KRestriction, LuaR](key: K, packageName: String, functionName: String, functionArgs: Any*)(
    implicit ev1: LuaR DefaultTypeTo Any, ev2: LRestriction[LuaR]): Task[Option[LuaR]] = {

    setOp.execute[LuaR](generalPolicy, parseKey[K](key), packageName, functionName, functionArgs.map(parseValue(_)): _*)
  }

  def execute(filter: AFilter, packageName: String, functionName: String, functionArgs: Any*): Task[Unit] = {

    setOp.execute(generalPolicy, createStmt(filter), packageName, functionName, functionArgs.map(parseValue(_)): _*)
  }

  def query(filter: AFilter): Process[Task, OHMap[String, Any]] = {

    setOp.query(queryPolicy, createStmt(filter))
  }

  def queryAggregate[LuaR](filter: AFilter, packageName: String, functionName: String, functionArgs: Any*)(
    implicit ev1: LuaR DefaultTypeTo Any, ev2: LRestriction[LuaR]): Process[Task, LuaR] = {

    setOp.queryAggregate(queryPolicy, createStmt(filter), packageName, functionName, functionArgs.map(parseValue(_)): _*)
  }

  def createIndex[I](indexName: String, binName: String)(
    implicit ev1: I DefaultTypeTo Empty, ev2: I =!= Empty, evIndexType: IRestriction[I]): Task[Unit] = {

    setOp.createIndex(
      generalPolicy,
      namespace.name,
      setName,
      indexName,
      binName,
      evIndexType match {
        case _: IRestriction.string.type ⇒ IndexType.STRING
        case _: IRestriction.int.type    ⇒ IndexType.NUMERIC
      }
    )
  }

  def dropIndex(indexName: String): Task[Unit] = {

    setOp.dropIndex(generalPolicy, indexName, namespace.name, setName)
  }
}
