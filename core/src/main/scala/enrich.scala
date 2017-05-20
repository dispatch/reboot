package dispatch

import scala.concurrent.{ExecutionContext,Await,ExecutionException}
import scala.concurrent.duration.Duration

class EnrichedFuture[A](underlying: Future[A]) {

  /** Project promised value into an either containing the value or any
   *  exception thrown retrieving it. Unwraps `cause` of any top-level
   *  ExecutionException */
  def either: Future[Either[Throwable, A]] = {
    implicit val ctx = EnrichedFuture.currentThreadContext

    underlying.map { res => Right(res) }.recover {
      case exc: ExecutionException => Left(exc.getCause)
      case throwable => Left(throwable)
    }
  }
  /** Create a left projection of a contained either */
  def left[B,C](implicit ev: Future[A] <:< Future[Either[B, C]],
                executor: ExecutionContext) =
    new FutureEither.LeftProjection(underlying)

  /** Create a right projection of a contained either */
  def right[B,C](implicit ev: Future[A] <:< Future[Either[B, C]],
                 executor: ExecutionContext) =
    new FutureEither.RightProjection(underlying)

  /** Project any resulting exception or result into a unified type X */
  def fold[X](fa: Throwable => X, fb: A => X)
    (implicit executor: ExecutionContext): Future[X] =
    for (eth <- either) yield eth.fold(fa, fb)

  def flatten[B]
    (implicit pv: Future[A] <:< Future[Future[B]],
     executor: ExecutionContext): Future[B] =
    (underlying: Future[Future[B]]).flatMap(identity)

  /** Facilitates projection over promised iterables */
  def values[B](implicit ev: Future[A] <:< Future[Iterable[B]],
                executor: ExecutionContext) =
    new FutureIterable.Values(underlying)

  /** Project promised value into an Option containing the value if retrived
   *  with no exception */
  def option: Future[Option[A]] = {
    implicit val ctx = EnrichedFuture.currentThreadContext
    either.map { _.right.toOption }
  }

  def apply() = Await.result(underlying, Duration.Inf)

  /** Some value if promise is complete, otherwise None */
  def completeOption = 
    for (tried <- underlying.value) yield tried.get

  def print =
    "Future(%s)".format(either.completeOption.map {
      case Left(exc) => "!%s!".format(exc.getMessage)
      case Right(value) => value.toString
    }.getOrElse("-incomplete-"))
}

object EnrichedFuture {
  /** Execute on the current thread, for certain cpu-bound operations */
  private val currentThreadContext = new ExecutionContext {
    def execute(runnable: Runnable) = {
      runnable.run()
    }
    def reportFailure(t: Throwable) = {
      ExecutionContext.defaultReporter(t)
    }
  }
}
