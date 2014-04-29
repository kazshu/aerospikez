package cl.otrimegistro.aerospikez

import com.aerospike.client.listener.{ RecordArrayListener, ExistsArrayListener, DeleteListener }
import com.aerospike.client.listener.{ WriteListener, RecordListener, ExistsListener }
import com.aerospike.client.{ AerospikeException, Record, Host, Key, Bin }
import com.aerospike.client.policy.{ QueryPolicy, WritePolicy, Policy }
import com.aerospike.client.async.AsyncClient

import scala.collection.mutable.{ OpenHashMap ⇒ OHMap }

import scalaz.{ -\/, \/- }
import scalaz.syntax.std.option._
import scalaz.concurrent.Task
import scalaz.Free.Trampoline
import scalaz.Trampoline
import scalaz.syntax.id._

import Util.{ pimpAny, pimpJavaMap }

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
        }, key, new Bin(bin, value))
    }
  }

  private[aerospikez] def putG[V](policyQ: QueryPolicy, policyW: WritePolicy, key: Key, value: V, bin: String): Task[Option[V]] = {
    val oldValue = get(policyQ, key, bin).run
    Task.async { register ⇒
      client.put(policyW,
        new WriteListener {
          def onSuccess(key: Key): Unit = register(\/-(oldValue.asInstanceOf[Option[V]]))
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, key, new Bin(bin, value))
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
            val length = keys.length

            def getMap(m: OHMap[K, V] = OHMap.empty[K, V],
                       i: Int = 0): Trampoline[OHMap[K, V]] = {
              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getMap({
                  if (records(i) != null && records(i).bins != null) {
                    records(i).bins.toOpenHashMap().run.get(bin).map { userValue ⇒
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
              i match {
                case `length` ⇒ Trampoline.done(m)
                case _ ⇒ Trampoline.suspend(getMap({
                  if (keys(i) != null && (records(i) != null && records(i).bins != null)) {
                    m.put(
                      keys(i).userKey.asInstanceOf[K],
                      records(i).bins.toOpenHashMap().run.asInstanceOf[OHMap[String, V]]
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
        }, key, new Bin(bin, value))
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
        }, key, new Bin(bin, value))
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
            val length = keys.length

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
        }, key, new Bin(bin, value))
    }
  }
}
