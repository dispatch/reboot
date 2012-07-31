package dispatch.clients

import dispatch._

object Foursquare {
  private val svc: Request = :/ ("api.foursquare.com") secure

  def apply(key: String, secret: String)(request: Request): Request =
    apply((key, secret))(request)

  def apply(creds: Pair[String, String])(request: Request): Request = {
    svc <& request <<? Map("client_id" -> creds._1, "client_secret" -> creds._2)
  }

}
