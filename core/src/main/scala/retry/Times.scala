package dispatch.retry

import dispatch._

object Directly {
  def apply[T](count: Int)(promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    promise.flatMap { res =>
      if (count < 1 || success.predicate(res)) promise
      else Directly(count - 1)(promise.replay)
    }
  }
}

object Pause {
  def apply[T](count: Int, delay: Duration)(promise: Promise[T])
              (implicit success: Success[T]): Promise[T] = {
    promise.flatMap { res =>
      if (count < 1 || success.predicate(res)) promise
      else promise.http.promise.sleep(delay) {
        Pause(count - 1, delay)(promise.replay)
      }.flatten
    }
  }
}

class Success[T](val predicate: T => Boolean)

object Success {
  implicit def either[A,B]: Success[Either[A,B]] =
    new Success(_.isRight)
}
