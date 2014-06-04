## Before start

**AerospikeClient()** and **Namespace()** are consumer of configuration information, so keep in mind that if the
[config file](https://github.com/otrimegistro/aerospikez/blob/master/src/test/resources/reference.conf) are:
- missing, but you provide configurations as argument then these are applied.
- encounter and also you provide configurations as argument, the config file always wins.
- missing and you not provide a configuration as argument, then defaults values (from this library) are applied.
