package dispatch

package object oauth {
  implicit def implySigningVerbs(builder: Req) =
    new SigningVerbs(builder)
}
