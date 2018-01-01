package dispatch

import io.netty.util.{TimerTask, Timeout, Timer}
import scala.concurrent.duration.Duration

object SleepFuture {
  def apply[T](d: Duration)(todo: => T)
              (implicit timer: Timer) = {
    val promise = scala.concurrent.Promise[T]()

    timer.newTimeout(new TimerTask {
      def run(timeout: Timeout) = {
        promise.complete(util.Try(todo))
        ()
      }
    }, d.length, d.unit)

    promise.future
  }
}
