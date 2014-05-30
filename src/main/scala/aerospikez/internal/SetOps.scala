package aerospikez.internal

import com.aerospike.client.listener.{ RecordArrayListener, ExistsArrayListener, DeleteListener }
import com.aerospike.client.listener.{ WriteListener, RecordListener, ExistsListener }
import com.aerospike.client.{ AerospikeException, Record, Host, Key, Bin ⇒ ABin, Value }
import com.aerospike.client.policy.{ QueryPolicy, WritePolicy, Policy }
import com.aerospike.client.query.{ Statement, IndexType }
import com.aerospike.client.async.AsyncClient

import com.aerospike.client.task.{ IndexTask, ExecuteTask }

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }
import scala.collection.JavaConverters._

import scalaz.{ -\/, \/- }
import scalaz.concurrent.Task
import scalaz.Free.Trampoline
import scalaz.Trampoline

import scalaz.stream.Process
import scalaz.stream.io

import aerospikez.Ops
import aerospikez.Operations._
import aerospikez.internal.util.Pimp._

private[aerospikez] class SetOps[K](client: AsyncClient) {

  private[aerospikez] def put[V](policy: WritePolicy, key: Key, value: V, bin: String): Task[Unit] = {

    Task.async { register ⇒
      client.put(policy,
        new WriteListener {
          def onSuccess(key: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, new ABin(bin, value))
    }
  }

  private[aerospikez] def put[V](policy: WritePolicy, key: Key, bins: Seq[aerospikez.Bin[V]]): Task[Unit] = {

    Task.async { register ⇒
      client.put(policy,
        new WriteListener {
          def onSuccess(key: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, bins.map(t ⇒ new ABin(t._1, t._2)): _*)
    }
  }

  private[aerospikez] def putG[V](policyQ: QueryPolicy, policyW: WritePolicy, key: Key, value: V, bin: String): Task[Option[V]] = {

    val oldValue = get(policyQ, key, bin).run
    Task.async { register ⇒
      client.put(policyW,
        new WriteListener {
          def onSuccess(key: Key): Unit = {
            register(\/-(oldValue.asInstanceOf[Option[V]]))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, new ABin(bin, value))
    }
  }

  private[aerospikez] def get[V](policy: QueryPolicy, key: Key, bin: String): Task[Option[V]] = {

    Task.async { register ⇒
      client.get(policy,
        new RecordListener {
          def onSuccess(key: Key, record: Record): Unit = {
            lazy val value = record.getValue(bin)
            register(\/-(
              if (record != null && value != null)
                Some(value.asInstanceOf[V])
              else
                None
            ))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def get[V](policy: QueryPolicy, key: Key, bins: Array[String]): Task[OHMap[String, V]] = {

    Task.async { register ⇒
      client.get(policy,
        new RecordListener {
          def onSuccess(key: Key, record: Record): Unit = {
            register(\/-(
              if (record != null && record.bins != null)
                record.bins.toOpenHashMap().run.asInstanceOf[OHMap[String, V]]
              else
                OHMap.empty[String, V]
            ))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, bins: _*)
    }
  }

  private[aerospikez] def get[K, V](
    policy: QueryPolicy,
    keys: Array[Key], bin: String): Task[OHMap[K, V]] = {

    Task.async { register ⇒
      client.get(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            lazy val length = keys.length

            def getMap(m: OHMap[K, V] = OHMap.empty[K, V],
                       i: Int = 0): Trampoline[OHMap[K, V]] = {
              lazy val rec = records(i)

              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getMap({
                  if (rec != null && rec.bins != null) {
                    rec.bins.toOpenHashMap().run.get(bin).map { userValue ⇒
                      m.put(keys(i).userKey.asInstanceOf[K], userValue.asInstanceOf[V])
                    }
                  }; m
                }, i + 1))
              }
            }

            register(\/-(getMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, keys, Seq(bin): _*)
    }
  }

  private[aerospikez] def get[K, V](
    policy: QueryPolicy,
    keys: Array[Key],
    bins: Array[String]): Task[OHMap[K, OHMap[String, V]]] = {

    Task.async { register ⇒
      client.get(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            val length = keys.length

            def getMap(m: OHMap[K, OHMap[String, V]] = OHMap.empty[K, OHMap[String, V]],
                       i: Int = 0): Trampoline[OHMap[K, OHMap[String, V]]] = {
              lazy val rec = records(i)

              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getMap({
                  if (keys(i) != null && (rec != null && rec.bins != null)) {
                    m.put(
                      keys(i).userKey.asInstanceOf[K],
                      rec.bins.toOpenHashMap().run.asInstanceOf[OHMap[String, V]]
                    )
                  }; m
                }, i + 1))
              }
            }

            register(\/-(getMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, keys, bins: _*)
    }
  }

  private[aerospikez] def delete(policy: WritePolicy, key: Key): Task[Unit] = {

    Task.async { register ⇒
      client.delete(policy,
        new DeleteListener {
          def onSuccess(key: Key, existed: Boolean): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def touch(policy: WritePolicy, key: Key): Task[Unit] = {
    Task.async { register ⇒
      client.touch(policy,
        new WriteListener {
          def onSuccess(key: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def append(policy: WritePolicy, key: Key, value: String, bin: String): Task[Unit] = {

    Task.async { register ⇒
      client.append(policy,
        new WriteListener {
          def onSuccess(k: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, new ABin(bin, value))
    }
  }

  private[aerospikez] def prepend(policy: WritePolicy, key: Key, value: String, bin: String): Task[Unit] = {

    Task.async { register ⇒
      client.prepend(policy,
        new WriteListener {
          def onSuccess(k: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, new ABin(bin, value))
    }
  }

  private[aerospikez] def exists(policy: QueryPolicy, key: Key): Task[Boolean] = {

    Task.async { register ⇒
      client.exists(policy,
        new ExistsListener {
          def onSuccess(key: Key, exists: Boolean): Unit = {
            register(\/-(exists))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def exists[K](policy: QueryPolicy, keys: Array[Key]): Task[OHMap[K, Boolean]] = {

    Task.async { register ⇒
      client.exists(policy,
        new ExistsArrayListener {
          def onSuccess(keys: Array[Key], existsArray: Array[Boolean]): Unit = {
            lazy val length = keys.length

            def getOHMap(m: OHMap[K, Boolean] = OHMap.empty[K, Boolean],
                         i: Int = 0): Trampoline[OHMap[K, Boolean]] = {
              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getOHMap({
                  m.put(keys(i).userKey.asInstanceOf[K], existsArray(i));
                  m
                },
                  i + 1))
              }
            }

            register(\/-(getOHMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, keys)
    }
  }

  private[aerospikez] def add(policy: WritePolicy, key: Key, value: Int, bin: String = ""): Task[Unit] = {

    Task.async { register ⇒
      client.add(policy,
        new WriteListener {
          def onSuccess(k: Key): Unit = {
            register(\/-(()))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key, new ABin(bin, value))
    }
  }

  private[aerospikez] def getHeader(policy: QueryPolicy, key: Key): Task[Option[Tuple2[Long, Long]]] = {

    Task.async { register ⇒
      client.getHeader(policy,
        new RecordListener {
          def onSuccess(key: Key, record: Record): Unit = {
            register(\/-(
              if (record != null)
                Some((record.generation.toLong, record.expiration.toLong))
              else
                None
            ))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def getHeader[K](policy: QueryPolicy, keys: Array[Key]): Task[OHMap[K, Option[Tuple2[Long, Long]]]] = {

    Task.async { register ⇒
      client.getHeader(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            lazy val length = keys.length

            def getOHMap(m: OHMap[K, Option[Tuple2[Long, Long]]] = OHMap.empty[K, Option[Tuple2[Long, Long]]],
                         i: Int = 0): Trampoline[OHMap[K, Option[(Long, Long)]]] = {
              lazy val rec = records(i)

              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getOHMap({
                  m.put(keys(i).userKey.asInstanceOf[K],
                    if (rec != null)
                      Some((rec.generation.toLong, rec.expiration.toLong))
                    else
                      None);
                  m
                },
                  i + 1))
              }
            }

            register(\/-(getOHMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, keys)
    }
  }

  private[aerospikez] def operate[V](policy: WritePolicy, key: Key, operations: Ops*): Task[Option[V]] = {

    operations.last match {
      case _: Touch | _: Append | _: Put[V] | _: Prepend | _: Add ⇒
        Task.async { register ⇒
          client.operate(policy,
            new RecordListener {
              def onSuccess(key: Key, record: Record): Unit = {
                register(\/-(Some(().asInstanceOf[V])))
              }
              def onFailure(ae: AerospikeException): Unit = {
                register(-\/(ae))
              }
            }, key, operations.map(_.toOperation): _*)
        }
      case _: GetHeader ⇒
        Task.async { register ⇒
          client.operate(policy,
            new RecordListener {
              def onSuccess(key: Key, record: Record): Unit = {
                register(\/-(
                  if (record != null)
                    Some((record.generation.toLong, record.expiration.toLong).asInstanceOf[V])
                  else
                    None
                ))
              }
              def onFailure(ae: AerospikeException): Unit = {
                register(-\/(ae))
              }
            }, key, operations.map(_.toOperation): _*)
        }
      case last: Get ⇒
        Task.async { register ⇒
          client.operate(policy,
            new RecordListener {
              def onSuccess(key: Key, record: Record): Unit = {
                register(\/-({
                  lazy val value = record.getValue(last.binName)
                  if (record != null && value != null)
                    Some(value.asInstanceOf[V])
                  else
                    None
                }))
              }
              def onFailure(ae: AerospikeException): Unit = {
                register(-\/(ae))
              }
            }, key, operations.map(_.toOperation): _*)
        }
    }
  }

  private[aerospikez] def execute[LuaR](policy: Policy, key: Key, packageName: String, functionName: String, functionArgs: Value*): Task[Option[LuaR]] = {

    Task.delay {
      client.execute(
        policy,
        key,
        packageName,
        functionName,
        functionArgs: _*
      ).toOption.map(result ⇒
          result match {
            case r: java.util.ArrayList[_] ⇒
              r.asScala.toList
            case r: java.util.HashMap[_, _] ⇒
              r.asScala.toMap
            case _ ⇒ result
          }
        ).asInstanceOf[Option[LuaR]]
    }
  }

  private[aerospikez] def execute(policy: Policy, stmt: Statement, packageName: String, functionName: String, functionArgs: Value*): Task[Unit] = {

    Task.delay {
      val t: ExecuteTask = client.execute(
        policy,
        stmt,
        packageName,
        functionName,
        functionArgs: _*
      )

      t.waitTillComplete(200)
    }
  }

  private[aerospikez] def query(policy: QueryPolicy, stmt: Statement): Process[Task, OHMap[String, Any]] = {

    io.resource(Task.delay(
      client.query(policy, stmt)
    ))(rs ⇒ Task.delay(rs.close)) { rs ⇒
      Task.delay {
        if (rs.next)
          rs.getRecord.bins.toOpenHashMap().run.asInstanceOf[OHMap[String, Any]]
        else
          throw Process.End
      }
    }
  }

  private[aerospikez] def queryAggregate[LuaR](policy: QueryPolicy, stmt: Statement, packageName: String, functionName: String, functionArgs: Value*): Process[Task, LuaR] = {

    io.resource(Task.delay(
      client.queryAggregate(
        policy,
        stmt,
        packageName,
        functionName,
        functionArgs: _*
      )
    ))(rs ⇒ Task.delay(rs.close)) { rs ⇒
      Task.delay {
        if (rs.next)
          (rs.getObject match {
            case r: java.util.ArrayList[_] ⇒
              r.asScala.toList
            case r: java.util.HashMap[_, _] ⇒
              r.asScala.toMap
            case other ⇒ other
          }).asInstanceOf[LuaR]
        else
          throw Process.End
      }
    }
  }

  private[aerospikez] def createIndex(policy: Policy, nsName: String, setName: String, indexName: String, binName: String, indexType: IndexType): Task[Unit] = {

    Task.delay {
      val t: IndexTask = client.createIndex(
        policy,
        nsName,
        setName,
        indexName,
        binName,
        indexType
      )

      t.waitTillComplete(200)
    }
  }

  private[aerospikez] def dropIndex(policy: Policy, indexName: String, nsName: String, setName: String): Task[Unit] = {

    Task.delay {
      client.dropIndex(
        policy,
        nsName,
        setName,
        indexName
      )
    }
  }
}
