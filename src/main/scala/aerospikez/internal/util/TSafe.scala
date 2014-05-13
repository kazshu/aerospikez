package aerospikez.internal.util

private[aerospikez] object TSafe {

  @annotation.implicitNotFound(msg = "That Set has been forced to accept only ${V} (or subtype) as Value, but you provide a ${V2}!")
  sealed class SubTypeOf[V2, V]
  object SubTypeOf {
    implicit def f[V2, V](implicit ev: V2 <:< V): SubTypeOf[V2, V] = new SubTypeOf[V2, V]
  }

  @annotation.implicitNotFound(msg = "Aaerospike support only String, Int, Long and Array[Byte] as Key, but you provide a ${K}!")
  trait SupportKey[@specialized(Int, Long) K]
  object SupportKey {
    implicit object string extends SupportKey[String]
    implicit object int extends SupportKey[Int]
    implicit object long extends SupportKey[Long]
    implicit object arraybyte extends SupportKey[Array[Byte]]
  }

  @annotation.implicitNotFound(msg = "Aaerospike support only String, Int, Long, Map and List as Value, but you provide a ${V}!")
  trait SupportValue[@specialized(Int, Long) V]
  object SupportValue {
    implicit object string extends SupportValue[String]
    implicit object int extends SupportValue[Int]
    implicit object long extends SupportValue[Long]
    implicit object list extends SupportValue[List[_]]
    implicit object map extends SupportValue[Map[_, _]]
  }

  sealed class DefaultTypeTo[T1, T2]
  trait DefaultToAny {
    implicit def defaultAny[T1, T2]: DefaultTypeTo[T1, T2] = new DefaultTypeTo[T1, T2]
  }
  object DefaultTypeTo extends DefaultToAny {
    implicit def defaultType[T2]: DefaultTypeTo[T2, T2] = new DefaultTypeTo[T2, T2]
  }
}
