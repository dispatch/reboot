package dispatch.immutable.spec

import dispatch.as
import org.scalacheck._
import dispatch.BuildInfo
import dispatch.immutable._

object OperationsSpecification extends Properties("Operations") {
  import Prop._

  def baseUrl = :/ ("localhost")

  property("Adding url paths") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getUrl(baseUrl / sample)
    res ?= ("http://localhost/%s".format(sample))
  }

  property("Removing url paths") = forAll(Gen.alphaStr) { (sample: String) =>
    val res0 = getUrl((baseUrl / sample / "path").removePath("path"))
    val res1 = getUrl((baseUrl / "path" / sample).removePath("path"))
    val res2 = getUrl((baseUrl / "path" / sample / "path2").removePath(sample))

    res0 ?= ("http://localhost/%s".format(sample))
    res1 ?= ("http://localhost/%s".format(sample))
    //res2 ?= ("http://localhost/path/path2")
  }

  property("Setting a url to secure") = forAll(Gen.alphaStr) { (sample: String) =>
    val req = baseUrl / "path" secure
    val res = getUrl(req)

    req.isSecure ?= true
    res ?= "https://localhost/path"
  }

  private[this] def getUrl(req: HttpRequest): String = req.request.build.getUrl()
}
