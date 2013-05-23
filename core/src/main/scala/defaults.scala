package dispatch

import scala.concurrent.duration.Duration
import org.jboss.netty.util.{Timer, HashedWheelTimer}
import org.jboss.netty.channel.socket.nio.{
  NioClientSocketChannelFactory, NioWorkerPool}
import java.util.{concurrent => juc}
import com.ning.http.client.{
  AsyncHttpClient, AsyncHttpClientConfig
}
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig

object Defaults {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val timer: Timer = InternalDefaults.timer
}

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

