package dispatch.oauth2

import dispatch._

import scala.concurrent.{Future,ExecutionContext}

case class ConsumerKey(id: String, secret: String)
case class AccessToken(token: String)

trait SomeHttp {
  def http: HttpExecutor
}

trait SomeConsumer {
  def consumer: ConsumerKey
}

trait SomeEndpoints {
  def authorize: String
  def token: String
}

trait SomeCallback {
  def callback: String
}

trait Exchange {
  self: SomeHttp
    with SomeConsumer
    with SomeEndpoints
    with SomeCallback =>

  def authorizeUrl = {
    (url(authorize) <<? Map(
      "response_type" -> "code",
      "client_id" -> consumer.id,
      "redirect_uri" -> callback
    )).url
  }

  def message[A](promised: Future[A], ctx: String)
                (implicit executor: ExecutionContext)  =
    for (exc <- promised.either.left)
      yield "Unexpected problem fetching %s:\n%s".format(ctx, exc.getMessage)

  def requestAccessToken(code: String)
                        (implicit executor: ExecutionContext)
  : Future[Either[String,AccessToken]] = {
    val promised = http(url(token) << Map(
      "grant_type" -> "authorization_code",
      "client_id" -> consumer.id,
      "client_secret" -> consumer.secret,
      "redirect_uri" -> callback,
      "code" -> code
    ) > as.oauth2.Token)

    for(eth <- message(promised, "access token")) yield eth.joinRight
  }
}
