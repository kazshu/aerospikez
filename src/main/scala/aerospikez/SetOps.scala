package aerospikez

import com.aerospike.client.listener.{ RecordArrayListener, ExistsArrayListener, DeleteListener }
import com.aerospike.client.listener.{ WriteListener, RecordListener, ExistsListener }
import com.aerospike.client.{ AerospikeException, Record, Host, Key }
import com.aerospike.client.policy.{ QueryPolicy, WritePolicy, Policy }
import com.aerospike.client.async.AsyncClient
import com.aerospike.client.{ Bin ⇒ ABin }

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }

import scalaz.{ -\/, \/- }
import scalaz.concurrent.Task
import scalaz.Free.Trampoline
import scalaz.Trampoline

import Util.{ pimpAny, pimpJavaMap }
import Operations._

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

  private[aerospikez] def put[V](policy: WritePolicy, key: Key, bins: Seq[Bin[V]]): Task[Unit] = {
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
            register(\/-(Trampoline.done({
              lazy val value = record.getValue(bin)
              if (record != null && value != null)
                Some(value.asInstanceOf[V])
              else
                None
            }).run))
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

  private[aerospikez] def getHeader(policy: QueryPolicy, key: Key): Task[String] = {
    Task.async { register ⇒
      client.getHeader(policy,
        new RecordListener {
          def onSuccess(key: Key, record: Record): Unit = {
            register(\/-(Trampoline.done(
              if (record != null)
                s"(gen: ${record.generation}, exp: ${record.expiration})"
              else
                s"(record no found)"
            ).run))
          }
          def onFailure(ae: AerospikeException): Unit = {
            register(-\/(ae))
          }
        }, key)
    }
  }

  private[aerospikez] def getHeader(policy: QueryPolicy, keys: Array[Key]): Task[OHMap[Key, String]] = {
    Task.async { register ⇒
      client.getHeader(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            lazy val length = keys.length

            def getOHMap(m: OHMap[Key, String] = OHMap.empty[Key, String],
                         i: Int = 0): Trampoline[OHMap[Key, String]] = {
              lazy val rec = records(i)

              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getOHMap({
                  m.put(keys(i),
                    if (rec != null)
                      s"(gen: ${rec.generation}, exp: ${rec.expiration})"
                    else
                      s"(record no found)");
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
                register(\/-(Some({
                  if (record != null)
                    s"gen: ${record.generation}, exp: ${record.expiration}"
                  else
                    s"record no found"
                }.asInstanceOf[V])))
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

}
