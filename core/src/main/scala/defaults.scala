package dispatch

import io.netty.util.{Timer, HashedWheelTimer}
import org.asynchttpclient._

object Defaults {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val timer: Timer = InternalDefaults.timer
}

private [dispatch] object InternalDefaults {
  private lazy val underlying = BasicDefaults

  lazy val config = underlying.builder.build()
  def client = new DefaultAsyncHttpClient(config)
  lazy val timer = underlying.timer

  private trait Defaults {
    def builder: DefaultAsyncHttpClientConfig.Builder
    def timer: Timer
  }

  /** Sets a user agent, no timeout for requests  */
  private object BasicDefaults extends Defaults {
    lazy val timer = new HashedWheelTimer()
    def builder = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent("Dispatch/%s" format BuildInfo.version)
      .setRequestTimeout(-1) // don't timeout streaming connections
  }
}
