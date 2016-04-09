package dispatch.oauth

import dispatch._
import org.asynchttpclient.oauth._

class SigningVerbs(val subject: Req) extends RequestVerbs {
  val emptyToken = new RequestToken(null, "")

  def sign(consumer: ConsumerKey, token: RequestToken = emptyToken) = {
    val calc = new OAuthSignatureCalculator(consumer, token)
    subject underlying { r =>
      calc.calculateAndAddSignature(r.build, r)
      r
    }
  }

  def <@(consumer: ConsumerKey, token: RequestToken = emptyToken) =
    sign(consumer, token)

}
