package dispatch

package object oauth2 {
  implicit def implySigningVerbs(builder: Req) =
    new SigningVerbs(builder)
}
