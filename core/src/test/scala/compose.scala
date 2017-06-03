package dispatch.spec

import org.scalacheck._

object ComposeSpecification
extends Properties("Compose")
with DispatchCleanup {
  import Prop.{forAll,AnyOperators}
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

  val localhost = host("127.0.0.1", server.port)

  def sum(nums: Iterable[String]) =
    Http.default(localhost / "sum" << nums.map { "num" -> _ } > as.String)

  val numList = nonEmptyListOf(chooseNum(-1000L, 1000L))

  property("sum in one request") = forAll(numList) { (sample: List[Long]) =>
    sum(sample.map { _.toString })() ?= sample.sum.toString
  }

  property("sum in fold") = forAll(numList) { (sample: List[Long]) =>
    val res = (Future.successful("0") /: sample) { (p, num) =>
      p.flatMap { cur => sum(Seq(cur, num.toString)) }
    }
    res() ?= sample.sum.toString
  }

  property("recursive sum") = forAll(numList) { (sample: List[Long]) =>
    @annotation.tailrec
    def recur(nums: Iterable[Future[String]]): Future[String] = {
      if (nums.size == 1) nums.head
      else recur(
        nums.grouped(2).map { twos =>
          if (twos.size == 1) twos.head
          else Future.sequence(twos).flatMap(sum)
        }.toIterable
      )
    }
    recur(
      sample.map { i => Future.successful(i.toString) }
    )() ?= sample.sum.toString
  }
}
