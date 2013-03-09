package dispatch.immutable.spec

import dispatch.as
import org.scalacheck._
import dispatch.BuildInfo
import dispatch.immutable._
import scala.collection.JavaConversions.JMapWrapper

object OperationsSpecification extends Properties("Operations") {
  import Prop._

  def baseUrl = :/ ("localhost")

  property("Adding url paths") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getUrl(baseUrl / sample)
    res ?= ("http://localhost/%s".format(sample))
  }

  property("Removing url paths") = forAll(Gen.alphaStr) { (sample: String) =>
    val middle = "middle" + sample.trim

    val res0 = getUrl((baseUrl / sample / "path").removePath("path"))
    val res1 = getUrl((baseUrl / "path" / sample).removePath("path"))
    val res2 = getUrl((baseUrl / "path" / middle / "path2").removePath(middle))

    res0 ?= ("http://localhost/%s".format(sample))
    res1 ?= ("http://localhost/%s".format(sample))
    res2 ?= "http://localhost/path/path2"
  }

  property("Adding headers") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getHeaders(baseUrl <:< Map("header1" -> sample, "header2" -> "value2"))
    res.find(_._1 == "header1") ?= Some("header1" -> sample)
    res.find(_._1 == "header2") ?= Some("header2" -> "value2")
  }

  property("Removing headers") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getHeaders((baseUrl <:< Map("header1" -> sample, "header2" -> "value2")).removeHeaders(Map("header2" -> "other")))
    res.find(_._1 == "header1") ?= Some("header1" -> sample)
    res.find(_._1 == "header2") ?= None
  }

  property("Adding Post Params") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getPostParams(baseUrl << Map("param1" -> sample, "param2" -> "value2"))
    res.find(_._1 == "param1") ?= Some("param1" -> sample)
    res.find(_._1 == "param2") ?= Some("param2" -> "value2")
  }

  property("Removing Post Params") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getPostParams((baseUrl << Map("param1" -> sample, "param2" -> "value2")).removePostParams(Map("param2" -> "other")))
    res.find(_._1 == "param1") ?= Some("param1" -> sample)
    res.find(_._1 == "param2") ?= None
  }

  property("Adding Query Params") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getQueryParams(baseUrl <<? Map("param1" -> sample, "param2" -> "value2"))
    res.find(_._1 == "param1") ?= Some("param1" -> sample)
    res.find(_._1 == "param2") ?= Some("param2" -> "value2")
  }

  property("Removing Query Params") = forAll(Gen.alphaStr) { (sample: String) =>
    val res = getQueryParams((baseUrl <<? Map("param1" -> sample, "param2" -> "value2")).removeQueryParams(Map("param2" -> "other")))
    res.find(_._1 == "param1") ?= Some("param1" -> sample)
    res.find(_._1 == "param2") ?= None
  }

  property("Setting a url to secure") = forAll(Gen.alphaStr) { (sample: String) =>
    val req = baseUrl / "path" secure
    val res = getUrl(req)

    req.isSecure ?= true
    res ?= "https://localhost/path"
  }

  property("GET verb handler") = forAll(Gen.alphaStr) { (sample: String) =>
    val res1 = getMethod(GET ("localhost"))
    val res2 = getMethod(POST ("localhost"))
    val res3 = getMethod(PUT ("localhost"))
    val res4 = getMethod(DELETE ("localhost"))
    val res5 = getMethod(TRACE ("localhost"))
    val res6 = getMethod(OPTIONS ("localhost"))
    val res7 = getMethod(HEAD ("localhost"))

    res1 ?= "GET"
    res2 ?= "POST"
    res3 ?= "PUT"
    res4 ?= "DELETE"
    res5 ?= "TRACE"
    res6 ?= "OPTIONS"
    res7 ?= "HEAD"
  }

  private[this] def getUrl(req: HttpRequest): String = req.request.build.getUrl()

  private[this] def getMethod(req: HttpRequest): String = req.request.build.getMethod()

  private[this] def getHeaders(req: HttpRequest): Map[String, String] =
    JMapWrapper(req.request.build.getHeaders()).toMap.map(pair => (pair._1 -> pair._2.get(0)))

  private[this] def getPostParams(req: HttpRequest): Map[String, String] =
    JMapWrapper(req.request.build.getParams()).toMap.map(pair => (pair._1 -> pair._2.get(0)))

  private[this] def getQueryParams(req: HttpRequest): Map[String, String] =
    JMapWrapper(req.request.build.getQueryParams()).toMap.map(pair => (pair._1 -> pair._2.get(0)))
}
