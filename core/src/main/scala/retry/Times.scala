package dispatch.retry

import dispatch._

object Times {
  def apply[T](count: Int)(implicit predicate: Predicate[T]):
  (T => Promise[T]) =
    sys.error("todo")
}

trait Predicate[T] extends (T => Boolean)

object Test {
  implicit def eitherPredicate[A,B]: Predicate[Either[A,B]] =
    sys.error("todo")

  def test(p: Promise[Either[String,Int]]): Promise[Either[String,Int]] =
    p.flatMap(Times(5))
}
