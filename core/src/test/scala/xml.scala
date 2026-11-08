package dispatch.spec

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.scalacheck._
import org.scalacheck.Prop._
import org.xml.sax.SAXParseException

object XmlSpecification
extends Properties("Xml")
with DispatchCleanup {
  import dispatch._

  private val port = unfiltered.util.Port.any
  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    netty.Server.local(port).handler(netty.cycle.Planify {
      case req @ Path("/xml") =>
        ContentType("application/xml") ~> ResponseString(Body.string(req))
    }).start()
  }

  val localhost = host("127.0.0.1", port)

  // The server responds with whatever it is sent, standing in for a hostile
  // service that controls the response body.
  private def fetch(body: String) =
    Http.default(localhost / "xml" << body OK as.xml.Elem).either

  private def rejected(res: Either[Throwable, scala.xml.Elem], forbidden: String) =
    s"result: $res" |: all(
      "fails with SAXParseException" |: res.fold(_.isInstanceOf[SAXParseException], _ => false),
      "does not contain the entity value" |: !res.fold(_.toString, _.toString).contains(forbidden)
    )

  property("parses a benign body") = forAll(Gen.const("benign")) { sample =>
    val res = fetch(s"<r>$sample</r>")
    res().map(_.text) ?= Right(sample)
  }

  property("rejects a malformed body") = forAll(Gen.const("<r>unclosed")) { sample =>
    rejected(fetch(sample)(), "unclosed")
  }

  property("rejects an external entity without reading the file") = forAll(Gen.uuid) { uuid =>
    val secret = s"xxe-canary-$uuid"
    val file = Files.createTempFile("dispatch-xxe", ".txt")
    try {
      Files.write(file, secret.getBytes(StandardCharsets.UTF_8))
      val body =
        s"""<?xml version="1.0"?>
           |<!DOCTYPE r [<!ENTITY x SYSTEM "${file.toUri}">]>
           |<r>&x;</r>""".stripMargin
      rejected(fetch(body)(), secret)
    } finally {
      Files.delete(file)
    }
  }

  property("rejects a DOCTYPE with only an internal entity") = forAll(Gen.uuid) { uuid =>
    val value = s"internal-$uuid"
    val body =
      s"""<?xml version="1.0"?>
         |<!DOCTYPE r [<!ENTITY x "$value">]>
         |<r>&x;</r>""".stripMargin
    rejected(fetch(body)(), value)
  }
}
