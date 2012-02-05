package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import scala.util.control.Exception.allCatch

trait Promise[+A] extends Function0[A] with PromiseSIP[A] { self =>
  /** Claim promise or throw exception, should only be called once */
  protected def claim: A

  /** Internal cache of promised value or exception thrown */
  protected lazy val result = allCatch.either { claim }

  /** Listener to be called in an executor when promise is available */
  protected def addListener(f: () => Unit)

  // lazily assign result when available, e.g. to evaluate mapped function
  // that may kick off further promises
  addListener { () => result }

  /** Blocks until promised value is available, returns promised value or throws
   *  its associated exception. */
  def apply() = result.fold(
    exc => throw(exc),
    identity
  )
  /** Nested promise that delegates listening directly to this self */
  protected trait SelfPromise[+A] extends Promise[A] {
      def addListener(f: () => Unit) { self.addListener(f) }
  }
  /** Map the promised value to something else */
  def map[B](f: A => B): Promise[B] =
    new SelfPromise[B] {
      def claim = f(self())
    }
  /** Bind this Promise to another Promise, or something which an
   *  implicit Guarantor may convert to a Promise. */
  def flatMap[B, C, That <: Promise[C]]
             (f: A => B)
             (implicit guarantor: Guarantor[B,C,That]): Promise[C] =
    new SelfPromise[C] {
      def claim = guarantor.promise(f(self()))()
    }
  /** Support if clauses in for expressions. A filtered promise
   *  behaves like an Option, in that apply() will throw a
   *  NoSuchElementException when the promise is empty. */
  def filter(p: A => Boolean): Promise[A] =
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
  /** Cause some side effect with the promised value, if it is produced
   *  with no exception */
  def foreach(f: A => Unit) {
    addListener { () => f(self()) }
  }

  /** Project promised value into an either containing the value or any
   *  exception thrown retrieving it */
  def either: Promise[Either[Throwable, A]] =
    new SelfPromise[Either[Throwable, A]] {
      def claim = self.result
    }

  /** Project promised value into an Option containing the value if retrived
   *  with no exception */
  def option: Promise[Option[A]] =
    either.map { _.right.toOption }
}

trait PromiseSIP[+A] { self: Promise[A] =>
  def onComplete[U](f: Either[Throwable, A] => U) = {
    for (e <- either) f(e)
    self
  }

  def onSuccess[U](f: PartialFunction[A, U]) = {
    for {
      p <- self
      _ <- f.lift(p)
    } ()
    self
  }

  def onFailure[U](f: PartialFunction[Throwable, U]) = {
    for {
      e <- either
      t <- e.left
      _ <- f.lift(t)
    } ()
    self
  }

  def recover[B >: A](f: PartialFunction[Throwable, B]): Promise[B] =
    for (e <- either) yield e.fold(
      t => f.lift(t).getOrElse { throw t },
      identity
    )

  def failed: Promise[Throwable] =
    for (e <- either if e.isLeft) yield e.left.get
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
