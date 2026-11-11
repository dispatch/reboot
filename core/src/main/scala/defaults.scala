package dispatch

import io.netty.util.{HashedWheelTimer, Timer}
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder
import org.asynchttpclient._

import scala.concurrent.ExecutionContext

object Defaults {
  implicit def executor: ExecutionContext = ExecutionContext.Implicits.global

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

  /**
   * Sets a user agent and keeps AHC's default request timeout. Streaming
   * clients that need longer-lived requests can raise or disable the timeout
   * with `Http.withConfiguration` or per request.
   */
  private object BasicDefaults extends Defaults {
    lazy val timer: Timer = new HashedWheelTimer()

    private val userAgent = "Dispatch/%s" format BuildInfo.version
    def builder: Builder = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent(userAgent)
      .setUseProxyProperties(true)
  }

}
