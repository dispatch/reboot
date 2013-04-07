package dispatch

import com.ning.http.client.{
  AsyncHttpClient, RequestBuilder, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig

import org.jboss.netty.channel.socket.nio.{
  NioClientSocketChannelFactory, NioWorkerPool}
import org.jboss.netty.util.{Timer,HashedWheelTimer}

import java.util.{concurrent => juc}
import scala.concurrent.{Future,ExecutionContext}

/** Http executor with defaults */
case class Http(
  client: AsyncHttpClient = InternalDefaults.client
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
  InternalDefaults.client
)

private [dispatch] object InternalDefaults {
  lazy val client = new AsyncHttpClient(config)
  lazy val config = new AsyncHttpClientConfig.Builder()
    .setUserAgent("Dispatch/%s" format BuildInfo.version)
    .setAsyncHttpClientProviderConfig(
      new NettyAsyncHttpProviderConfig().addProperty(
        NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY,
        nioClientSocketChannelFactory
      )
    ).setRequestTimeoutInMs(-1) // don't timeout streaming connections
    .build
  lazy val nioClientSocketChannelFactory = {
    val workerCount = 2 * Runtime.getRuntime().availableProcessors()
    new NioClientSocketChannelFactory(
      juc.Executors.newCachedThreadPool(DaemonThreads.factory),
      1,
      new NioWorkerPool(
        juc.Executors.newCachedThreadPool(DaemonThreads.factory),
        workerCount
      ),
      timer
    )
  }
  lazy val timer = new HashedWheelTimer(DaemonThreads.factory)
}

trait HttpExecutor { self =>
  def client: AsyncHttpClient

  object promise {
    @deprecated("use scala.concurrent.Future.successful", "0.10.0")
    def apply[T](f: => T) = Future.successful(f)
    @deprecated("use scala.concurrent.Future.sequence", "0.10.0")
    def all[T](seq: Iterable[Future[T]])
              (implicit executor: ExecutionContext) = 
      Future.sequence(seq)
  }

  def apply(builder: RequestBuilder)
           (implicit executor: ExecutionContext): Future[Response] =
    apply(builder.build() -> new FunctionHandler(identity))

  def apply[T](pair: (Request, AsyncHandler[T]))
              (implicit executor: ExecutionContext): Future[T] =
    apply(pair._1, pair._2)

  def apply[T]
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
