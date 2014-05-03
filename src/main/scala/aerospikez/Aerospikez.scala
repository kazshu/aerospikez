package aerospikez

import com.typesafe.config.{ ConfigFactory, Config }

import com.aerospike.client.async.AsyncClient
import com.aerospike.client.Host

import scalaz.NonEmptyList
import Util._

object Aerospikez {

  def apply(hosts: NonEmptyList[String] = NonEmptyList("localhost:3000"),
            clientConfig: ⇒ ClientConfig = ClientConfig(),
            configFile: Config = ConfigFactory.load()): Aerospikez = {

    ConfigFile.file = configFile

    new Aerospikez(hosts, clientConfig, configFile)
  }
}

private[aerospikez] class Aerospikez(hosts: NonEmptyList[String], clientConfig: ClientConfig, configFile: Config) {
  self ⇒

  def setOf[V](namespace: Namespace = Namespace(), name: String = "myset")(
    implicit ev: V DefaultValueTo Any): SetOf[V] = {

    new SetOf[V](namespace, name, self.async)
  }

  private[aerospikez] final val async = new AsyncClient(
    clientConfig.getPolicy(),
    getHosts(configFile, hosts).map { createHost(_, getPort(configFile)) }.list: _*
  )

  def close = async.close()

  private[aerospikez] def getHosts(configFile: Config, hosts: NonEmptyList[String]): NonEmptyList[String] = {
    import scala.collection.JavaConversions.asScalaBuffer

    trySome(configFile.getStringList("aerospike.hosts").toList match {
      case h :: t ⇒ NonEmptyList.nel(h, t)
      case Nil    ⇒ hosts
    }).getOrElse(hosts)
  }

  private[aerospikez] def getPort(configFile: Config): Int = {
    trySome(configFile.getInt("aerospike.port")).getOrElse(3000)
  }

  private[aerospikez] def createHost(host: String, defaultPort: Int): Host = {
    host.split(':') match {
      case Array(address, port) ⇒ new Host(address, port.toInt)
      case Array(address)       ⇒ new Host(address, defaultPort)
      case _                    ⇒ throw new IllegalArgumentException(s"Bad format: $host")
    }
  }
}
