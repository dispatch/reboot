package dispatch.oauth

import dispatch._

import com.ning.http.client.oauth._

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
    com.ning.http.util.Base64.encode(nonceBuffer)
  }

  def message[A](promised: Promise[A], ctx: String) =
    for (exc <- promised.either.left)
      yield "Unexpected problem fetching %s:\n%s".format(ctx, exc.getMessage)

  def fetchRequestToken: Promise[Either[String,RequestToken]] = {
    val promised = http(
      url(requestToken) 
        << Map("oauth_callback" -> callback)
        <@ (consumer)
        > AsOAuth.token
    )
    for (eth <- message(promised, "request token")) yield eth.joinRight
  }

  def signedAuthorize(reqToken: RequestToken) = {
    import com.ning.http.client.FluentStringsMap

    val calc = new OAuthSignatureCalculator(consumer, reqToken)
    val timestamp = System.currentTimeMillis() / 1000L
    val unsigned = url(authorize) <<? Map("oauth_token" -> reqToken.getKey)
    val sig = calc.calculateSignature("GET",
                                      unsigned.url,
                                      timestamp,
                                      generateNonce,
                                      new FluentStringsMap,
                                      new FluentStringsMap)
    (unsigned <<? Map("oauth_signature" -> sig)).url
  }

  def fetchAccessToken(reqToken: RequestToken, verifier: String)
  : Promise[Either[String,RequestToken]]  = {
    val promised = http(
      url(accessToken)
        << Map("oauth_verifier" -> verifier)
        <@ (consumer, reqToken)
        > AsOAuth.token
    )
    for (eth <- message(promised, "access token")) yield eth.joinRight
  }

}
