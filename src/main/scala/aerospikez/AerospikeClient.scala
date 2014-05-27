package aerospikez

import scala.collection.JavaConversions.asScalaBuffer

import com.typesafe.config.{ ConfigFactory, Config }

import com.aerospike.client.{ Language, Host }
import com.aerospike.client.async.AsyncClient
import com.aerospike.client.lua.LuaConfig
import com.aerospike.client.policy.Policy
import com.aerospike.client.cluster.Node

import scalaz.NonEmptyList

import internal.util.Util._
import internal.util.TSafe._
import internal.util.Pimp._

object AerospikeClient {

  def apply(hosts: NonEmptyList[String] = NonEmptyList("localhost:3000"),
            clientConfig: ⇒ ClientConfig = ClientConfig(),
            configFile: Config = ConfigFactory.load()): AerospikeClient = {

    ConfigFile.file = configFile

    new AerospikeClient(hosts, clientConfig, configFile)
  }
}

private[aerospikez] class AerospikeClient(hosts: NonEmptyList[String], clientConfig: ClientConfig, configFile: Config) {

  private lazy val generalPolicy = new Policy { timeout = clientConfig.policy.timeout }

  private[aerospikez] final val asyncClient: AsyncClient = {

    new AsyncClient(
      clientConfig.policy,
      getHosts(configFile, hosts).map {
        createHost(_, getPort(configFile))
      }.list: _*
    )
  }

  def setOf[V](namespace: Namespace = Namespace(), name: String = "myset")(
    implicit ev: V DefaultTypeTo Any): SetOf[V] = {

    new SetOf[V](namespace, name, this.asyncClient, generalPolicy)
  }

  def register(name: String, path: String = "udf", language: String = "LUA"): Unit = {

    var realName: String = name
    lazy val realPath: String = if (path.endsWith("/")) path else path + "/"
    val realLanguage = language.toUpperCase match {
      case "LUA" ⇒
        LuaConfig.SourceDirectory = path
        if (!name.endsWith(".lua")) realName += ".lua"
        Language.LUA
      case _ ⇒ throw new IllegalArgumentException(s"$language is not supported as UDF for Aerospike")
    }

    asyncClient.register(
      generalPolicy,
      realPath + realName,
      realName,
      realLanguage
    )
  }

  def isConnected: Boolean = asyncClient.isConnected()

  def getNodes: Array[Node] = asyncClient.getNodes()

  def close: Unit = asyncClient.close()

  private[aerospikez] def getHosts(configFile: Config, hosts: NonEmptyList[String]): NonEmptyList[String] = {

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
