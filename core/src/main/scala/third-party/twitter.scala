package dispatch.clients

import dispatch._
import oauth._
import oauth.OAuth._

object Twitter {
  val svc = :/("api.twitter.com") / "oauth"

  /** Get a request token with no callback URL, out-of-band
   * authorization assumed */
  def request_token(consumer: Consumer): Handler[Token] =
    request_token(consumer, OAuth.oob)

  def request_token(consumer: Consumer, callback_url: String) =
    svc.secure.POST / "request_token" <@ (consumer, callback_url) as_token

  def authorize_url(token: Token) =
    svc / "authorize" with_token token
  def authenticate_url(token: Token) =
    svc / "authenticate" with_token token

  def access_token(consumer: Consumer, token: Token, verifier: String) =
    svc.secure.POST / "access_token" <@ (consumer, token, verifier) >% {
      m => (Token(m).get, m("user_id"), m("screen_name"))
    }
}
