package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}
import java.util.{concurrent => juc}

object Http extends Http

/** Defaults to no timeout value and a fixed thread pool (256) for promises */
class Http extends FixedThreadPoolExecutor { self =>
  lazy val client = new AsyncHttpClient
  val timeout = Duration.Zero
  def threadPoolSize = 256

  /** Convenience method for an Executor with the given timeout */
  def waiting(t: Duration) = new Executor {
    def client = self.client
    val timeout = t
    lazy val promiseExecutor = self.promiseExecutor
  }
  /** Convenience method for an executor with a fixed thread pool of
      the given size */
  def threads(promiseThreadPoolSize: Int) = new FixedThreadPoolExecutor {
    def client = self.client
    val timeout = self.timeout
    def threadPoolSize = promiseThreadPoolSize
  }
}

// produce thread(daemon=true), so it doesn't block JVM shutdown
class DaemonThreadFactory extends juc.ThreadFactory {
  def newThread(r: Runnable):Thread ={
    val thread = new Thread
    thread.setDaemon(true) // this ensure the created threads don't prevent JVM shutdown
    thread
  }
}

trait FixedThreadPoolExecutor extends Executor {
  def threadPoolSize: Int
  lazy val promiseExecutor = juc.Executors.newFixedThreadPool(threadPoolSize,new DaemonThreadFactory)
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
