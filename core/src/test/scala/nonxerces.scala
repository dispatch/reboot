package dispatch.spec

import javax.xml.parsers.SAXParserFactory
import org.scalacheck._
import org.scalacheck.Prop._
import org.xml.sax.SAXNotRecognizedException

import dispatch.as.xml.Elem

import scala.util.Try

/** SAXParserFactory stub simulating a non-Xerces provider: it treats the
  * Xerces-only DOCTYPE and external-DTD features as unrecognized and delegates
  * everything else (including parser construction) to the JDK default. */
class NonXercesFactory extends SAXParserFactory {
  private val delegate = SAXParserFactory.newInstance()
  private val unrecognized =
    Set("http://apache.org/xml/features/disallow-doctype-decl",
        "http://apache.org/xml/features/nonvalidating/load-external-dtd")
  override def setFeature(name: String, value: Boolean): Unit =
    if (unrecognized(name)) throw new SAXNotRecognizedException(name)
    else delegate.setFeature(name, value)
  override def getFeature(name: String): Boolean =
    if (unrecognized(name)) throw new SAXNotRecognizedException(name)
    else delegate.getFeature(name)
  override def newSAXParser() = delegate.newSAXParser()
}

/** Regression tests for dispatch/reboot#252: DOCTYPE hardening must fail closed
  * on SAX providers that don't recognize the Xerces disallow-doctype-decl
  * feature. Pure factory tests; no HTTP server involved. */
object NonXercesSpecification
extends Properties("NonXerces") {

  property("fails closed when the DOCTYPE ban is unrecognized") = {
    val res = Try(Elem.harden(new NonXercesFactory).newSAXParser()).toEither
    s"result: $res" |: res.fold(
      _.isInstanceOf[java.lang.IllegalStateException],
      _ => false
    )
  }

  property("still parses a benign document with a recognized provider") = {
    val parser = Elem.harden(SAXParserFactory.newInstance()).newSAXParser()
    val res = Try(scala.xml.XML.withSAXParser(parser).loadString("<r>benign</r>"))
    val ok: Prop = res match {
      case scala.util.Success(elem) => elem.text ?= "benign"
      case _                        => Prop(false)
    }
    s"result: $res" |: ok
  }
}
