package dispatch.as.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes
import org.asynchttpclient.Response

object Document extends (Response => nodes.Document) {
  def apply(r: Response) =
    Jsoup.parse(dispatch.as.String(r), r.getUri().toString)
}

object Query {
  import org.jsoup.select.Elements
  def apply(query: String): Response => Elements =
    Document(_).select(query)
} 

object Clean {
  import org.jsoup.safety.Whitelist
  def apply(wl: Whitelist): Response => String =
    { r => Jsoup.clean(dispatch.as.String(r), r.getUri().toString, wl) }
}
