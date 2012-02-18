package dispatch.spec

import org.scalacheck._

object ResponseSpecification
extends Properties("Response")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll

  import unfiltered.netty
  import unfiltered.response._

  val server = netty.Http.anylocal.handler(netty.cycle.Planify {
    case _ => PlainTextContent ~> ResponseString("hello")
  }).start()

  property("equals") = forAll(Gen.value("hello")) { (sample: String) =>
    import dispatch._
    val res = dispatch.Http(host("127.0.0.1", server.port) > As.string)()
    res == sample
  }
}
