package dispatch.spec

import org.scalacheck._

object ComposeSpecification
extends Properties("Compose")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll
  import Gen._

  val server = { 
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Num extends Params.Extract("num", { seq =>
      Some(seq.flatMap { str => Params.long(Some(str)) } )
    })
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case req @ Path("/sum") & Params(Num(nums)) =>
        PlainTextContent ~> ResponseString(nums.sum.toString)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  val numList = listOf(chooseNum(-1000L, 1000L))

  property("sum in one request") = forAll(numList) { (sample: List[Long]) =>
    val res = Http(
      localhost / "sum" << sample.map { "num" -> _.toString } > As.string
    )
    res() == sample.sum.toString
  }

  property("compose in sequence") = forAll(numList) { (sample: List[Long]) =>
    val res = (Promise.of("0") /: sample) { (p, num) =>
      p.flatMap { cur =>
        Http(localhost / "sum" << Seq("num" -> cur,
                                      "num" -> num.toString) > As.string)
      }
    }
    res() == sample.sum.toString
  }
}
