package dispatch

import com.ning.http.client.RequestBuilder

object ImplicitRequestVerbs extends ImplicitRequestVerbs

trait ImplicitRequestVerbs {
  implicit def implyRequestVerbs(builder: RequestBuilder) =
    new RequestVerbs(builder)
}

class RequestVerbs(val subject: RequestBuilder)
extends MethodVerbs with UrlVerbs with ParamVerbs

object :/ {
  def apply(host: String) =
    new RequestBuilder().setUrl("http://%s/".format(host))
  def apply(host: String, port: Int) =
    new RequestBuilder().setUrl("http://%s:%d/".format(host, port))
}

trait MethodVerbs {
  def subject: RequestBuilder
  def HEAD   = subject.setMethod("HEAD")
  def GET    = subject.setMethod("GET")
  def POST   = subject.setMethod("POST")
  def PUT    = subject.setMethod("PUT")
  def DELETE = subject.setMethod("DELETE")
}

trait UrlVerbs {
  import java.net.URI
  def subject: RequestBuilder
  def / (path: String) = subject.setUrl(
    URI.create(subject.build.getUrl).resolve(path).toString
  )
  def secure (path: String) = {
    val uri = URI.create(subject.build.getUrl)
    subject.setUrl(new URI(
      "https", uri.getAuthority, uri.getPath, uri.getQuery, uri.getFragment
    ).toString)
  }
}

trait ParamVerbs {
  def subject: RequestBuilder
  def << (params: Traversable[(String,String)]) =
    (subject.setMethod("POST") /: params) {
      case (s, (key, value)) =>
        s.addParameter(key, value)
    }
  def <<? (params: Traversable[(String,String)]) =
    (subject /: params) {
      case (s, (key, value)) =>
        s.addQueryParameter(key, value)
    }
}
