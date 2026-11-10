package dispatch.as.xml

import dispatch._
import scala.xml._

object Elem extends (Res => scala.xml.Elem) {
  def apply(res: Res) =
    XML.withSAXParser(factory.newSAXParser).loadString(res.getResponseBody)

  private lazy val factory = harden(javax.xml.parsers.SAXParserFactory.newInstance())

  // Visible for testing: applies the XXE protections shared by every SAX
  // provider to an arbitrary factory.
  private[dispatch] def harden(spf: javax.xml.parsers.SAXParserFactory): javax.xml.parsers.SAXParserFactory = {
    spf.setNamespaceAware(false)
    spf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true)
    setRequiredFeature(spf, "http://apache.org/xml/features/disallow-doctype-decl", true)
    spf.setFeature("http://xml.org/sax/features/external-general-entities", false)
    spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    setXercesFeature(spf, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    spf.setXIncludeAware(false)
    spf
  }

  // The DOCTYPE ban is the core XXE protection: it rejects DOCTYPE declarations
  // outright, including internal entity expansion. If the SAX provider doesn't
  // recognize or support this feature we would silently parse DOCTYPEs, so fail
  // closed instead.
  private def setRequiredFeature(
    spf: javax.xml.parsers.SAXParserFactory,
    name: String,
    value: Boolean
  ): Unit =
    try spf.setFeature(name, value)
    catch {
      case e @ (_: org.xml.sax.SAXNotRecognizedException | _: org.xml.sax.SAXNotSupportedException) =>
        throw new IllegalStateException(
          s"SAX provider does not support required parser feature $name; refusing to parse responses without DOCTYPE protection",
          e
        )
    }

  // These features are Xerces-specific, so other SAX providers may not recognize
  // them. Skipping them is safe because the DOCTYPE ban above is required.
  private def setXercesFeature(
    spf: javax.xml.parsers.SAXParserFactory,
    name: String,
    value: Boolean
  ): Unit =
    try spf.setFeature(name, value)
    catch {
      case _: org.xml.sax.SAXNotRecognizedException | _: org.xml.sax.SAXNotSupportedException => ()
    }
}
