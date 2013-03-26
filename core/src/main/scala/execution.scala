package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import org.jboss.netty.util.{Timer,HashedWheelTimer}
import java.util.{concurrent => juc}
import scala.concurrent.{Future,ExecutionContext}

/** Http executor with defaults */
case class Http(
  client: AsyncHttpClient = Defaults.client,
  timer: Timer = Defaults.timer
) extends HttpExecutor {
  import AsyncHttpClientConfig.Builder

  /** Replaces `client` with a new instance configured using the withBuilder
      function. The current client config is the builder's prototype.  */
  def configure(withBuilder: Builder => Builder) =
    copy(client =
      new AsyncHttpClient(withBuilder(
        new AsyncHttpClientConfig.Builder(client.getConfig)
      ).build)
    )
}

/** Singleton default Http executor, can be used directly or altered
 *  with its case-class `copy` */
object Http extends Http(
  Defaults.client,
  Defaults.timer
)

private [dispatch] object Defaults {
  lazy val client = new AsyncHttpClient(config)
  lazy val config = new AsyncHttpClientConfig.Builder()
    .setUserAgent("Dispatch/%s" format BuildInfo.version)
    .setAsyncHttpClientProviderConfig(
      new NettyAsyncHttpProviderConfig().addProperty(
        NettyAsyncHttpProviderConfig.BOSS_EXECUTOR_SERVICE, bossExecutor
      )
    ).setRequestTimeoutInMs(-1) // don't timeout streaming connections
    .build
  lazy val bossExecutor =
    juc.Executors.newCachedThreadPool(DaemonThreads.factory)
  lazy val timer = new HashedWheelTimer(DaemonThreads.factory)
}

trait HttpExecutor { self =>
  def timer: Timer
  def client: AsyncHttpClient

  object promise {
    @deprecated("use scala.concurrent.Future.successful", "0.10.0")
    def apply[T](f: => T) = Future.successful(f)
    @deprecated("use scala.concurrent.Future.sequence", "0.10.0")
    def all[T](seq: Iterable[Future[T]])
              (implicit executor: ExecutionContext) = 
      Future.sequence(seq)
  }

  def apply(builder: RequestBuilder): Future[Response] =
    apply(builder.build() -> new FunctionHandler(identity))

  def apply[T](pair: (Request, AsyncHandler[T])): Future[T] =
    apply(pair._1, pair._2)

  def future[T]
    (request: Request, handler: AsyncHandler[T])
    (implicit executor: ExecutionContext): Future[T] = {
    val lfut = client.executeRequest(request, handler)
    val promise = scala.concurrent.Promise[T]()
    lfut.addListener(
      () => promise.complete(util.Try(lfut.get())),
      new juc.Executor {
        def execute(runnable: Runnable) {
          executor.execute(runnable)
        }
      }
    )
    promise.future
  }

  def shutdown() {
    client.close()
    timer.stop()
  }
}

object DaemonThreads {
  /** produces daemon threads that won't block JVM shutdown */
  val factory = new juc.ThreadFactory {
    def newThread(runnable: Runnable): Thread ={
      val thread = new Thread(runnable)
      thread.setDaemon(true)
      thread
    }
  }
  def apply(threadPoolSize: Int) =
    juc.Executors.newFixedThreadPool(threadPoolSize, factory)
}
