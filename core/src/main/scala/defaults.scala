package dispatch

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
  val inSbt = (
    for (group <- Option(Thread.currentThread.getThreadGroup))
    yield (
      group.getName == "trap.exit" // sbt version <= 0.13.0
      || group.getName.startsWith("run-main-group") // sbt 0.13.1+
    )
  ).getOrElse(false)

  private lazy val underlying = 
    if (inSbt) SbtProcessDefaults
    else BasicDefaults

  def client = new AsyncHttpClient(underlying.builder.build())
  lazy val timer = underlying.timer

  private trait Defaults {
    def builder: AsyncHttpClientConfig.Builder
    def timer: Timer
  }

  /** Sets a user agent, no timeout for requests  */
  private object BasicDefaults extends Defaults {
    lazy val timer = new HashedWheelTimer()
    def builder = new AsyncHttpClientConfig.Builder()
      .setUserAgent("Dispatch/%s" format BuildInfo.version)
      .setRequestTimeout(-1) // don't timeout streaming connections
  }

  /** Uses daemon threads and tries to exit cleanly when running in sbt  */
  private object SbtProcessDefaults extends Defaults {
    def builder = {
      val shuttingDown = new juc.atomic.AtomicBoolean(false)

      def shutdown() = {
        if (shuttingDown.compareAndSet(false, true)) {
          nioClientSocketChannelFactory.releaseExternalResources()
          timer.stop()
        }
        ()
      }
      /** daemon threads that also shut down everything when interrupted! */
      lazy val interruptThreadFactory = new juc.ThreadFactory {
        def newThread(runnable: Runnable) = {
          new Thread(runnable) {
            setDaemon(true)
            /** only reliably called on any thread if all spawned threads are daemon */
            override def interrupt() = {
              shutdown()
              super.interrupt()
            }
          }
        }
      }
      lazy val nioClientSocketChannelFactory = {
        val workerCount = 2 * Runtime.getRuntime().availableProcessors()
        new NioClientSocketChannelFactory(
          juc.Executors.newCachedThreadPool(interruptThreadFactory),
          1,
          new NioWorkerPool(
            juc.Executors.newCachedThreadPool(interruptThreadFactory),
            workerCount
          ),
          timer
        )
      }

      val config = new NettyAsyncHttpProviderConfig().addProperty(
        "socketChannelFactory",
        nioClientSocketChannelFactory
      )
      config.setNettyTimer(timer)
      BasicDefaults.builder.setAsyncHttpClientProviderConfig(config)
    }
    lazy val timer = new HashedWheelTimer(DaemonThreads.factory)
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
