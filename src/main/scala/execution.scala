package dispatch

import com.ning.http.client.{
  AsyncHttpClient, Request, AsyncHandler
}
import java.util.{concurrent => juc}

// "AsyncHandler aren't thread safe, hence you should avoid re-using the
// same instance when doing concurrent requests."
case class Executable[T](request: Request, handler: () => AsyncHandler[T])

class Http extends Executor {
  lazy val executor = juc.Executors.newCachedThreadPool
  lazy val client = new AsyncHttpClient
}

object Http extends Http

trait Executor {
  def client: AsyncHttpClient
  implicit def executor: juc.ExecutorService
  def apply[T](exec: Executable[T]) =
    Promise.make(client.executeRequest(exec.request, exec.handler()))
  def shutdown() {
    client.close()
    executor.shutdown()
  }
}
