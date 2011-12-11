package dispatch

import com.ning.http.client.RequestBuilder

object ImplicitRequestVerbs extends ImplicitRequestVerbs

trait ImplicitRequestVerbs {
  implicit def implyRequestVerbs(builder: RequestBuilder) =
    new RequestVerbs(builder)
}

class RequestVerbs(val subject: RequestBuilder)
extends MethodVerbs with PathVerbs with ParamVerbs

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

trait PathVerbs {
  def subject: RequestBuilder
  def / (path: String) = subject.setUrl(
    java.net.URI.create(subject.build.getUrl()).resolve(path).toString
  )
}

trait ParamVerbs {
  def subject: RequestBuilder
  def << (params: Traversable[(String,String)]) =
    (subject.setMethod("POST") /: params) {
      case (s, (key, value)) =>
        s.addParameter(key, value)
    }
}
