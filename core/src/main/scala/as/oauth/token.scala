package dispatch.as.oauth

import org.asynchttpclient.Response
import org.asynchttpclient.oauth.RequestToken

object Token extends (Response => Either[String, RequestToken]) {
  def apply(res: Response) = tokenDecode(dispatch.as.String(res))
  private def decode(str: String) = java.net.URLDecoder.decode(str, "utf-8")
  private def formDecode(str: String) =
    (for (pair <- str.trim.split('&'))
      yield pair.split('=')
    ).collect {
      case Array(key, value) => decode(key) -> decode(value)
      case Array(key) => decode(key) -> ""
    }

  private def tokenDecode(str: String) = {
    val params = formDecode(str)
    (for {
      ("oauth_token", tok) <- params
      ("oauth_token_secret", secret) <- params
    } yield new RequestToken(tok, secret)).headOption.toRight {
      "No token found in response: \n\n" + str
    }
  }
}
