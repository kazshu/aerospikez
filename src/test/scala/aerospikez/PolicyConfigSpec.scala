package aerospikez

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

class ConfigSpec extends Specification {

  "ClientConfig" should {

    "use the configuration file if exists" in {

      ConfigFile.file = ConfigFactory.load("reference.conf")
      val file = ConfigFile.file.getConfig("aerospike")
      val clientPolicy = ClientConfig().policy

      clientPolicy.timeout must beEqualTo(
        file.getInt("client-policy.timeout")
      )
      clientPolicy.maxSocketIdle must beEqualTo(
        file.getInt("client-policy.max-socket-idle")
      )
      clientPolicy.asyncMaxCommands must beEqualTo(
        file.getInt("client-policy.async-max-commands")
      )
      clientPolicy.asyncSelectorTimeout must beEqualTo(
        file.getInt("client-policy.async-selector-timeout")
      )
      clientPolicy.failIfNotConnected must beEqualTo(
        file.getBoolean("client-policy.fail-if-not-connected")
      )
      clientPolicy.asyncSelectorThreads must beEqualTo(
        file.getInt("client-policy.async-selector-threads")
      )
      clientPolicy.asyncMaxCommandAction.toString must beEqualTo(
        file.getString("client-policy.async-max-command-action")
      )
    }

    "use the default values if the configuration file is missing" in {

      ConfigFile.file = ConfigFactory.load()
      val clientPolicy = ClientConfig().policy

      clientPolicy.timeout must beEqualTo(0)
      clientPolicy.maxSocketIdle must beEqualTo(14)
      clientPolicy.asyncMaxCommands must beEqualTo(200)
      clientPolicy.asyncSelectorTimeout must beEqualTo(0)
      clientPolicy.asyncSelectorThreads must beEqualTo(1)
      clientPolicy.sharedThreadPool must beEqualTo(false)
      clientPolicy.failIfNotConnected must beEqualTo(true)
      clientPolicy.asyncMaxCommandAction.toString must beEqualTo("REJECT")
    }
  }

  "QueryConfig" should {

    "use the configuration file if exists" in {

      ConfigFile.file = ConfigFactory.load("reference.conf")
      val file = ConfigFile.file.getConfig("aerospike")
      val queryPolicy = QueryConfig().policy

      queryPolicy.timeout must beEqualTo(
        file.getInt("query-policy.timeout")
      )
      queryPolicy.maxRetries must beEqualTo(
        file.getInt("query-policy.max-retries")
      )
      queryPolicy.recordQueueSize must beEqualTo(
        file.getInt("query-policy.record-queue-size")
      )
      queryPolicy.maxConcurrentNodes must beEqualTo(
        file.getInt("query-policy.max-concurrent-nodes")
      )
      queryPolicy.sleepBetweenRetries must beEqualTo(
        file.getInt("query-policy.sleep-between-retries")
      )
    }

    "use the default values if the configuration file is missing" in {

      ConfigFile.file = ConfigFactory.load()
      val queryPolicy = QueryConfig().policy

      queryPolicy.timeout must beEqualTo(0)
      queryPolicy.maxRetries must beEqualTo(2)
      queryPolicy.recordQueueSize must beEqualTo(5000)
      queryPolicy.maxConcurrentNodes must beEqualTo(0)
      queryPolicy.sleepBetweenRetries must beEqualTo(500)
    }
  }

  "WriteConfig" should {

    "use the configuration file if exists" in {

      ConfigFile.file = ConfigFactory.load("reference.conf")
      val file = ConfigFile.file.getConfig("aerospike")
      val writePolicy = WriteConfig().policy

      writePolicy.timeout must beEqualTo(
        file.getInt("write-policy.timeout")
      )
      writePolicy.expiration must beEqualTo(
        file.getInt("write-policy.expiration")
      )
      writePolicy.generation must beEqualTo(
        file.getInt("write-policy.generation")
      )
      writePolicy.maxRetries must beEqualTo(
        file.getInt("write-policy.max-retries")
      )
      writePolicy.sleepBetweenRetries must beEqualTo(
        file.getInt("write-policy.sleep-between-retries")
      )
      writePolicy.priority.toString must beEqualTo(
        file.getString("write-policy.priority")
      )
      writePolicy.generationPolicy.toString must beEqualTo(
        file.getString("write-policy.generation-policy")
      )
      writePolicy.recordExistsAction.toString must beEqualTo(
        file.getString("write-policy.record-exists-action")
      )
    }

    "use the default values if the configuration file is missing" in {

      ConfigFile.file = ConfigFactory.load()
      val writePolicy = WriteConfig().policy

      writePolicy.timeout must beEqualTo(0)
      writePolicy.expiration must beEqualTo(0)
      writePolicy.generation must beEqualTo(0)
      writePolicy.maxRetries must beEqualTo(2)
      writePolicy.sleepBetweenRetries must beEqualTo(500)
      writePolicy.priority.toString must beEqualTo("DEFAULT")
      writePolicy.generationPolicy.toString must beEqualTo("NONE")
      writePolicy.recordExistsAction.toString must beEqualTo("UPDATE")
    }
  }
}
