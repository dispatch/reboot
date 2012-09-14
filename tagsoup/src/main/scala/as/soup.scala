package dispatch.as.tagsoup

import com.ning.http.client.Response
import xml.{NodeSeq => XNodeSeq}
import xml.parsing.NoBindingFactoryAdapter
import java.io.StringReader
import org.xml.sax.InputSource
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

object NodeSeq extends (Response => XNodeSeq) {
  lazy val parserFactory = new SAXFactoryImpl
  lazy val adapter = new NoBindingFactoryAdapter
  def apply(r: Response) =
    adapter.loadXML(new InputSource(r.getResponseBodyAsStream), parserFactory.newSAXParser)
}
