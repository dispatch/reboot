package dispatch.spec

import org.scalacheck._

object BasicSpecification
extends Properties("Basic")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case req @ Path("/echo") & Params(Echo(echo)) =>
        PlainTextContent ~> ResponseString(req.method + echo)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("POST and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" << Map("echo" -> sample) > As.string
    )
    res() == ("POST" + sample)
  }

  property("GET and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample) > As.string
    )
    res() == ("GET" + sample)
  }

  property("GET and get response") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample)
    )
    res().getResponseBody == ("GET" + sample)
  }
}
