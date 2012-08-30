package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}
import java.util.{concurrent => juc}

object Http extends Http

/** Defaults to no timeout value and a fixed thread pool */
class Http extends ConfiguredExecutor {
  val timeout = Duration.Zero
  val configure = new AsyncHttpClientConfig.Builder()
    .setExecutorService(juc.Executors.newFixedThreadPool(256))
}

trait ConfiguredExecutor extends Executor {
  lazy val client = new AsyncHttpClient(configure.build)
  def configure: AsyncHttpClientConfig.Builder
}

trait Executor { self =>
  /** Convenience method for an Executor with the given timeout */
  def waiting(t: Duration) = new Executor {
    def client = self.client
    def timeout = t
  }

  def client: AsyncHttpClient
  /** Timeout for promises made by this HTTP Executor */
  def timeout: Duration

  def apply(builder: RequestBuilder): Promise[Response] =
    apply(builder.build() -> new FunctionHandler(identity))

  def apply[T](pair: (Request, AsyncHandler[T])): Promise[T] =
    apply(pair._1, pair._2)

  def apply[T](request: Request, handler: AsyncHandler[T]): Promise[T] =
    new ListenableFuturePromise(
      client.executeRequest(request, handler),
      client.getConfig.executorService,
      timeout
    )

  def shutdown() {
    client.close()
  }
}
