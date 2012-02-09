package dispatch.specs

import org.scalacheck._

object ResponseSpecification extends Properties("Response") {
  import Prop.forAll

  property("equals") = forAll(Gen.alphaStr) { (sample: String) =>
    import unfiltered.netty
    import unfiltered.response._
    import dispatch._
    val server = netty.Http.anylocal.handler(netty.cycle.Planify {
      case _ => PlainTextContent ~> ResponseString(sample)
    }).start()
    val res = dispatch.Http(host("127.0.0.1", server.port) > As.string)()
    server.stop()
    res == sample
  }
}
