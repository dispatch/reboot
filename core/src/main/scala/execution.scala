package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}
import java.util.{concurrent => juc}

// Module extending companion class cannot use default arguments. :/
object Http extends Http(new AsyncHttpClientConfig.Builder().build(), 256, Duration.Zero)

/** Defaults to no timeout value and a fixed thread pool (256) for promises */
class Http(
    config: AsyncHttpClientConfig = new AsyncHttpClientConfig.Builder().build(),
    val threadPoolSize: Int = 256,
    val timeout: Duration = Duration.Zero
    ) extends FixedThreadPoolExecutor { self =>
  lazy val client = new AsyncHttpClient(config)

  def copy(
    threadPoolSize: Int = self.threadPoolSize,
    timeout: Duration = self.timeout
    ): Http = new Http(self.config, threadPoolSize, timeout) {
      override lazy val client = self.client
  }

  def waiting(t: Duration) = copy(timeout = t)

  def threads(promiseThreadPoolSize: Int) = copy(threadPoolSize = promiseThreadPoolSize)
}

trait FixedThreadPoolExecutor extends Executor {
  val threadPoolSize: Int
  lazy val promiseExecutor = juc.Executors.newFixedThreadPool(threadPoolSize)
}

trait Executor { self =>
  def promiseExecutor: juc.Executor

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
      promiseExecutor,
      timeout
    )

  def shutdown() {
    client.close()
    promiseExecutor match {
      case service: juc.ExecutorService => service.shutdown()
      case _ => ()
    }
  }
}
