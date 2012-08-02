package dispatch.as.lift

import net.liftweb.json._
import JsonDSL._

import com.ning.http.client

object Json extends (client.Response => JValue) {
  def apply(r: client.Response) = JsonParser.parse(dispatch.as.String(r))
}
