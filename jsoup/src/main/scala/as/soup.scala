package dispatch.as.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes
import com.ning.http.client.Response

object Document extends (Response => nodes.Document) {
  def apply(r: Response) =
    (dispatch.as.String andThen Jsoup.parse)(r)
}

object Query {
  import org.jsoup.select.Elements
  def apply(query: String)(r: Response): Elements =
    Document(r).select(query)
} 
