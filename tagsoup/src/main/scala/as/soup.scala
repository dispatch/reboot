package dispatch.as.tagsoup

import com.ning.http.client.Response
import xml.{NodeSeq => XNodeSeq}
import xml.parsing.NoBindingFactoryAdapter
import java.io.StringReader
import org.xml.sax.InputSource
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl

object NodeSeq extends (Response => XNodeSeq) {
  lazy val parserFactory = new SAXFactoryImpl
  def apply(r: Response) =
    (dispatch.as.String andThen (s => new NoBindingFactoryAdapter().loadXML(
      new InputSource(new StringReader(s)),
      parserFactory.newSAXParser()
    )))(r)
}
