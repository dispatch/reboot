package dispatch.as.tagsoup

import org.asynchttpclient.Response
import xml.{NodeSeq => XNodeSeq}
import xml.parsing.NoBindingFactoryAdapter
import org.xml.sax.InputSource
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

object NodeSeq extends (Response => XNodeSeq) {
  lazy val parserFactory = new SAXFactoryImpl
  lazy val adapter = new NoBindingFactoryAdapter
  def apply(r: Response) =
    adapter.loadXML(new InputSource(r.getResponseBodyAsStream), parserFactory.newSAXParser)
}
