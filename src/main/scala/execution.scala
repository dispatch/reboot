package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler
}
import java.util.{concurrent => juc}

class Http extends Executor {
  lazy val executor = juc.Executors.newCachedThreadPool
  lazy val client = new AsyncHttpClient
}

object Http extends Http

trait Executor {
  def client: AsyncHttpClient
  implicit def executor: juc.ExecutorService

  def apply[T](pair: (Request, AsyncHandler[T])): Promise[T] =
    apply(pair._1, pair._2)

  def apply[T](request: Request, handler: AsyncHandler[T]): Promise[T] =
    Promise.make(client.executeRequest(request, handler))

  def shutdown() {
    client.close()
    executor.shutdown()
  }
}
