package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import scala.util.control.Exception.allCatch

trait Promise[+A] extends Function0[A] { self =>
  def apply() = get
  def get: A
  def filter(p: A => Boolean) =
    new Promise[A] {
      def get = {
        val r = self.get
        if (p(r)) r
        else throw new java.util.NoSuchElementException("Empty Promise.get")
      }
      def foreach(f: A => Unit) =
        for (a <- self)
          if (p(a)) f(a)
    }
  def map[B](f: A => B) =
    new Promise[B] {
      def get = f(self.get)
      def foreach(f2: B => Unit) =
        for (a <- self)
          f2(f(a))
    }
  def flatMap[B, C, That <: Promise[C]]
             (f: A => B)
             (implicit guarantor: Guarantor[B,C,That]) =
    new Promise[C] {
      def get = guarantor.promise(f(self.get)).get
      def foreach(f2: C => Unit) =
        for {
          a <- self
          b <- guarantor.promise(f(a))
        } f2(b)
    }
  /** Promise to asynchronously cause some side effect */
  def foreach(f: A => Unit)

  def either =
    new Promise[Either[Throwable,A]] {
      // retain this as the exception will not be thrown more than once
      // by the underlying get
      lazy val get = allCatch.either { self.get }

      def foreach(f: Either[Throwable,A] => Unit) =
        for ( a <- self)
          f(allCatch.either { a })
    }

  def option: Promise[Option[A]] =
    either.map { _.right.toOption }
}

class ListenableFuturePromise[A](
  underlying: ListenableFuture[A],
  executor: juc.Executor
) extends Promise[A] {
  def get = underlying.get
  def foreach(f: A => Unit) =
    underlying.addListener(new Runnable {
      def run {
        try {
          f(get)
        } catch { case _ => println("caught") }
      }
    }, executor)
}

object Promise {
  def make[A](underlying: ListenableFuture[A])
             (implicit executor: juc.Executor) =
    new ListenableFuturePromise(underlying, executor)

  def all[A](promises: Traversable[Promise[A]]) =
    new Promise[Traversable[A]] { self =>
      def get = promises.map { _.get }
      def foreach(f: Traversable[A] => Unit) = {
        val count = new juc.atomic.AtomicInteger(promises.size)
        for (p <- promises; a <- p)
          if (count.decrementAndGet == 0)
            f(self.get)
      }
    }
  implicit def traversable[T] = new TraversableGuarantor[T]
  implicit def identity[T] = new IdentityGuarantor[T]
}

trait Guarantor[-A, B, That <: Promise[B]] {
  def promise(collateral: A): That
}

class TraversableGuarantor[T] extends Guarantor[
  Traversable[Promise[T]],
  Traversable[T],
  Promise[Traversable[T]]
] {
  def promise(collateral: Traversable[Promise[T]]) =
    Promise.all(collateral)
}

class IdentityGuarantor[T] extends Guarantor[Promise[T],T,Promise[T]] {
  def promise(collateral: Promise[T]) = collateral
}
