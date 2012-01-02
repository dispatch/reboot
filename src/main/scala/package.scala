package object dispatch

extends ImplicitRequestVerbs {
  def url(url: String) =
    new com.ning.http.client.RequestBuilder().setUrl(url)
}
