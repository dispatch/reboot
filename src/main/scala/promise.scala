package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import juc.TimeUnit
import scala.util.control.Exception.{allCatch,catching}

trait Promise[+A] extends PromiseSIP[A] { self =>
  /** Claim promise or throw exception, should only be called once */
  protected def claim: A

  def timeout: Duration

  /** Internal cache of promised value or exception thrown */
  protected lazy val result = allCatch.either { claim }

  /** Listener to be called in an executor when promise is available */
  protected def addListener(f: () => Unit)

  /** True if promised value is available */
  def isComplete: Boolean

  // lazily assign result when available, e.g. to evaluate mapped function
  // that may kick off further promises
  addListener { () => result }

  /** Blocks until promised value is available, returns promised value or
   *  throws ExecutionException. */
  def apply() =
    result.fold(
      exc => throw(exc),
      identity
    )
  /** Nested promise that delegates listening directly to this self */
  protected trait SelfPromise[+A] extends Promise[A] {
    def addListener(f: () => Unit) { self.addListener(f) }
    def isComplete = self.isComplete
    def timeout = self.timeout
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
      def isComplete = self.isComplete && p(self())
      def timeout = self.timeout
    }
  /** Cause some side effect with the promised value, if it is produced
   *  with no exception */
  def foreach(f: A => Unit) {
    addListener { () => f(self()) }
  }

  /** Project promised value into an either containing the value or any
   *  exception thrown retrieving it. Unwraps `cause` of any top-level
   *  ExecutionException */
  def either =
    new PromiseEither[Throwable, A] with SelfPromise[Either[Throwable, A]] {
      def claim = self.result.left.map { 
        case e: juc.ExecutionException => e.getCause
        case e => e
      }
    }

  /** Project promised value into an Option containing the value if retrived
   *  with no exception */
  def option: Promise[Option[A]] =
    either.map { _.right.toOption }

  /** Some value if promise is complete, otherwise None */
  def completeOption = 
    if (isComplete) Some(self())
    else None

  override def toString =
    "Promise(%s)".format(completeOption.getOrElse("-incomplete-"))
}

object Promise {
  def all[A](promises: Iterable[Promise[A]]) =
    new Promise[Iterable[A]] { self =>
      def claim = promises.map { _() }
      def isComplete = promises.forall { _.isComplete }
      lazy val timeout = promises.map { _.timeout }.max
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

  def of[T](existing: T) =
    new Promise[T] { self =>
      def claim = existing
      def isComplete = true
      val timeout: Duration = Duration.Zero
      def addListener(f: () => Unit) = f()
    }

  def of[A,B](existing: Either[A,B]) =
    new PromiseEither[A,B] { self =>
      def claim = existing
      def isComplete = true
      val timeout: Duration = Duration.Zero
      def addListener(f: () => Unit) = f()
    }

  implicit def iterable[T] = new IterableGuarantor[T]
  implicit def identity[T] = new IdentityGuarantor[T]
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
  val underlying: ListenableFuture[A],
  val executor: juc.Executor,
  val timeout: Duration
) extends Promise[A] {
  def claim = timeout match {
    case Duration.Zero => underlying.get
    case Duration(duration, unit) => underlying.get(duration, unit)
  }
  def isComplete = underlying.isDone
  def addListener(f: () => Unit) =
    underlying.addListener(new Runnable {
      def run {
        f()
      }
    }, executor)
}

trait PromiseEither[+A,+B] extends Promise[Either[A, B]] { self =>
  def left = new {
    def flatMap[BB >: B,X](f: A => PromiseEither[X,BB]) =
      new PromiseEither[X,BB] with SelfPromise[Either[X,BB]] {
        def claim = self().left.flatMap { a => f(a)() }
      }
    def map[X](f: A => X) =
      new PromiseEither[X,B] with SelfPromise[Either[X,B]] {
        def claim = self().left.map(f)
      }
    def foreach(f: A => Unit) {
      addListener { () => self().left.foreach(f) }
    }
  }
  def right = new {
    def flatMap[AA >: A,Y](f: B => PromiseEither[AA,Y]) =
      new PromiseEither[AA,Y] with SelfPromise[Either[AA,Y]] {
        def claim = self().right.flatMap { b => f(b)() }
      }
    def map[Y](f: B => Y) =
      new PromiseEither[A,Y] with SelfPromise[Either[A,Y]] {
        def claim = self().right.map(f)
      }
    def foreach(f: B => Unit) {
      addListener { () => self().right.foreach(f) }
    }
  }
}

trait Guarantor[-A, B, That <: Promise[B]] {
  def promise(collateral: A): That
}

class IterableGuarantor[T] extends Guarantor[
  Iterable[Promise[T]],
  Iterable[T],
  Promise[Iterable[T]]
] {
  def promise(collateral: Iterable[Promise[T]]) =
    Promise.all(collateral)
}

class IdentityGuarantor[T] extends Guarantor[Promise[T],T,Promise[T]] {
  def promise(collateral: Promise[T]) = collateral
}

case class Duration(length: Long, unit: TimeUnit) {
  def millis = unit.toMillis(length)
}

object Duration {
  val Zero = Duration(-1L, TimeUnit.MILLISECONDS)
  def millis(length: Long) = Duration(length, TimeUnit.MILLISECONDS)
}
