package dispatch.as.json4s

import org.json4s.JValue
import org.json4s.jackson.JsonParser
import com.ning.http.client.Response

object Json extends (Response => JValue) {
  def apply(r: Response) =
    (dispatch.as.String andThen JsonParser.parse)(r)
}
