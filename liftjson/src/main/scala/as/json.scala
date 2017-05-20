package dispatch.as.lift

import net.liftweb.json.{ JsonParser, JValue }
import org.asynchttpclient.Response

object Json extends (Response => JValue) {
  def apply(r: Response) =
    (dispatch.as.String andThen JsonParser.parse)(r)
}
