package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler
}
import java.util.{concurrent => juc}

class Http extends Executor { self =>
  lazy val executor = juc.Executors.newCachedThreadPool
  lazy val client = new AsyncHttpClient
  val timeout = Timeout.none

  /** Convenience method for an Executor with the given timeout */
  def waiting(t: Timeout) = new Executor {
    def executor = self.executor
    def client = self.client
    def timeout = t
  }
}

object Http extends Http

trait Executor {
  def client: AsyncHttpClient
  def executor: juc.ExecutorService
  /** Timeout for promises made by this HTTP Executor */
  def timeout: Timeout

  def apply[T](pair: (Request, AsyncHandler[T])): Promise[T] =
    apply(pair._1, pair._2)

  def apply[T](request: Request, handler: AsyncHandler[T]): Promise[T] =
    new ListenableFuturePromise(
      client.executeRequest(request, handler),
      executor,
      timeout
    )

  def shutdown() {
    client.close()
    executor.shutdown()
  }
}
