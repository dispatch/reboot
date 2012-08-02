package dispatch.spec

import org.scalacheck._

object BasicSpecification
extends Properties("Lift Json")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll

  import net.liftweb.json._
  import JsonDSL._

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._

    object In extends Params.Extract("in", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Params(In(in)) => Json(("out" -> in))
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  property("parse json") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = Http(
      localhost <:< Map("Accept" -> "application/json") <<? Map("in" -> sample) > as.lift.Json
    )
    sample == (for { JObject(fields) <- res(); JField("out", JString(o)) <- fields } yield o).head
  }
}
