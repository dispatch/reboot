package dispatch

// handy projections

object PromiseEither {
  def apply[A,B](underlying: Promise[Either[A,B]]) = new {
    def left = new LeftProjection(underlying)
    def right = new RightProjection(underlying)
  }

  class EitherDelegate[+A,+B](underlying: Promise[Either[A,B]])
  extends DelegatePromise[Either[A,B]] {
    def delegate = underlying
  }

  class LeftProjection[+A,+B](underlying: Promise[Either[A,B]]) {
    def flatMap[BB >: B,X](f: A => Promise[Either[X,BB]]) =
      new EitherDelegate(underlying) with Promise[Either[X,BB]] {
        def claim = underlying().left.flatMap { a => f(a)() }
      }
    def map[X](f: A => X) =
      new EitherDelegate(underlying) with Promise[Either[X,B]] {
        def claim = underlying().left.map(f)
      }
    def foreach[U](f: A => U) {
      underlying.addListener { () => underlying().left.foreach(f) }
    }
  }
  class RightProjection[+A,+B](underlying: Promise[Either[A,B]]) {
    def flatMap[AA >: A,Y](f: B => Promise[Either[AA,Y]]) =
      new EitherDelegate(underlying) with Promise[Either[AA,Y]] {
        def claim = underlying().right.flatMap { b => f(b)() }
      }
    def map[Y](f: B => Y) =
      new EitherDelegate(underlying) with Promise[Either[A,Y]] {
        def claim = underlying().right.map(f)
      }
    def foreach(f: B => Unit) {
      underlying.addListener { () => underlying().right.foreach(f) }
    }
  }
}

object PromiseIterable {
  def apply[A](underlying: Promise[Iterable[A]]) = new {
    /** Facilitates projection over promised iterables */
    def values = new Values(underlying)
  }

  class Flatten[A](underlying: Promise[Iterable[A]]) {
    def flatMap[Iter[B] <: Iterable[B], B](f: A => Promise[Iter[B]]) =
      new ComposedPromise[Iterable[A],Iterable[B]] {
        def a = underlying
        def b = Promise.all(underlying().map(f)).map { _.flatten }
      }
    def map[Iter[B] <: Iterable[B], B](f: A => Iter[B])
    : Promise[Iterable[B]] =
      underlying.map { _.map(f) }.map { _.flatten }
    def foreach(f: A => Unit) {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Flatten(underlying.map { _.filter(p) })
    def filter(p: A => Boolean) = withFilter(p)
  }
  class Values[A](underlying: Promise[Iterable[A]]) {
    def flatMap[B](f: A => Promise[B]) =
      new ComposedPromise[Iterable[A],Iterable[B]] {
        def a = underlying
        def b = Promise.all(underlying().map(f))
      }
    def map[B](f: A => B): Promise[Iterable[B]] =
      underlying.map { _.map(f) }
    def foreach(f: A => Unit) {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Values(underlying.map { _.filter(p) })
    def filter(p: A => Boolean) = withFilter(p)
    def flatten = new Flatten(underlying)
  }
}
