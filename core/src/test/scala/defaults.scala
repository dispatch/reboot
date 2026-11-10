package dispatch.spec

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.scalacheck._

object DefaultsSpecification
extends Properties("Defaults") {
  import Prop._

  import dispatch._

  private val ahcRequestTimeout =
    new DefaultAsyncHttpClientConfig.Builder().build.getRequestTimeout

  property("AHC's default request timeout is finite") =
    Prop(ahcRequestTimeout.toMillis > 0) :| s"AHC request timeout: $ahcRequestTimeout"

  property("Default client keeps AHC's request timeout") =
    Http.defaultClientBuilder.build.getRequestTimeout ?= ahcRequestTimeout

  property("Configured clients inherit the request timeout") =
    Http.withConfiguration(identity).clientBuilder.build.getRequestTimeout ?= ahcRequestTimeout
}
