package dispatch.spec

import org.scalacheck._

object BasicSpecification
extends Properties("Basic")
with DispatchCleanup {
  import java.net.{URLEncoder,URLDecoder}
  import Prop.{forAll,AnyOperators}

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case req @ Path("/echo") & Params(Echo(echo)) =>
        PlainTextContent ~> ResponseString(req.method + echo)
      case req @ Path(Seg("echopath" :: echo :: _)) =>
        PlainTextContent ~> ResponseString(req.method + URLDecoder.decode(echo, "utf-8"))
      case req @ Path(Seg("echopath" :: Nil)) =>
        PlainTextContent ~> ResponseString(req.method)
      case req @ Path(Seg("echobody" :: Nil)) =>
        PlainTextContent ~> ResponseString(req.method + Body.string(req))
      case Path(Seg("agent" :: Nil)) & UserAgent(agent) =>
        PlainTextContent ~> ResponseString(agent)
    }).start()
  }

  import dispatch._

  val localhost = host("127.0.0.1", server.port)

  // a shim until we can update scalacheck to a version that non-alpha strings that don't break Java
  val syms = "&#$@%"

  def cyrillicChars = Gen.choose( 0x0400, 0x04FF) map {_.toChar}

  def cyrillic = for {
    cs <- Gen.listOf(cyrillicChars)
  } yield {
    cs.mkString
  }

  property("url() should encode non-ascii chars in the path") = forAll(cyrillic) { (sample: String) =>
    val path = if (sample.isEmpty) "" else "/" + sample
    val wiki = "http://wikipedia.com" + path
    val uri = url(wiki)
    uri.toRequest.getUrl() ?= RawUri(wiki).toString
  }

  property("POST and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" << Map("echo" -> sample) > as.String
    )
    res() ?= ("POST" + sample)
  }

  property("POST non-ascii chars body and get response") = forAll(cyrillic) { (sample: String) =>
    val res = Http(
      localhost / "echobody" << sample > as.String
    )
    res() ?= ("POST" + sample)
  }

  property("GET and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() ?= ("GET" + sample)
  }

  property("GET and get response") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "echo" <<? Map("echo" -> sample)
    )
    res().getResponseBody ?= ("GET" + sample)
  }

  property("GET with encoded path") = forAll(Gen.alphaStr) { (sample: String) =>
    // (second sample in request path is ignored)
    val res = Http(
      localhost / "echopath" / (sample + syms) / sample OK as.String
    )
    ("GET" + sample + syms) ?= res()
  }

  property("GET with encoded path as url") = forAll(Gen.alphaStr) { (sample: String) =>
    val requesturl = "http://127.0.0.1:%d/echopath/%s".format(server.port, URLEncoder.encode(sample + syms, "utf-8"))
    val res = Http(url(requesturl) / sample OK as.String)
    res() == ("GET" + sample + syms)
  }

  property("OPTIONS and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost.OPTIONS / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() ?= ("OPTIONS" + sample)
  }

  property("Send Dispatch/%s User-Agent" format BuildInfo.version) = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost / "agent" > as.String
    )
    res() ?= ("Dispatch/%s" format BuildInfo.version)
  }

}
