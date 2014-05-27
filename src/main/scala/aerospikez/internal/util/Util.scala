package aerospikez.internal.util

private[aerospikez] object Util {

  def trySome[A](a: ⇒ A): Option[A] = {
    try {
      Some(a)
    } catch {
      case _: Throwable ⇒ None
    }
  }
}
