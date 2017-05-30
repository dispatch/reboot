package dispatch.clients

import dispatch._

sealed trait OAuth
case class OAuth2Creds(client_key: String, client_secret: String) extends OAuth
