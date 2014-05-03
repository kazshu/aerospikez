package aerospikez

import com.aerospike.client.policy.{ RecordExistsAction, GenerationPolicy, WritePolicy, QueryPolicy, Priority }
import com.aerospike.client.async.{ MaxCommandAction, AsyncClientPolicy }

import com.typesafe.config.{ Config, ConfigFactory }
import Util.trySome

private[aerospikez] object ConfigFile {
  var file: Config = ConfigFactory.load()
  val config: Option[Config] = trySome(file.getConfig("aerospike"))
}

object ClientConfig {

  def apply(
    timeout: Int = 1000,
    asyncMaxCommands: Int = 200,
    maxSocketIdle: Int = 14,
    asyncSelectorThreads: Int = 1,
    asyncSelectorTimeout: Int = 0,
    sharedThreadPool: Boolean = false,
    failIfNotConnected: Boolean = true,
    asyncMaxCommandAction: MaxCommandAction = MaxCommandAction.REJECT): ClientConfig = {

    new ClientConfig(
      ConfigFile.config.map(_.getInt("client-policy.async-selector-timeout")).getOrElse(asyncSelectorTimeout),
      ConfigFile.config.map(_.getInt("client-policy.timeout")).getOrElse(timeout),
      ConfigFile.config.map(_.getInt("client-policy.async-max-commands")).getOrElse(asyncMaxCommands),
      ConfigFile.config.map(_.getInt("client-policy.max-socket-idle")).getOrElse(maxSocketIdle),
      ConfigFile.config.map(_.getInt("client-policy.async-selector-threads")).getOrElse(asyncSelectorThreads),
      ConfigFile.config.map(_.getBoolean("client-policy.shared-thread-pool")).getOrElse(sharedThreadPool),
      ConfigFile.config.map(_.getBoolean("client-policy.fail-if-not-connected")).getOrElse(failIfNotConnected),
      ConfigFile.config.map(_.getString("client-policy.async-max-command-action")).map {
        parseAction(_, asyncMaxCommandAction)
      }.getOrElse(asyncMaxCommandAction)
    )

  }

  private[aerospikez] def parseAction(action: String, defaultAction: MaxCommandAction): MaxCommandAction = action.toUpperCase match {
    case "ACCEPT" ⇒ MaxCommandAction.ACCEPT
    case "REJECT" ⇒ MaxCommandAction.REJECT
    case "BLOCK"  ⇒ MaxCommandAction.BLOCK
    case _        ⇒ defaultAction
  }
}

private[aerospikez] class ClientConfig(
    asyncSelectorTimeout: Int,
    timeout: Int,
    asyncMaxCommands: Int,
    maxSocketIdle: Int,
    asyncSelectorThreads: Int,
    sharedThreadPool: Boolean,
    failIfNotConnected: Boolean,
    asyncMaxCommandAction: MaxCommandAction) {

  private[aerospikez] def getPolicy() = {

    val clientPolicy = new AsyncClientPolicy()
    clientPolicy.timeout = timeout
    clientPolicy.maxSocketIdle = maxSocketIdle
    clientPolicy.sharedThreadPool = sharedThreadPool
    clientPolicy.asyncMaxCommands = asyncMaxCommands
    clientPolicy.failIfNotConnected = failIfNotConnected
    clientPolicy.asyncSelectorThreads = asyncSelectorThreads
    clientPolicy.asyncSelectorTimeout = asyncSelectorTimeout
    clientPolicy.asyncMaxCommandAction = asyncMaxCommandAction

    clientPolicy
  }
}

object QueryConfig {

  def apply(
    timeout: Int = 0,
    maxRetries: Int = 2,
    recordQueueSize: Int = 5000,
    maxConcurrentNodes: Int = 0,
    sleepBetweenRetries: Int = 500): QueryConfig = {

    new QueryConfig(
      ConfigFile.config.map(_.getInt("query-policy.timeout")).getOrElse(timeout),
      ConfigFile.config.map(_.getInt("query-policy.max-retries")).getOrElse(maxRetries),
      ConfigFile.config.map(_.getInt("query-policy.record-queue-size")).getOrElse(recordQueueSize),
      ConfigFile.config.map(_.getInt("query-policy.max-concurrent-nodes")).getOrElse(maxConcurrentNodes),
      ConfigFile.config.map(_.getInt("query-policy.sleep-between-retries")).getOrElse(sleepBetweenRetries)
    )

  }
}

