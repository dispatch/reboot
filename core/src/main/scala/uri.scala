package dispatch

object UriEncode {
  // uri character sets
  def alpha = lowalpha ++ upalpha
  def lowalpha = 'a' to 'z'
  def upalpha = 'A' to 'Z'
  def digit = '0' to '9'
  def alphanum = alpha ++ digit
  def mark = '-' :: '_' :: '.' :: '!' :: '~' :: '*' ::
             '\'' :: '(' :: ')' :: Nil
  def unreserved = alpha ++ mark
  def pchar = unreserved ++ (
    ':' :: '@' :: '&' :: '=' :: '+' :: '$' :: ',' :: Nil
  )
  val segmentValid = (';' +: pchar).toSet

  def path(pathSegment: String) = {
    (for (char <- pathSegment.iterator) yield {
      if (segmentValid.contains(char))
        char
      else
        "%%%X".format(char.toInt)
    }).mkString
  }
}

class RawUri(subject: Uri) {
  def copy(
    scheme: Option[String] = Option(subject.getScheme),
    userInfo: Option[String] = Option(subject.getRawUserInfo),
    host: Option[String] = Option(subject.getHost),
    port: Option[Int] = Some(subject.getPort).filter( _ != -1),
    path: Option[String] = Option(subject.getRawPath),
    query: Option[String] = Option(subject.getRawQuery),
    fragment: Option[String] = Option(subject.getRawFragment)
  ) =
    (scheme.map { _ + ":" } ::
     userInfo.map { _ +  "@" } ::
     host ::
     port.map { ":" + _ } ::
     path ::
     query.map { "?" + _ } ::
     fragment.map { "#" + _ } ::
     Nil
   ).flatten.mkString
}
