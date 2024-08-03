package dispatch

import io.netty.util.{HashedWheelTimer, Timer}
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder
import org.asynchttpclient._

import java.time.Duration

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

    private val userAgent = "Dispatch/%s" format BuildInfo.version
    private val infinite = Duration.ofMillis(-1)
    def builder: Builder = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent(userAgent)
      .setRequestTimeout(infinite) // don't timeout streaming connections
      .setUseProxyProperties(true)
  }

}
