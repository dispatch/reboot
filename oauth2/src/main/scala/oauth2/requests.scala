package dispatch.oauth2

import dispatch.{Req, RequestVerbs}

class SigningVerbs(val subject: Req) extends RequestVerbs {

  val emptyToken = AccessToken("")

  def sign(at: AccessToken = emptyToken, verb: String = "OAuth"): Req = {
    subject.addHeader("Authorization", List(verb, at.token).mkString(" "))
  }

  def <@(at: AccessToken = emptyToken, verb: String = "OAuth"): Req =
    sign(at, verb)
}
