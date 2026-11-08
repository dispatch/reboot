package dispatch.as.xml

import dispatch._
import scala.xml._

object Elem extends (Res => scala.xml.Elem) {
  def apply(res: Res) =
    XML.withSAXParser(factory.newSAXParser).loadString(res.getResponseBody)

  private lazy val factory = {
    val spf = javax.xml.parsers.SAXParserFactory.newInstance()
    spf.setNamespaceAware(false)
    spf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true)
    setXercesFeature(spf, "http://apache.org/xml/features/disallow-doctype-decl", true)
    spf.setFeature("http://xml.org/sax/features/external-general-entities", false)
    spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    setXercesFeature(spf, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    spf.setXIncludeAware(false)
    spf
  }

  // These features are Xerces-specific, so other SAX providers may not recognize them.
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
