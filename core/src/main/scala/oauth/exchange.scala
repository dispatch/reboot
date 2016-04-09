package dispatch.oauth

import java.util

import dispatch._
import org.asynchttpclient._
import org.asynchttpclient.oauth._
import org.asynchttpclient.uri.Uri
import org.asynchttpclient.util.Base64

import scala.concurrent.{ExecutionContext, Future}

trait SomeHttp {
  def http: HttpExecutor
}

trait SomeConsumer {
  def consumer: ConsumerKey
}

trait SomeEndpoints {
  def requestToken: String
  def accessToken: String
  def authorize: String
}

trait SomeCallback {
  def callback: String
}

trait Exchange {
  self: SomeHttp
    with SomeConsumer
    with SomeCallback
    with SomeEndpoints =>
  private val random = new java.util.Random(System.identityHashCode(this) +
                                            System.currentTimeMillis)
  private val nonceBuffer = Array.fill[Byte](16)(0)

  def generateNonce = nonceBuffer.synchronized {
    random.nextBytes(nonceBuffer)
    Base64.encode(nonceBuffer)
  }

  def message[A](promised: Future[A], ctx: String)
                (implicit executor: ExecutionContext)  =
    for (exc <- promised.either.left)
      yield "Unexpected problem fetching %s:\n%s".format(ctx, exc.getMessage)

  def fetchRequestToken(implicit executor: ExecutionContext)
  : Future[Either[String,RequestToken]] = {
    val promised = http(
      url(requestToken) 
        << Map("oauth_callback" -> callback)
        <@ (consumer)
        > as.oauth.Token
    )
    for (eth <- message(promised, "request token")) yield eth.joinRight
  }

  def signedAuthorize(reqToken: RequestToken) = {

    val calc = new OAuthSignatureCalculator(consumer, reqToken)
    val timestamp = System.currentTimeMillis() / 1000L
    val unsigned = url(authorize) <<? Map("oauth_token" -> reqToken.getKey)
    val sig = calc.calculateSignature("GET",
                                      Uri.create(unsigned.url),
                                      timestamp,
                                      generateNonce,
                                      new util.ArrayList[Param](),
                                      new util.ArrayList[Param]())
    (unsigned <<? Map("oauth_signature" -> sig)).url
  }

  def fetchAccessToken(reqToken: RequestToken, verifier: String)
                      (implicit executor: ExecutionContext)
  : Future[Either[String,RequestToken]]  = {
    val promised = http(
      url(accessToken)
        << Map("oauth_verifier" -> verifier)
        <@ (consumer, reqToken)
        > as.oauth.Token
    )
    for (eth <- message(promised, "access token")) yield eth.joinRight
  }

}
