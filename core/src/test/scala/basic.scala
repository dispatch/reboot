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
      localhost / "echo" << Map("echo" -> sample) > as.String
    )
    res() == ("POST" + sample)
  }

  property("GET and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() == ("GET" + sample)
  }

  property("GET and get response") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample)
    )
    res().getResponseBody == ("GET" + sample)
  }

  property("OPTIONS and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost.OPTIONS / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() == ("OPTIONS" + sample)
  }
}
