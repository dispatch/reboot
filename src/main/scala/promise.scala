package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import scala.util.control.Exception.allCatch

trait Promise[+A] extends Function0[A] { self =>
  private lazy val result = allCatch.either { claim }
  def apply() = result.fold(
    exc => throw(exc),
    identity
  )
  protected def claim: A
  def filter(p: A => Boolean) =
    new Promise[A] {
      def claim = {
        val r = self()
        if (p(r)) r
        else throw new java.util.NoSuchElementException("Empty Promise")
      }
      /** Listener only invoked if predicate is met, otherwise exception
       * will be thrown by Promise#foreach */
      def addListener(f: () => Unit) {
        self.addListener { () =>
          if (p(self()))
            f()
        } 
      }
    }
  def map[B](f: A => B) =
    new Promise[B] {
      def claim = f(self())
      def addListener(f: () => Unit) { self.addListener(f) }
    }
  def flatMap[B, C, That <: Promise[C]]
             (f: A => B)
             (implicit guarantor: Guarantor[B,C,That]) =
    new Promise[C] {
      def claim = guarantor.promise(f(self()))()
      def addListener(f: () => Unit) { self.addListener(f) }
    }
  /** Promise to asynchronously cause some side effect */
  def foreach(f: A => Unit) {
    addListener { () => f(self()) }
  }

  def addListener(f: () => Unit)
  addListener { () => result }

  def either =
    new Promise[Either[Throwable,A]] {
      def claim = self.result

      def addListener(f: () => Unit) { self.addListener(f) }
    }

  def option: Promise[Option[A]] =
    either.map { _.right.toOption }
}

class ListenableFuturePromise[A](
  underlying: ListenableFuture[A],
  executor: juc.Executor
) extends Promise[A] {
  def claim = underlying.get
  def addListener(f: () => Unit) =
    underlying.addListener(new Runnable {
      def run {
        f()
      }
    }, executor)
}

object Promise {
  def make[A](underlying: ListenableFuture[A])
             (implicit executor: juc.Executor) =
    new ListenableFuturePromise(underlying, executor)

  def all[A](promises: Traversable[Promise[A]]) =
    new Promise[Traversable[A]] { self =>
      def claim = promises.map { _() }
      def addListener(f: () => Unit) = {
        val count = new juc.atomic.AtomicInteger(promises.size)
        promises.foreach { p =>
          p.addListener { () =>
            if (count.decrementAndGet == 0)
              f()
          }
        }
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
