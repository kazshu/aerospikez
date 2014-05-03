package aerospikez

import org.specs2.mutable.Specification

import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigFactory

import com.aerospike.client.async.AsyncClient
import com.aerospike.client.Host

import scalaz.NonEmptyList

class AerospikezSpec extends Specification {

  val aerospikezInstance = Aerospikez.apply()

  val defaultPort: Int = 3000
  val defaultHosts: NonEmptyList[String] = Hosts("127.0.0.1:3000")

  val badConfigFile = ConfigFactory.load("inexistentFile")
  val goodConfigFile = ConfigFactory.parseString("""
    aerospike {
      port = 4000

      hosts = ["234.15.67.102:5000", "56.34.109.100"]
    }
  """, ConfigParseOptions.defaults)

  "An Aerospikez object" should {

    "received a optional hosts list, client config and config file" in {

      Aerospikez()
      Aerospikez(Hosts("127.0.0.1:3000"))
      Aerospikez(Hosts("127.0.0.1:3000"), ClientConfig())
      Aerospikez(Hosts("127.0.0.1:3000"), ClientConfig(), badConfigFile)

      // because the previos expresions will try to connect to the aeroskipe
      // server, if not exception are throw then everything is correct:
      success
    }

    "create an async client" in {

      aerospikezInstance.async should beAnInstanceOf[AsyncClient]
    }
  }

  /////////////////////////////////////////////////////////////////////
  // The nexts methods are compose when created an Aeroskipe client, //
  // so if all test are success, the composition work as expected    //
  /////////////////////////////////////////////////////////////////////

  "getPort method" should {

    "be take the general port indicated in the configuration file as default port" in {

      aerospikezInstance.getPort(goodConfigFile) must beEqualTo(4000)
    }

    "be use the 3000 port as default if there are not port indicated in the configuration file" in {

      aerospikezInstance.getPort(badConfigFile) must beEqualTo(defaultPort)
    }
  }

  "getHosts method" should {

    "be take the hosts/nodes that are indicated in the configuration file" in {

      aerospikezInstance.getHosts(
        goodConfigFile,
        defaultHosts).list must containTheSameElementsAs(List("234.15.67.102:5000", "56.34.109.100"))
    }

    "be use the \"localhost\" if there are not hosts indicated in the configuration file" in {

      aerospikezInstance.getHosts(
        badConfigFile,
        defaultHosts).list must containTheSameElementsAs(List("localhost"))
    }
  }

  "createHosts method" should {

    val firstHost = aerospikezInstance.getHosts(goodConfigFile, defaultHosts).head
    val secondHost = aerospikezInstance.getHosts(goodConfigFile, defaultHosts).tail.head

    "be create a host with address and port indicate in the configuration file" in {

      val host = aerospikezInstance.createHost(firstHost, defaultPort)

      host must beAnInstanceOf[Host]
      host.name must beEqualTo("234.15.67.102")
      host.port must beEqualTo(5000)
    }

    "be create host with the address and default port if only the address are indicate in the configuration file" in {

      val host = aerospikezInstance.createHost(secondHost, defaultPort)

      host must beAnInstanceOf[Host]
      host.name must beEqualTo("56.34.109.100")
      host.port must beEqualTo(3000)
    }

    "throw a exception if the host (string) are bad formed" in {

      lazy val host = aerospikezInstance.createHost("123.101.34.101:3000:lala", defaultPort)

      host must throwA[IllegalArgumentException]
    }
  }
}
