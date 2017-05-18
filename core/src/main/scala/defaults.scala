package dispatch

import io.netty.util.{HashedWheelTimer, Timer}
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder
import org.asynchttpclient._

object Defaults {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val timer: Timer = InternalDefaults.timer
}

private[dispatch] object InternalDefaults {
  private lazy val underlying = BasicDefaults

  lazy val clientBuilder: Builder = underlying.builder
  lazy val timer: Timer = underlying.timer

  private trait Defaults {
    def builder: DefaultAsyncHttpClientConfig.Builder

    def timer: Timer
  }

  /** Sets a user agent, no timeout for requests  */
  private object BasicDefaults extends Defaults {
    lazy val timer = new HashedWheelTimer()

    def builder: Builder = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent("Dispatch/%s" format BuildInfo.version)
      .setRequestTimeout(-1) // don't timeout streaming connections
      .setUseProxyProperties(true)
  }

}
