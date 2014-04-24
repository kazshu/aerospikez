package cl.otrimegistro.aerospikez

import com.aerospike.client.listener.{ RecordArrayListener, ExistsArrayListener, DeleteListener, WriteListener, RecordListener, ExistsListener }
import com.aerospike.client.{ AerospikeException, Record, Host, Key, Bin }
import com.aerospike.client.policy.{ QueryPolicy, WritePolicy, Policy }
import com.aerospike.client.async.AsyncClient

import scalaz.{ -\/, \/- }
import scalaz.syntax.std.option._
import scalaz.concurrent.Task
import scalaz.Free.Trampoline
import scalaz.Trampoline
import scalaz.syntax.id._

import scala.collection.mutable.OpenHashMap

import Util.{ pimpAny, pimpJavaMap }

private[aerospikez] class SetOps[K](client: AsyncClient) {

  private[aerospikez] def put[V](policy: WritePolicy, key: Key, value: V, bin: String): Task[Unit] = {
    Task.async { register ⇒
      client.put(policy,
        new WriteListener {
          def onSuccess(key: Key): Unit = register(\/-(()))
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, key, new Bin(bin, value))
    }
  }

  private[aerospikez] def get[K, V](
    policy: QueryPolicy,
    keys: Array[Key],
    bins: Array[String]): Task[OpenHashMap[K, OpenHashMap[String, V]]] = {

    Task.async { register ⇒
      client.get(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            val length = keys.length

            def getMap(m: OpenHashMap[K, OpenHashMap[String, V]] = OpenHashMap.empty[K, OpenHashMap[String, V]],
                       i: Int = 0): Trampoline[OpenHashMap[K, OpenHashMap[String, V]]] = i match {
              case `length` ⇒ Trampoline.done(m)
              case _ ⇒ Trampoline.suspend(getMap({
                if (keys(i) != null && (records(i) != null && records(i).bins != null)) {
                  m.put(keys(i).userKey.asInstanceOf[K], records(i).bins.toOpenHashMap().run.asInstanceOf[OpenHashMap[String, V]])
                }; m
              }, i + 1))
            }

            register(\/-(getMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, keys, bins: _*)
    }
  }

  private[aerospikez] def get[K, V](policy: QueryPolicy, keys: Array[Key], bin: String): Task[OpenHashMap[K, V]] = {
    Task.async { register ⇒
      client.get(policy,
        new RecordArrayListener {
          def onSuccess(keys: Array[Key], records: Array[Record]): Unit = {
            val length = keys.length

            def getMap(m: OpenHashMap[K, V] = OpenHashMap.empty[K, V], i: Int = 0): Trampoline[OpenHashMap[K, V]] = i match {
              case `length` ⇒ Trampoline.done(m)
              case _ ⇒ Trampoline.suspend(getMap({
                if (records(i) != null && records(i).bins != null) {
                  records(i).bins.toOpenHashMap().run.get(bin).map { userValue ⇒
                    m.put(keys(i).userKey.asInstanceOf[K], userValue.asInstanceOf[V])
                  }
                }; m
              }, i + 1))
            }

            register(\/-(getMap().run))
          }
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, keys, Seq(bin): _*)
    }
  }

  private[aerospikez] def get[V](policy: QueryPolicy, key: Key, bins: Array[String]): Task[OpenHashMap[String, V]] = {
    Task.async { register ⇒
      client.get(policy,
        new RecordListener {
          def onSuccess(key: Key, record: Record): Unit = register(\/-(
            if (record != null && record.bins != null)
              record.bins.toOpenHashMap().run.asInstanceOf[OpenHashMap[String, V]]
            else
              OpenHashMap.empty[String, V]
          ))
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, key, bins: _*)
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
          def onFailure(ae: AerospikeException): Unit = register(-\/(ae))
        }, key)
    }
  }
}
