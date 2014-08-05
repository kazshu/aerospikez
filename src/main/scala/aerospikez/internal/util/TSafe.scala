package aerospikez.internal.util

private[aerospikez] object TSafe {

  @annotation.implicitNotFound(
    msg = """
    That Set has been forced to accept only ${T2} (or a Option[${T2}]) as Value, but you provide a ${T1}:
    """
  )
  sealed class TypeOf[T1, T2]
  object TypeOf {
    implicit def f1[T1, T2](implicit ev: T1 <:< T2): TypeOf[T1, T2] = new TypeOf[T1, T2]
    implicit def f2[T1, T2](implicit ev: T1 =:= Option[T2]): TypeOf[T1, T2] = new TypeOf[T1, T2]
  }

  @annotation.implicitNotFound(
    msg = """
    Aaerospike support only String, Int, Long and Array[Byte] as Key, but you provide a ${K}:
    """
  )
  sealed trait KRestriction[K]
  object KRestriction {
    implicit object int extends KRestriction[Int]
    implicit object long extends KRestriction[Long]
    implicit object string extends KRestriction[String]
    implicit object arraybyte extends KRestriction[Array[Byte]]
  }

  @annotation.implicitNotFound(
    msg = """
    Aaerospike support only String, Int, Long, Map and List as Value (or a Option[T] where T is any type described above), but you provide a ${V}:
    """
  )
  sealed class VRestriction[V]
  object VRestriction {
    implicit object int extends VRestriction[Int]
    implicit object long extends VRestriction[Long]
    implicit object string extends VRestriction[String]
    implicit def list[A: VRestriction]: VRestriction[List[A]] = new VRestriction[List[A]]
    implicit def option[A: VRestriction]: VRestriction[Option[A]] = new VRestriction[Option[A]]
    implicit def map[A: VRestriction, B: VRestriction]: VRestriction[Map[A, B]] = new VRestriction[Map[A, B]]

    // This is necessary only if the user no specified a type argument (default to Any)
    implicit object any extends VRestriction[Any]
  }

  @annotation.implicitNotFound(
    msg = """
    Aerospike support only Int, Long, String, Map and List as Lua Type Result, but you provide a ${LuaV}:
    """
  )
  sealed class LRestriction[LuaV]
  object LRestriction {
    implicit object int extends LRestriction[Int]
    implicit object long extends LRestriction[Long]
    implicit object string extends LRestriction[String]
    implicit def list[A]: LRestriction[List[A]] = new LRestriction[List[A]]
    implicit def map[A, B]: LRestriction[Map[A, B]] = new LRestriction[Map[A, B]]

    // This is necessary only if the user no specified a type argument (default to Any)
    implicit object any extends LRestriction[Any]
  }

  @annotation.implicitNotFound(
    msg = """
    Aerospike support only Int and String as Type for Secondary Index, but you provide a ${I}:
    """
  )
  sealed class IRestriction[I]
  object IRestriction {
    implicit object int extends IRestriction[Int]
    implicit object string extends IRestriction[String]
  }

  trait Empty
  @annotation.implicitNotFound(
    msg = """
    An explicit type parameter is required.
    """
  )
  sealed trait =!=[T1, T2]
  object =!= {
    class Impl[T1, T2]

    object Impl {
      implicit def neq[T1, T2]: T1 Impl T2 = null
      implicit def neqAmbig1[T1]: T1 Impl T1 = null
      implicit def neqAmbig2[T1]: T1 Impl T1 = null
    }

    implicit def f[T1, T2](implicit eV: T1 Impl T2): T1 =!= T2 = null
  }

  sealed class DefaultTypeTo[T1, T2]
  trait TypePassed {
    implicit def typePassed[T1, T2]: DefaultTypeTo[T1, T2] = new DefaultTypeTo[T1, T2]
  }
  object DefaultTypeTo extends TypePassed {
    implicit def defaultType[T2]: DefaultTypeTo[T2, T2] = new DefaultTypeTo[T2, T2]
  }
}
