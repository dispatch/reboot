package dispatch.retry

import java.util.concurrent.TimeUnit

import dispatch._

/** Retry immediately after failure */
object Directly extends CountingRetry {
  def apply[T](max: Int = 3)(promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    retry(max, promise, success, Directly(_)(promise.replay))
  }
}

/** Retry with a pause between attempts */
object Pause extends CountingRetry {
  def apply[T](max: Int = 4,
               delay: Duration = Duration(500, TimeUnit.MILLISECONDS))
              (promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    retry(max,
          promise,
          success,
          c => promise.http.promise.sleep(delay) {
            Pause(c, delay)(promise.replay)
          }.flatten)
  }
}

/** Retry with exponential backoff */
object Backoff extends CountingRetry {
  def apply[T](max: Int = 8,
               delay: Duration = Duration(500, TimeUnit.MILLISECONDS),
               base: Int = 2)
              (promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    retry(max,
          promise,
          success,
          count => promise.http.promise.sleep(delay) {
            Backoff(count, 
                    Duration(delay.length * base, delay.unit),
                    base)(promise.replay)
          }.flatten)
  }
}

class Success[T](val predicate: T => Boolean)

object Success {
  implicit def either[A,B]: Success[Either[A,B]] =
    new Success(_.isRight)
  implicit def option[A]: Success[Option[A]] =
    new Success(!_.isEmpty)
}

trait CountingRetry {
  protected def retry[T](max: Int,
                         promise: Promise[T],
                         success: Success[T],
                         orElse: Int => Promise[T]
                       ) =
    promise.flatMap { res =>
      if (max < 1 || success.predicate(res)) promise
      else orElse(max - 1)
    }
}
