package dispatch

object uri {
  def apply(str: String) = new Uri(str)
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
