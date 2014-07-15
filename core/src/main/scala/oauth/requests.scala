package dispatch.oauth

import com.ning.http.client.oauth._

import dispatch._

class SigningVerbs(val subject: Req) extends RequestVerbs {
  val emptyToken = new RequestToken(null, "")

  def sign(consumer: ConsumerKey, token: RequestToken = emptyToken) = {
    val calc = new OAuthSignatureCalculator(consumer, token)
    subject underlying { r =>
      val req = r.build
      val baseurl = req.getUrl().takeWhile { _ != '?' }.mkString("")
      calc.calculateAndAddSignature(baseurl, req, r)
      r
    }
  }

  def <@(consumer: ConsumerKey, token: RequestToken = emptyToken) =
    sign(consumer, token)

}
