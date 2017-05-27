package dispatch.spec

import org.scalacheck._
import org.jsoup.safety.Whitelist

object JsoupSpecification
extends Properties("Basic")
with DispatchCleanup {
  import Prop.forAll

  val UnsafeFormat =
    "<html><head></head><body><p><a href='http://example.com/' onclick='stealCookies()'>%s</a></p></body></html>"

  val SafeFormat =
    """<p><a href="http://example.com/" rel="nofollow">%s</a></p>"""

  val PageWithRelativeLink =
    """<html><head></head><body><p><a href="/category">%s</a></p></body></html>"""

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Path("/echo") & Params(Echo(echo)) =>
        Html(<html><head></head><body><div id="echo">{echo}</div></body></html>)
      case Path("/unclean") & Params(Echo(echo)) =>
        HtmlContent ~> ResponseString(UnsafeFormat format echo)
      case Path("/relative") & Params(Echo(echo)) =>
        HtmlContent ~> ResponseString(PageWithRelativeLink format echo)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("handle Documents") = forAll(Gen.alphaStr) { (sample: String) =>
    val doc = Http.default(
      localhost / "echo" <<? Map("echo" -> sample) > as.jsoup.Document
    )
    doc().select("div#echo").first().text() == sample
  }

  property("handle Queries") = forAll(Gen.alphaStr) { (sample: String) =>
    val els = Http.default(
      localhost / "echo" <<? Map("echo" -> sample) > as.jsoup.Query("div")
    )
    els().first().text() == sample
  }

  property("handle Cleaning") = forAll(Gen.alphaStr) { (sample: String) =>
    val clean = Http.default(
      localhost / "unclean" <<? Map("echo" -> sample) > as.jsoup.Clean(
        Whitelist.basic)
    )
    clean() == (SafeFormat format sample)
  }

  property("handle absolute urls on page") = forAll(Gen.alphaStr) { (sample: String) =>
    val doc = Http.default(
      localhost / "relative" <<? Map("echo" -> sample) > as.jsoup.Document
    )
    doc().select("a").first().absUrl("href") == (localhost.url + "category")
  }
}
