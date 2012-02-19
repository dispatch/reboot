package dispatch.spec

import org.scalacheck._

object FailureSpecification
extends Properties("Failure Handling")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll
  import Gen._

  val server = { 
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Path("/foo") =>
        PlainTextContent ~> ResponseString("bar")
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("yield a Left on not found") = forAll(
    Gen.alphaStr.suchThat { _ != "foo"}
  ) { sample =>
    val res = Http(localhost / sample > As.string).either
    res() == Left(StatusCode(404))
  }
}
