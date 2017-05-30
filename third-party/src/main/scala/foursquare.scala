package dispatch.clients

import dispatch._
import com.ning.http.client.RequestBuilder

class Foursquare(params: (String, String)*) extends FoursquareHelper {
  protected val svc = "https://api.foursquare.com"
  protected var urlParams = Map[String, String]()
  protected var version = ""

  urlParams ++= params.toList
}

object Foursquare {
  def apply(creds: OAuth2Creds) =
    new Foursquare("client_id" -> creds.client_key, "client_secret" -> creds.client_secret)
}

trait FoursquareHelper {
  this: Foursquare =>

  def setVersion(newVersion: String) = {
    version = "/" + newVersion
  }

  def call(path: String, params: Traversable[(String, String)] = List()) = {
    executeHttp(buildUri(path, params))
  }

  def buildUri(fullPath: String, params: Traversable[(String, String)]) = {
    url(svc + version + fullPath) <<? (urlParams ++ params)
  }

  def executeHttp(uri: RequestBuilder) = {
    Http(uri OK as.String)
  }
}
