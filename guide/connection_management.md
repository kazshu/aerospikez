## Cluster Connection Management

### Connecting

```scala
import aerospikez.AerospikeClient

val client = AerospikeClient()    // default by the library: localhost and 3000 port
```

Other alternatives:

- a diferent name of config file:
```scala
import com.typesafe.config.ConfigFactory
import aerospikez.AerospikeClient

val client = AerospikeClient(configFile = ConfigFactory.load("myconfig.conf"))
```

- if you want to specified hosts (remember if you use the config file, this is no necessary):
```scala
import aerospikez.{ AerospikeClient , Hosts }
val client = AerospikeClient(Hosts("203.87.169.58:3000", "203.87.169.59:3000"))
```

### Connection check

```scala
client.isConnected
```

### Listing active server nodes

```scala
client.getNodes
```

### Close Connection

```scala
client.close
```
