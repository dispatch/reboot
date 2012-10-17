package dispatch

// handy projections

object PromiseEither {
  class EitherDelegate[+A,+B](underlying: Promise[Either[A,B]])
  extends DelegatePromise[Either[A,B]] {
    def delegate = underlying
  }

  class LeftProjection[+A,+B](underlying: => Promise[Either[A,B]]) { self =>
    private def repeat = new LeftProjection(underlying.repeat)
    def flatMap[BB >: B,X](f: A => Promise[Either[X,BB]]):
      Promise[Either[X,BB]] =
      new EitherDelegate(underlying) with Promise[Either[X,BB]] {
        def claim = underlying().left.flatMap { a => f(a)() }
        def repeat = self.repeat.flatMap(f)
      }
    def map[X](f: A => X): Promise[Either[X,B]] =
      new EitherDelegate(underlying) with Promise[Either[X,B]] {
        def claim = underlying().left.map(f)
        def repeat = self.repeat.map(f)
      }
    def foreach[U](f: A => U) {
      underlying.addListener { () => underlying().left.foreach(f) }
    }
  }
  class RightProjection[+A,+B](underlying: Promise[Either[A,B]]) { self =>
    private def repeat = new RightProjection(underlying.repeat)
    def flatMap[AA >: A,Y](f: B => Promise[Either[AA,Y]]):
    Promise[Either[AA,Y]] =
      new EitherDelegate(underlying) with Promise[Either[AA,Y]] {
        def claim = underlying().right.flatMap { b => f(b)() }
        def repeat = self.repeat.flatMap(f)
      }
    def map[Y](f: B => Y): Promise[Either[A,Y]] =
      new EitherDelegate(underlying) with Promise[Either[A,Y]] {
        def claim = underlying().right.map(f)
        def repeat = self.repeat.map(f)
      }
    def foreach(f: B => Unit) {
      underlying.addListener { () => underlying().right.foreach(f) }
    }
    def values[A1 >: A, C]
    (implicit ev: RightProjection[A, B] <:<
                  RightProjection[A1, Iterable[C]]) =
      new PromiseRightIterable.Values(underlying, this)
  }

}

object PromiseIterable {

  class Flatten[A](val underlying: Promise[Iterable[A]]) {
    import underlying.http.promise
    def flatMap[Iter[B] <: Iterable[B], B](f: A => Promise[Iter[B]]) =
      underlying.flatMap { iter =>
        promise.all(iter.map(f)).map { _.flatten }
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
    import underlying.http.promise
    def flatMap[B](f: A => Promise[B]) =
      underlying.flatMap { iter =>
        promise.all(iter.map(f))
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

object PromiseRightIterable {
  import PromiseEither.RightProjection
  type RightIter[E,A] = RightProjection[E,Iterable[A]]

  private def flatRight[L,R](eithers: Iterable[Either[L,R]]) = {
    val start: Either[L,Seq[R]] = Right(Seq.empty)
    (start /: eithers) { (a, e) =>
      for {
        seq <- a.right
        cur <- e.right
      } yield (seq :+ cur)
    }
  }
  class Flatten[E,A](parent: Promise[_], underlying: RightIter[E,A]) {
    import parent.http.promise
    def flatMap[Iter[B] <: Iterable[B], B]
    (f: A => Promise[Either[E,Iter[B]]]) =
      underlying.flatMap { iter =>
        promise.all(iter.map(f)).map { eths =>
          flatRight(eths).right.map { _.flatten }
        }
      }
    def map[Iter[B] <: Iterable[B], B](f: A => Iter[B]) =
      underlying.flatMap { iter =>
        promise(Right(iter.map(f).flatten))
      }
    def foreach(f: A => Unit) {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Values(parent, underlying.map { _.filter(p) }.right)
    def filter(p: A => Boolean) = withFilter(p)
  }
  class Values[E,A](parent: Promise[_], underlying: RightIter[E,A]) {
    import parent.http.promise
    def flatMap[B](f: A => Promise[Either[E,B]]) =
      underlying.flatMap { iter =>
        promise.all(iter.map(f)).map(flatRight)
      }
    def map[B](f: A => B) =
      underlying.flatMap { iter =>
        promise(Right(iter.map(f)))
    }
    def foreach(f: A => Unit) {
      underlying.foreach { _.foreach(f) }
    }
    def flatten = new Flatten(parent, underlying)
    def withFilter(p: A => Boolean) =
      new Values(parent, underlying.map { _.filter(p) }.right)
    def filter(p: A => Boolean) = withFilter(p)
  }
}
