package dispatch.as.json4s

import org.json4s._
import org.json4s.native.JsonMethods._
import org.asynchttpclient.Response

object Json extends (Response => JValue) {
  def apply(r: Response) =
    (dispatch.as.String andThen (s => parse(StringInput(s), true)))(r)
}
