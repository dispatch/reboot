package dispatch.spec

import java.nio.charset.Charset

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
      case req @ Path(Seg("echoquery" :: Nil)) & QueryParams(queryParams) =>
        val params = queryParams.flatMap { case (k, vs) => vs.map(v => k + "=" + v) }.mkString("&")
        PlainTextContent ~> ResponseString(req.method + params)
      case Path(Seg("agent" :: Nil)) & UserAgent(agent) =>
        PlainTextContent ~> ResponseString(agent)
      case Path(Seg("contenttype" :: Nil)) & RequestContentType(contenttype) =>
        PlainTextContent ~> ResponseString(contenttype)
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

  property("Path segments can be before and after query parameters") = forAll(Gen.alphaStr) { (sample: String) =>
    val segmentLast = (localhost <<? Map("key" -> "value")) / sample
    val segmentFirst = localhost / sample <<? Map("key" -> "value")
    segmentLast.toRequest.getUrl() ?= segmentFirst.toRequest.getUrl()
  }

  property("Path segments can be optional") = forAll(Gen.alphaStr) { (sample: String) =>
    val segmentLast = (localhost <<? Map("key" -> "value")) / sample
    val segmentOptional = localhost /? Some(sample) /? None <<? Map("key" -> "value")
    segmentLast.toRequest.getUrl ?= segmentOptional.toRequest.getUrl
  }

  property("POST and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http.default(
      localhost / "echo" << Map("echo" -> sample) > as.String
    )
    res() ?= ("POST" + sample)
  }

  property("POST json with query params") = forAll(Gen.alphaStr) { (value: String) =>
    val headers = Map("Content-Type" -> "application/json")
    val params = Map("key" -> value)
    val body = """{"foo":"bar"}"""
    val res = Http.default(
      localhost / "echoquery" <:< headers <<? params << body OK  as.String
    )
    res() ?= ("POST" + "key=" + value)
  }

  property("POST non-ascii chars body and get response") = forAll(cyrillic) { (sample: String) =>
    val res = Http.default(
      localhost / "echobody" << sample > as.String
    )
    res() ?= ("POST" + sample)
  }

  property("GET and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http.default(
      localhost / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() ?= ("GET" + sample)
  }

  property("GET and get response") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http.default(
      localhost / "echo" <<? Map("echo" -> sample)
    )
    res().getResponseBody ?= ("GET" + sample)
  }

  property("GET with encoded path") = forAll(Gen.alphaStr) { (sample: String) =>
    // (second sample in request path is ignored)
    val res = Http.default(
      localhost / "echopath" / (sample + syms) / sample OK as.String
    )
    ("GET" + sample + syms) ?= res()
  }

  property("GET with encoded path as url") = forAll(Gen.alphaStr) { (sample: String) =>
    val requesturl = "http://127.0.0.1:%d/echopath/%s".format(server.port, URLEncoder.encode(sample + syms, "utf-8"))
    val res = Http.default(url(requesturl) / sample OK as.String)
    res() == ("GET" + sample + syms)
  }

  property("OPTIONS and handle") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http.default(
      localhost.OPTIONS / "echo" <<? Map("echo" -> sample) > as.String
    )
    res() ?= ("OPTIONS" + sample)
  }

  property("Send Dispatch/%s User-Agent" format BuildInfo.version) = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http.default(
      localhost / "agent" > as.String
    )
    res() ?= ("Dispatch/%s" format BuildInfo.version)
  }

  property("Send a default content-type with <<") = forAll(Gen.const("unused")) { (sample: String) =>
    val res = Http.default(
      localhost / "contenttype" << "request body" > as.String
    )
    res() ?= ("text/plain; charset=UTF-8")
  }

  property("Send a custom content type after <<") = forAll(Gen.oneOf("application/json", "application/foo")) { (sample: String) =>
    val res = Http.default(
      (localhost / "contenttype" << "request body").setContentType(sample, Charset.forName("UTF-8")) > as.String
    )
    res() ?= (sample + "; charset=UTF-8")
  }

  property("Send a custom content type with <:< after <<") = forAll(Gen.oneOf("application/json", "application/foo")) { (sample: String) =>
    val res: Future[String] = Http.default(
      localhost / "contenttype" << "request body" <:< Map("Content-Type" -> sample) > as.String
    )
    res() ?= (sample)
  }

  property("Set query params with <<? after setBody(String) and setContentType") = {
    forAll(Gen.mapOf(Gen.zip(
      Gen.alphaStr.suchThat(_.nonEmpty),
      Gen.alphaStr
    )).suchThat(_.nonEmpty)) { (sample : Map[String, String]) =>
      val expectedParams = sample.map { case (key, value) => "%s=%s".format(key, value) }
      val req = localhost.setBody("").setContentType("text/plain", Charset.forName("UTF-8")) <<? sample
      req.toRequest.getUrl ?= "http://127.0.0.1:%d/?%s".format(server.port, expectedParams.mkString("&"))
    }
  }
}
