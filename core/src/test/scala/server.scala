package dispatch.spec

import org.scalacheck._

object ServerSpecification
extends Properties("Server")
with unfiltered.spec.ServerCleanup {
  import Prop._

  import dispatch._

  val server = { 
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    object What extends Params.Extract("what", Params.first)
    netty.Http.anylocal.handler(netty.async.Planify {
      unfiltered.kit.AsyncCycle.perfect {
        case req @ Path("/echo") & Params(Echo(echo)) =>
          Promise(PlainTextContent ~> ResponseString(echo))
        case req @ Path("/ask") & Params(Echo(echo) & What(what)) =>
          for {
            e <- Http(
              localhost / what << Map("echo" -> echo) OK as.String
            ).either
          } yield {
            e.fold(
              _ => InternalServerError ~> ResponseString("service error"),
              str => PlainTextContent ~> ResponseString(str)
            )
          }
      }
    }).start()
  }

  def localhost: Req = host("127.0.0.1", server.port)

  property("Server receieves same answer from itself") =
    forAll(Gen.alphaStr) { sample =>
      val res = Http(
        localhost / "ask" << Map("what" -> "echo",
                                 "echo" -> sample) > as.String
      )
      res() ?= sample
    }
  property("Backend failure produces error response") =
    forAll(Gen.alphaStr.suchThat { _ != "echo"}, Gen.alphaStr) {
      (sample1, sample2) =>
        val res = Http(
          localhost / "ask" << Map("what" -> sample1,
                                   "echo" -> sample2) OK as.String
        ).either
        res() ?= Left(StatusCode(500))
    }
}
