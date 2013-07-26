package dispatch.as.oauth2

import com.ning.http.client.Response
import dispatch.oauth2._
import scala.util.parsing.json.JSON

object Token extends (Response => Either[String, AccessToken]) {

  def apply(res: Response) = {
    val response = JSON.parseFull(dispatch.as.String(res)).
                        get.asInstanceOf[Map[String, String]]

    (for {
      ("access_token", token) <- response
    } yield AccessToken(
      token = token
    )).headOption.toRight {
      (for {
        ("error_description", description) <- response
      } yield description).head
    }
  }
}
