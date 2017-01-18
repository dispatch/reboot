package dispatch

// handy projections

import scala.concurrent.{ExecutionContext}

object FutureEither {

  class LeftProjection[+A,+B]
  (underlying: Future[Either[A,B]])(implicit executor: ExecutionContext) {

    def flatMap[BB >: B,X](f: A => Future[Either[X,BB]]):
      Future[Either[X,BB]] =
      underlying.flatMap {
        _.fold(a => f(a),
                 b => Future.successful(Right(b)))
      }

    def map[X](f: A => X): Future[Either[X,B]] =
      underlying.map {
        _.left.map(f)
      }

    def foreach[U](f: A => U) = {
      underlying.foreach { _.left.foreach(f) }
    }
  }
  class RightProjection[+A,+B]
  (underlying: Future[Either[A,B]])(implicit executor: ExecutionContext) {
    def flatMap[AA >: A,Y](f: B => Future[Either[AA,Y]]):
    Future[Either[AA,Y]] =
      underlying.flatMap { eth =>
        eth.fold(a => Future.successful(Left(a)),
                 b => f(b))
      }

    def map[Y](f: B => Y): Future[Either[A,Y]] =
      underlying.map {
        _.right.map(f)
      }

    def foreach(f: B => Unit) = {
      underlying.foreach { _.right.foreach(f) }
    }
    def values[A1 >: A, C]
    (implicit ev: RightProjection[A, B] <:<
                  RightProjection[A1, Iterable[C]]) =
      new FutureRightIterable.Values(underlying, this)
  }
}

object FutureIterable {

  class Flatten[A](val underlying: Future[Iterable[A]])
                  (implicit executor: ExecutionContext) {

    def flatMap[Iter[B] <: Iterable[B], B](f: A => Future[Iter[B]]) =
      underlying.flatMap { iter =>
        Future.sequence(iter.map(f)).map { _.flatten }
      }
    def map[Iter[B] <: Iterable[B], B](f: A => Iter[B])
    : Future[Iterable[B]] =
      underlying.map { _.map(f) }.map { _.flatten }
    def foreach(f: A => Unit) = {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Flatten(underlying.map { _.filter(p) })
    def filter(p: A => Boolean) = withFilter(p)
  }

  class Values[A](underlying: Future[Iterable[A]])
                 (implicit executor: ExecutionContext) {
    def flatMap[B](f: A => Future[B]) =
      underlying.flatMap { iter =>
        Future.sequence(iter.map(f))
      }
    def map[B](f: A => B): Future[Iterable[B]] =
      underlying.map { _.map(f) }
    def foreach(f: A => Unit) = {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Values(underlying.map { _.filter(p) })
    def filter(p: A => Boolean) = withFilter(p)
    def flatten = new Flatten(underlying)
  }
}


object FutureRightIterable {
  import FutureEither.RightProjection
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
  class Flatten[E,A](parent: Future[_], underlying: RightIter[E,A])
                    (implicit executor: ExecutionContext) {
    def flatMap[Iter[B] <: Iterable[B], B]
    (f: A => Future[Either[E,Iter[B]]]) =
      underlying.flatMap { iter =>
        Future.sequence(iter.map(f)).map { eths =>
          flatRight(eths).right.map { _.flatten }
        }
      }
    def map[Iter[B] <: Iterable[B], B](f: A => Iter[B]) =
      underlying.flatMap { iter =>
        Future.successful(Right(iter.map(f).flatten))
      }
    def foreach(f: A => Unit) = {
      underlying.foreach { _.foreach(f) }
    }
    def withFilter(p: A => Boolean) =
      new Values(parent, underlying.map { _.filter(p) }.right)
    def filter(p: A => Boolean) = withFilter(p)
  }
  class Values[E,A](parent: Future[_], underlying: RightIter[E,A])
                   (implicit executor: ExecutionContext) {

    def flatMap[B](f: A => Future[Either[E,B]]) =
      underlying.flatMap { iter =>
        Future.sequence(iter.map(f)).map(flatRight)
      }
    def map[B](f: A => B) =
      underlying.flatMap { iter =>
        Future.successful(Right(iter.map(f)))
    }
    def foreach(f: A => Unit) = {
      underlying.foreach { _.foreach(f) }
    }
    def flatten = new Flatten(parent, underlying)
    def withFilter(p: A => Boolean) =
      new Values(parent, underlying.map { _.filter(p) }.right)
    def filter(p: A => Boolean) = withFilter(p)
  } 
}
