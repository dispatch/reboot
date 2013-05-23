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
import scala.concurrent.{ExecutionContext}

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
  /** true if we think we're runing un-forked in an sbt-interactive session */
  val inTrapExit = (
    for (group <- Option(Thread.currentThread.getThreadGroup))
    yield group.getName == "trap.exit"
  ).getOrElse(false)

  private lazy val underlying = 
    if (inTrapExit) SbtProcessDefaults
    else BasicDefaults

  lazy val client = new AsyncHttpClient(underlying.builder.build())
  lazy val timer = underlying.timer

  private trait Defaults {
    def builder: AsyncHttpClientConfig.Builder
    def timer: Timer
  }

  /** Sets a user agent, no timeout for requests  */
  private object BasicDefaults extends Defaults {
    lazy val timer = new HashedWheelTimer()
    lazy val builder = new AsyncHttpClientConfig.Builder()
      .setUserAgent("Dispatch/%s" format BuildInfo.version)
      .setRequestTimeoutInMs(-1) // don't timeout streaming connections
  }

  /** Uses daemon threads and tries to exit cleanly when running in sbt  */
  private object SbtProcessDefaults extends Defaults {
    lazy val builder =
      BasicDefaults.builder.setAsyncHttpClientProviderConfig(
        new NettyAsyncHttpProviderConfig().addProperty(
          NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY,
          nioClientSocketChannelFactory
        )
      )

    lazy val timer = new HashedWheelTimer(daemonThreadFactory)

    lazy val nioClientSocketChannelFactory = {
      val workerCount = 2 * Runtime.getRuntime().availableProcessors()
      new NioClientSocketChannelFactory(
        juc.Executors.newCachedThreadPool(daemonThreadFactory),
        1,
        new NioWorkerPool(
          juc.Executors.newCachedThreadPool(daemonThreadFactory),
          workerCount
        ),
        timer
      )
    }

    private val shuttingDown = new juc.atomic.AtomicBoolean(false)
    def shutdown() {
      if (shuttingDown.compareAndSet(false, true)) {
        nioClientSocketChannelFactory.releaseExternalResources()
      }
    }
    /** produces daemon threads that shut down everything when interrupted! */
    val daemonThreadFactory = new juc.ThreadFactory {
      def newThread(runnable: Runnable): Thread ={
        val thread = new Thread(runnable) {
          override def interrupt() {
            SbtProcessDefaults.shutdown()
            super.interrupt()
          }
        }
        thread.setDaemon(true)
        thread
      }
    }
  }
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

