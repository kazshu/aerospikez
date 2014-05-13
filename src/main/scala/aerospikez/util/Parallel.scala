// This is a extended implementation of @pchiusano idea: https://gist.github.com/pchiusano/8965595
package aerospikez.util

import scalaz.{ Nondeterminism, Applicative }
import scalaz.concurrent.Task

object Parallel {

  def apply[V1, V2, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2])(f: (V1, V2) ⇒ R): Task[R] = applicative.apply2(a, b)(f)

  def apply[V1, V2, V3, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2],
    c: ⇒ Task[V3])(f: (V1, V2, V3) ⇒ R): Task[R] = applicative.apply3(a, b, c)(f)

  def apply[V1, V2, V3, V4, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2],
    c: ⇒ Task[V3],
    d: ⇒ Task[V4])(f: (V1, V2, V3, V4) ⇒ R): Task[R] = applicative.apply4(a, b, c, d)(f)

  def apply[V1, V2, V3, V4, V5, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2],
    c: ⇒ Task[V3],
    d: ⇒ Task[V4],
    e: ⇒ Task[V5])(f: (V1, V2, V3, V4, V5) ⇒ R): Task[R] = applicative.apply5(a, b, c, d, e)(f)

  def apply[V1, V2, V3, V4, V5, V6, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2],
    c: ⇒ Task[V3],
    d: ⇒ Task[V4],
    e: ⇒ Task[V5],
    r: ⇒ Task[V6])(f: (V1, V2, V3, V4, V5, V6) ⇒ R): Task[R] = applicative.apply6(a, b, c, d, e, r)(f)

  def apply[V1, V2, V3, V4, V5, V6, V7, R](
    a: ⇒ Task[V1],
    b: ⇒ Task[V2],
    c: ⇒ Task[V3],
    d: ⇒ Task[V4],
    e: ⇒ Task[V5],
    r: ⇒ Task[V6],
    s: ⇒ Task[V7])(f: (V1, V2, V3, V4, V5, V6, V7) ⇒ R): Task[R] = applicative.apply7(a, b, c, d, e, r, s)(f)

  private val applicative = new Applicative[Task] {

    def point[A](a: ⇒ A) = Task.now(a)

    def ap[A, B](a: ⇒ Task[A])(f: ⇒ Task[A ⇒ B]): Task[B] =
      apply2(f, a)(_(_))

    override def apply2[V1, V2, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2])(f: (V1, V2) ⇒ R): Task[R] =
      Nondeterminism[Task].mapBoth(a, b)(f)

    override def apply3[V1, V2, V3, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2],
      c: ⇒ Task[V3])(f: (V1, V2, V3) ⇒ R): Task[R] =
      apply2(apply2(a, b)((_, _)), c)((ab, c) ⇒ f(ab._1, ab._2, c))

    override def apply4[V1, V2, V3, V4, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2],
      c: ⇒ Task[V3],
      d: ⇒ Task[V4])(f: (V1, V2, V3, V4) ⇒ R): Task[R] =
      apply2(apply2(a, b)((_, _)), apply2(c, d)((_, _)))((ab, cd) ⇒ f(ab._1, ab._2, cd._1, cd._2))

    override def apply5[V1, V2, V3, V4, V5, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2],
      c: ⇒ Task[V3],
      d: ⇒ Task[V4],
      e: ⇒ Task[V5])(f: (V1, V2, V3, V4, V5) ⇒ R): Task[R] =
      apply2(apply2(a, b)((_, _)), apply3(c, d, e)((_, _, _)))((ab, cde) ⇒ f(ab._1, ab._2, cde._1, cde._2, cde._3))

    override def apply6[V1, V2, V3, V4, V5, V6, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2],
      c: ⇒ Task[V3],
      d: ⇒ Task[V4],
      e: ⇒ Task[V5],
      r: ⇒ Task[V6])(f: (V1, V2, V3, V4, V5, V6) ⇒ R): Task[R] =
      apply2(apply3(a, b, c)((_, _, _)), apply3(d, e, r)((_, _, _)))((abc, der) ⇒ f(abc._1, abc._2, abc._3, der._1, der._2, der._3))

    override def apply7[V1, V2, V3, V4, V5, V6, V7, R](
      a: ⇒ Task[V1],
      b: ⇒ Task[V2],
      c: ⇒ Task[V3],
      d: ⇒ Task[V4],
      e: ⇒ Task[V5],
      r: ⇒ Task[V6],
      s: ⇒ Task[V7])(f: (V1, V2, V3, V4, V5, V6, V7) ⇒ R): Task[R] =
      apply2(apply4(a, b, c, d)((_, _, _, _)), apply3(e, r, s)((_, _, _)))((abcd, ers) ⇒ f(abcd._1, abcd._2, abcd._3, abcd._4, ers._1, ers._2, ers._3))
  }
}