private[aerospikez] class QueryConfig(
    timeout: Int,
    maxRetries: Int,
    recordQueueSize: Int,
    maxConcurrentNodes: Int,
    sleepBetweenRetries: Int) {

  private[aerospikez] def getPolicy() = {

    val queryPolicy = new QueryPolicy()
    queryPolicy.timeout = timeout
    queryPolicy.maxRetries = maxRetries
    queryPolicy.recordQueueSize = recordQueueSize
    queryPolicy.maxConcurrentNodes = maxConcurrentNodes
    queryPolicy.sleepBetweenRetries = sleepBetweenRetries

    queryPolicy
  }
}

object WriteConfig {

  def apply(
    timeout: Int = 0,
    expiration: Int = 0,
    generation: Int = 0,
    maxRetries: Int = 2,
    sleepBetweenRetries: Int = 500,
    priority: Priority = Priority.DEFAULT,
    generationPolicy: GenerationPolicy = GenerationPolicy.NONE,
    recordExistsAction: RecordExistsAction = RecordExistsAction.UPDATE): WriteConfig = {

    new WriteConfig(
      ConfigFile.config.map(_.getInt("write-policy.timeout")).getOrElse(timeout),
      ConfigFile.config.map(_.getInt("write-policy.expiration")).getOrElse(expiration),
      ConfigFile.config.map(_.getInt("write-policy.generation")).getOrElse(generation),
      ConfigFile.config.map(_.getInt("write-policy.max-retries")).getOrElse(maxRetries),
      ConfigFile.config.map(_.getInt("write-policy.sleep-between-retries")).getOrElse(sleepBetweenRetries),
      ConfigFile.config.map(_.getString("write-policy.priority")).map {
        parsePriority(_, priority)
      }.getOrElse(priority),
      ConfigFile.config.map(_.getString("write-policy.generation-policy")).map {
        parseGenerationPolicy(_, generationPolicy)
      }.getOrElse(generationPolicy),
      ConfigFile.config.map(_.getString("write-policy.record-exists-action")).map {
        parseRecordExistsAction(_, recordExistsAction)
      }.getOrElse(recordExistsAction)
    )
  }

  private[aerospikez] def parseRecordExistsAction(recordExistsAction: String, defaultRecordExistsAction: RecordExistsAction): RecordExistsAction =
    recordExistsAction.toUpperCase match {
      case "UPDATE"       ⇒ RecordExistsAction.UPDATE
      case "UPDATE_ONLY"  ⇒ RecordExistsAction.UPDATE_ONLY
      case "REPLACE"      ⇒ RecordExistsAction.REPLACE
      case "REPLACE_ONLY" ⇒ RecordExistsAction.REPLACE_ONLY
      case "CREATE_ONLY"  ⇒ RecordExistsAction.CREATE_ONLY
      case _              ⇒ defaultRecordExistsAction
    }

  private[aerospikez] def parseGenerationPolicy(generationPolicy: String, defaultGenerationPolicy: GenerationPolicy): GenerationPolicy =
    generationPolicy.toUpperCase match {
      case "NONE"             ⇒ GenerationPolicy.NONE
      case "DUPLICATE"        ⇒ GenerationPolicy.DUPLICATE
      case "EXPECT_GEN_GT"    ⇒ GenerationPolicy.EXPECT_GEN_GT
      case "EXPECT_GEN_EQUAL" ⇒ GenerationPolicy.EXPECT_GEN_EQUAL
      case _                  ⇒ defaultGenerationPolicy
    }

  private[aerospikez] def parsePriority(priority: String, defaultPriority: Priority): Priority =
    priority.toUpperCase match {
      case "DEFAULT" ⇒ Priority.DEFAULT
      case "MEDIUM"  ⇒ Priority.MEDIUM
      case "HIGH"    ⇒ Priority.HIGH
      case "LOW"     ⇒ Priority.LOW
      case _         ⇒ defaultPriority
    }
}

private[aerospikez] class WriteConfig(
    timeout: Int,
    expiration: Int,
    generation: Int,
    maxRetries: Int,
    sleepBetweenRetries: Int,
    priority: Priority,
    generationPolicy: GenerationPolicy,
    recordExistsAction: RecordExistsAction) {

  private[aerospikez] def getPolicy() = {

    val writePolicy = new WritePolicy()
    writePolicy.timeout = timeout
    writePolicy.expiration = expiration
    writePolicy.generation = generation
    writePolicy.maxRetries = maxRetries
    writePolicy.recordExistsAction = recordExistsAction
    writePolicy.sleepBetweenRetries = sleepBetweenRetries
    writePolicy.generationPolicy = generationPolicy
    writePolicy.priority = priority

    writePolicy
  }
}
