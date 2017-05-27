package dispatch.spec

import org.scalacheck._

object ServerSpecification
extends Properties("Server")
with DispatchCleanup {
  import Prop.{forAll,AnyOperators}

  import dispatch._

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    object What extends Params.Extract("what", Params.first)
    object EchoHeader extends StringHeader("echo")
    netty.Http.anylocal.handler(netty.async.Planify {
      case req @ Path("/echo") & Params(Echo(echo)) =>
        req.respond(PlainTextContent ~> ResponseString(echo))
      case req @ Path("/ask") & Params(Echo(echo) & What(what)) =>
        for {
          e <- Http.default(
            localhost / what << Map("echo" -> echo) OK as.String
          ).either
        } {
          req.respond(e.fold(
            _ => InternalServerError ~> ResponseString("service error"),
            str => PlainTextContent ~> ResponseString(str)
          ))
        }
      case req @ Path("/ask") & Params(What(what)) & EchoHeader(echo) =>
        for {
          e <- Http.default(
            localhost / what << Map("echo" -> echo) OK as.String
          ).either
        } {
          req.respond(e.fold(
            _ => InternalServerError ~> ResponseString("service error"),
            str => PlainTextContent ~> ResponseString(str)
          ))
        }
    }).start()
  }

  lazy val localhost: Req = host("127.0.0.1", server.port)

  property("Server receieves same answer (from param) from itself") =
    forAll(Gen.alphaStr) { sample =>
      val res: Future[String] = Http.default(
        localhost / "ask" << Map("what" -> "echo",
                                 "echo" -> sample) > as.String
      )
      res() ?= sample
    }

  property("Server receives same answer (from header) from itself") =
    forAll(Gen.alphaStr) { sample =>
      val res = Http.default(
        (localhost / "ask") << Map("what" -> "echo") <:< Map("echo" -> sample) > as.String
      )
      res() ?= sample
    }

  property("Backend failure produces error response") =
    forAll(Gen.alphaStr.suchThat { _ != "echo"}, Gen.alphaStr) {
      (sample1, sample2) =>
        val res = Http.default(
          localhost / "ask" << Map("what" -> sample1,
                                   "echo" -> sample2) OK as.String
        ).either
        res() ?= Left(StatusCode(500))
    }
}
