package dispatch.spec

import org.scalacheck._

object IterableGuarantorSpecification
extends Properties("Iterable Guarantor")
with unfiltered.spec.ServerCleanup {
  import Prop.forAll
  import Gen._

  val server = { 
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Str extends Params.Extract("str", Params.first)
    object Chr extends Params.Extract("chr", Params.first ~> {
      _.flatMap{ _.toList.headOption } } )
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Path("/split") & Params(Str(str)) =>
        PlainTextContent ~> ResponseString(str.mkString("",",",","))
      case Path("/value") & Params(Chr(chr)) =>
        PlainTextContent ~> ResponseString(chr.toInt.toString)
    }).start()
  }

  import dispatch._

  def localhost = host("127.0.0.1", server.port)

  def split(str: String): Promise[Seq[String]] =
    for (csv <- Http(localhost / "split" << Seq("str" -> str) > As.string))
      yield csv.split(",")

  def value(str: String): Promise[Int] =
    for (v <- Http(localhost / "value" << Seq("chr" -> str) > As.string))
      yield v.toInt

  property("iterable promise guarantor") = forAll(Gen.alphaStr) {
  (sample: String) =>
    val values: Promise[Iterable[Int]] = for {
      chrs <- split(sample)
      chr <- chrs
    } yield value(chr)
    values() == sample.map { _.toInt }
  }
}
