package dispatch

object Uri {
  def apply(str: String) = new Uri(str)

  // uri character sets
  val alpha = lowalpha ++ upalpha
  val lowalpha = 'a' to 'z'
  val upalpha = 'A' to 'Z'
  val digit = '0' to '9'
  val alphanum = alpha ++ digit
  val mark = '-' :: '_' :: '.' :: '!' :: '~' :: '*' ::
             '\'' :: '(' :: ')' :: Nil
  val unreserved = alpha ++ mark
  val pchar = unreserved ++ (
    ':' :: '@' :: '&' :: '=' :: '+' :: '$' :: ',' :: Nil
  )
  val segmentValid = (';' +: pchar).toSet
}

class RichUri(subject: Uri) {
  def copy(
    scheme: String = subject.getScheme,
    userInfo: String = subject.getUserInfo,
    host: String = subject.getHost,
    port: Int = subject.getPort,
    path: String = subject.getPath,
    query: String = subject.getQuery,
    fragment: String = subject.getFragment
  ) = new Uri(
    scheme,
    userInfo,
    host,
    port,
    path,
    query,
    fragment
  )
}
