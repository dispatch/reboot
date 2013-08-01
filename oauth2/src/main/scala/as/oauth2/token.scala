package dispatch.as.oauth2

import com.ning.http.client.Response
import dispatch.oauth2.AccessToken
import org.json4s.JString

object Token extends (Response => Either[String, AccessToken]) {

  def apply(res: Response) = {
    val response = dispatch.as.json4s.Json(res)
    val eth = extractField(response, "access_token").toRight {
      extractField(response, "error_description").get
    }
    for (right <- eth.right) yield AccessToken(right)
  }

  def extractField(json: org.json4s.JValue, name: String) = {
    (for {
      JString(v) <- (json \\ name)
    } yield v).headOption
  }
}
