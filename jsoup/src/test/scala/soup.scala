package dispatch.spec

import org.scalacheck._

object JsoupSpecification
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
        Html(<html><head></head><body><div>{echo}</div></body></html>)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("handle Documents") = forAll(Gen.alphaStr) { (sample: String) =>
    val doc = Http(
      localhost / "echo" <<? Map("echo" -> sample) > as.jsoup.Document
    )  
    doc().select("div").first().text() == (sample)
  }

  property("handle Queries") = forAll(Gen.alphaStr) { (sample: String) =>
    val els = Http(
      localhost / "echo" <<? Map("echo" -> sample) > as.jsoup.Query("div")
    )
    els().first().text() == (sample)
  }
}
