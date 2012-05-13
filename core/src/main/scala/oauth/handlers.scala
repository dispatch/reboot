package dispatch.oauth

import dispatch._
import com.ning.http.client.oauth._

object AsOAuth {
  def decode(str: String) = java.net.URLDecoder.decode(str, "utf-8")
  def formDecode(str: String) =
    (for (pair <- str.trim.split('&'))
      yield pair.split('=')
    ).collect {
      case Array(key, value) => decode(key) -> decode(value)
    }

  def tokenDecode(str: String) = {
    val params = formDecode(str)
    (for {
      ("oauth_token", tok) <- params
      ("oauth_token_secret", secret) <- params
    } yield new RequestToken(tok, secret)).headOption.toRight {
      "No token found in response: \n\n" + str
    }
  }
  val token = dispatch.As.string.andThen(tokenDecode)
}
