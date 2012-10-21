package dispatch.retry

import dispatch._

object Times {
  def apply[T](count: Int)(promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    promise.flatMap { res =>
      if (count < 1 || success.predicate(res)) promise
      else Times(count - 1)(promise.replay)
    }
  }
}

class Success[T](val predicate: T => Boolean)

object Success {
  implicit def either[A,B]: Success[Either[A,B]] =
    new Success(_.isRight)
}
