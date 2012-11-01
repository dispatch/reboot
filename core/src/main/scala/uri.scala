package dispatch

/** URI representation with raw parts, so  */
case class RawUri(
  scheme: Option[String],
  userInfo: Option[String],
  host: Option[String],
  port: Option[Int],
  path: Option[String],
  query: Option[String],
  fragment: Option[String]
) {
  def toUri = new Uri(
    (scheme.map { _ + ":" } ::
     Some("//") ::
     userInfo.map { _ +  "@" } ::
     host ::
     port.map { ":" + _ } ::
     path ::
     query.map { "?" + _ } ::
     fragment.map { "#" + _ } ::
     Nil
   ).flatten.mkString)
  override def toString = toUri.toASCIIString
}

object RawUri {
  def apply(str: String): RawUri = RawUri(new Uri(str))
  def apply(subject: Uri): RawUri = RawUri(
    scheme = Option(subject.getScheme),
    userInfo = Option(subject.getRawUserInfo),
    host = Option(subject.getHost),
    port = Some(subject.getPort).filter( _ != -1),
    path = Option(subject.getRawPath),
    query = Option(subject.getRawQuery),
    fragment = Option(subject.getRawFragment)
  )
}

object UriEncode {
  // uri character sets
  def alpha = lowalpha ++ upalpha
  def lowalpha = 'a' to 'z'
  def upalpha = 'A' to 'Z'
  def digit = '0' to '9'
  def alphanum = alpha ++ digit
  def mark = '-' :: '_' :: '.' :: '!' :: '~' :: '*' ::
             '\'' :: '(' :: ')' :: Nil
  def unreserved = alphanum ++ mark
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
