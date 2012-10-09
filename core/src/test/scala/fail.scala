package dispatch.spec

import org.scalacheck._

object FailureSpecification
extends Properties("Failure Handling")
with DispatchCleanup {
  import Prop._
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
    val res = Http(localhost / sample OK as.String).either
    res() =? Left(StatusCode(404, "Not found"))
  }

  property("project left on failure") = forAll(
    Gen.alphaStr.suchThat { _ != "foo"}
  ) { sample =>
    val res = Http(localhost / sample OK as.String).either.right.map {
      _ => "error"
    }
    res() =? Left(StatusCode(404, "Not found"))
  }

  property("project right on success2") = {
    val path = Right("foo")
    val eth = for {
      p <- Promise(path).right
      res <- Http(localhost / p OK as.String).either.right
      res2 <- Http(localhost / p OK as.String).either.right
    } yield res2.length
    eth() =? Right(3)
  }

  property("project left on failure2") = forAll(
    Gen.alphaStr.suchThat { _ != "foo"}
  ){ sample =>
    val good = Right("meh")
    val bad = Right(sample)
    val eth = for {
      g <- Promise(good).right
      b <- Promise(bad).right
      res <- Http(localhost / g OK as.String).either.right
      res2 <- Http(localhost / b OK as.String).either.right
    } yield res2
    eth() =? Left(StatusCode(404, "Not found"))
  }
}
