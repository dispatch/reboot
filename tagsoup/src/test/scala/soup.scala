package dispatch.spec

import org.scalacheck._

object TagsoupSpecification
extends Properties("Basic")
with DispatchCleanup {
  import Prop.forAll

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Path("/echo") & Params(Echo(echo)) =>
        Html(<html><head></head><body><div id="echo">{echo}</div></body></html>)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("handle documents") = forAll(Gen.alphaStr) { (sample: String) =>
    val doc = Http.default(
      localhost / "echo" <<? Map("echo" -> sample) > as.tagsoup.NodeSeq
    )
    (doc() \\ "div").find (n => (n \ "@id").text == "echo").map (_.text) .mkString == (sample)
  }

}
