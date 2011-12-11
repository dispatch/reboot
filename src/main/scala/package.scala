package object dispatch

extends ImplicitRequestVerbs with ImplicitHandlerVerbs {
  def url(url: String) =
    new com.ning.http.client.RequestBuilder().setUrl(url)
}
