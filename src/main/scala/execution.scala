package dispatch

import com.ning.http.client.{
  AsyncHttpClient, Request, AsyncHandler
}

case class Executable[T](request: Request, handler: AsyncHandler[T])

object Http extends Executor {
  lazy val client = new AsyncHttpClient
}

trait Executor {
  def client: AsyncHttpClient
  def apply[T](exec: Executable[T]) =
    client.executeRequest(exec.request, exec.handler)
  def shutdown() { client.close() }
}
