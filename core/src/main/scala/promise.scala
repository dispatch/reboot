package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import juc.TimeUnit
import scala.util.control.Exception.{allCatch,catching}

object Promise {
  implicit def iterable[T] = new IterableGuarantor[T]
  implicit def identity[T] = new IdentityGuarantor[T]

  class Factory(http: HttpExecutor) { factory =>
    def all[A](promises: Iterable[Promise[A]]): Promise[Iterable[A]] =
      new Promise[Iterable[A]] {
        def replay = all(for (p <- promises) yield p.replay)
        def claim = promises.map { _() }
        def isComplete = promises.forall { _.isComplete }
        val http = factory.http
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
    def sleep[T](d: Duration)(todo: => T) =
      new SleepPromise(factory.http, d, todo)
    def apply[T](existing: T): Promise[T] =
      new Promise[T] {
        def claim = existing
        def replay = factory.apply(existing)
        def isComplete = true
        val http = factory.http
        def addListener(f: () => Unit) =
          factory.http.promiseExecutor.execute(f)
      }
  }

  @deprecated("Use Http.promise.all or other HttpExecutor")
  def all[A](promises: Iterable[Promise[A]]): Promise[Iterable[A]] =
    Http.promise.all(promises)

  /** Wraps a known value in a Promise. Useful in binding
   *  some value to other promises in for-expressions. */
  @deprecated("Use Http.promise.apply or other HttpExecutor")
  def apply[T](existing: T): Promise[T] =
    Http.promise(existing)
}

trait Promise[+A] extends PromiseSIP[A] { self =>
  /** Claim promise or throw exception, should only be called once */
  protected def claim: A

  /** Replay operations that produce the promised value */
  def replay: Promise[A]
  /* Reference to parent HttpExecutor, to access configured background
   * Executor, default timeout Duration, and async Timer. */
  val http: HttpExecutor

  /** Internal cache of promised value or exception thrown */
  protected lazy val result = allCatch.either { claim }

  /** Listener to be called in an executor when promise is available */
  protected [dispatch] def addListener(f: () => Unit)

  /** True if promised value is available */
  def isComplete: Boolean

  /** Blocks until promised value is available, returns promised value or
   *  throws ExecutionException. */
  def apply() =
    result.fold(
      exc => throw(exc),
      identity
    )
  /** Nested promise that delegates listening directly to this self */
  protected trait SelfPromise[+B] extends DelegatePromise[A] with Promise[B] {
    def delegate = self
  }
  /** Map the promised value to something else */
  def map[B](f: A => B): Promise[B] =
    new SelfPromise[B] {
      addListener { () => result }
      def claim = f(self())
      def replay = self.replay.map(f)
    }
  /** Bind this Promise to another Promise, or something which an
   *  implicit Guarantor may convert to a Promise. */
  def flatMap[B, C, That <: Promise[C]]
             (f: A => B)
             (implicit guarantor: Guarantor[B,C,That]): Promise[C] =
    new Promise[C] {
      addListener { () => result }
      lazy val other = guarantor.promise(self, f(self()))
      def addListener(f: () => Unit) {
        for (_ <- self; _ <- other) f()
      }
      def isComplete = self.isComplete && other.isComplete
      val http = self.http
      def claim = other()
      def replay = self.replay.flatMap(f)(guarantor)
    }
  /** Support if clauses in for expressions. A filtered promise
   *  behaves like an Option, in that apply() will throw a
   *  NoSuchElementException when the promise is empty. */
  def withFilter(p: A => Boolean): Promise[A] =
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
      def isComplete = self.isComplete
      def replay = self.replay.withFilter(p)
      val http = self.http
    }
  /** filter still used for certain cases in for expressions */
  def filter(p: A => Boolean): Promise[A] = withFilter(p)
  /** Cause some side effect with the promised value, if it is produced
   *  with no exception */
  def foreach[U](f: A => U) {
    addListener { () => f(self()) }
  }

  /** Project promised value into an either containing the value or any
   *  exception thrown retrieving it. Unwraps `cause` of any top-level
   *  ExecutionException */
  def either: Promise[Either[Throwable, A]] =
    new Promise[Either[Throwable, A]] with SelfPromise[Either[Throwable, A]] {
      def claim = self.result.left.map { 
        case e: juc.ExecutionException => e.getCause
        case e => e
      }
      def replay = self.replay.either
    }

  /** Create a left projection of a contained either */
  def left[B,C](implicit ev: Promise[A] <:< Promise[Either[B, C]]) =
    new PromiseEither.LeftProjection(this)

  /** Create a right projection of a contained either */
  def right[B,C](implicit ev: Promise[A] <:< Promise[Either[B, C]]) =
    new PromiseEither.RightProjection(this)

  /** Project any resulting exception or result into a unified type X */
  def fold[X](fa: Throwable => X, fb: A => X): Promise[X] =
    for (eth <- either) yield eth.fold(fa, fb)

  def flatten[B](implicit pv: Promise[A] <:< Promise[Promise[B]]):
    Promise[B] = (this: Promise[Promise[B]]).flatMap(identity)

  /** Facilitates projection over promised iterables */
  def values[B](implicit ev: Promise[A] <:< Promise[Iterable[B]]) =
    new PromiseIterable.Values(this)

  /** Project promised value into an Option containing the value if retrived
   *  with no exception */
  def option: Promise[Option[A]] =
    either.map { _.right.toOption }

  /** Some value if promise is complete, otherwise None */
  def completeOption = 
    if (isComplete) Some(self())
    else None

  override def toString =
    "Promise(%s)".format(either.completeOption.map {
      case Left(exc) => "!%s!".format(exc.getMessage)
      case Right(value) => value.toString
    }.getOrElse("-incomplete-"))
}
trait DelegatePromise[+D] {
  def delegate: Promise[D]
  def addListener(f: () => Unit) { delegate.addListener(f) }
  def isComplete = delegate.isComplete
  val http = delegate.http
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
  underlyingIn: => ListenableFuture[A],
  val executor: juc.Executor,
  val http: HttpExecutor
) extends Promise[A] {
  lazy val underlying = underlyingIn
  def replay = new ListenableFuturePromise(underlyingIn, executor, http)
  def claim = http.timeout match {
    case Duration.None => underlying.get
    case Duration(duration, unit) => underlying.get(duration, unit)
  }
  def isComplete = underlying.isDone || underlying.isCancelled
  def addListener(f: () => Unit) =
    underlying.addListener(f, executor)
}

trait Guarantor[-A, B, That <: Promise[B]] {
  def promise(parent: Promise[_], collateral: A): That
}

class IterableGuarantor[T] extends Guarantor[
  Iterable[Promise[T]],
  Iterable[T],
  Promise[Iterable[T]]
] {
  def promise(parent: Promise[_], collateral: Iterable[Promise[T]]) =
    parent.http.promise.all(collateral)
}

class IdentityGuarantor[T] extends Guarantor[Promise[T],T,Promise[T]] {
  def promise(parent: Promise[_], collateral: Promise[T]) = collateral
}

case class Duration(length: Long, unit: TimeUnit) {
  def millis = unit.toMillis(length)
}

object Duration {
  val None = Duration(-1L, TimeUnit.MILLISECONDS)
  def millis(length: Long) = Duration(length, TimeUnit.MILLISECONDS)
}
