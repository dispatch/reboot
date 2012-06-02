package dispatch.spec

import org.scalacheck._

object HeadersSpecification
extends Properties("Headers")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object EchoIn extends StringHeader("echo")
    object EchoOut extends HeaderName("echo")
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case req @ EchoIn(echo) => EchoOut(echo)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("Set and retrieve") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost <:< Map("echo" -> sample) > As.headers
    )
    res()("echo").contains(sample)
  }
}
