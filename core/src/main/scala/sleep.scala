package dispatch

import org.jboss.netty.util.{TimerTask, Timeout}
import scala.concurrent.{ExecutionContext,Promise}
import scala.concurrent.duration.Duration
import java.util.{concurrent => juc}

object SleepFuture {
  def apply[T](http: HttpExecutor, d: Duration)(todo: => T)
              (implicit executor: ExecutionContext) = {
    val promise = Promise[T]()

    val sleepTimeout = http.timer.newTimeout(new TimerTask {
      def run(timeout: Timeout) {
        promise.complete(util.Try(todo))
      }
    }, d.length, d.unit)

    promise.future
  }
}
