package aerospikez.internal.util

private[aerospikez] object Util {

  object distinct1 { implicit val distinct: distinct1.type = this }
  object distinct2 { implicit val distinct: distinct2.type = this }
  object distinct3 { implicit val distinct: distinct3.type = this }

  def trySome[A](a: ⇒ A): Option[A] = {
    try {
      Some(a)
    } catch {
      case _: Throwable ⇒ None
    }
  }

  def parseOption[V](value: V) = {

    value match {
      case Some(v) ⇒ v.asInstanceOf[V]
      case None    ⇒ null.asInstanceOf[V]
      case _       ⇒ value.asInstanceOf[V]
    }
  }
}
