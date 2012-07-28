package dispatch

import com.ning.http.client.RequestBuilder

class DefaultRequestVerbs(val subject: RequestBuilder)
extends MethodVerbs with UrlVerbs with ParamVerbs with AuthVerbs
with HeaderVerbs

trait HostVerbs {
  def apply(host: String) =
    new RequestBuilder().setUrl("http://%s/".format(host))
  def apply(host: String, port: Int) =
    new RequestBuilder().setUrl("http://%s:%d/".format(host, port))
}
object :/ extends HostVerbs
object host extends HostVerbs

object url extends (String => RequestBuilder) {
  def apply(url: String) = new RequestBuilder().setUrl(url)
}
trait RequestVerbs {
  def subject: RequestBuilder
}

trait MethodVerbs extends RequestVerbs {
  def HEAD   = subject.setMethod("HEAD")
  def GET    = subject.setMethod("GET")
  def POST   = subject.setMethod("POST")
  def PUT    = subject.setMethod("PUT")
  def DELETE = subject.setMethod("DELETE")
  def PATCH  = subject.setMethod("PATCH")
}

trait UrlVerbs extends RequestVerbs {
  import java.net.URI
  def url = subject.build.getUrl // unfortunate
  def / (path: String) = subject.setUrl(url match {
    case u if u.endsWith("/") => u + path
    case u => u + "/" + path
  })
  def secure = {
    val uri = URI.create(url)
    subject.setUrl(new URI(
      "https", uri.getAuthority, uri.getPath, uri.getQuery, uri.getFragment
    ).toString)
  }
}

trait HeaderVerbs extends RequestVerbs {
  def <:< (hs: Traversable[(String,String)]) =
    (subject /: hs) {
      case (s, (key, value)) =>
        s.addHeader(key, value)
    }
}

trait ParamVerbs extends RequestVerbs {
  /** Adds `params` to the request body. Sets request method
   *  to POST if it is currently GET. */
  def << (params: Traversable[(String,String)]) = {
    val subj =
      if (subject.build.getMethod.toUpperCase == "GET")
        subject.setMethod("POST")
      else subject
    (subj /: params) {
      case (s, (key, value)) =>
        s.addParameter(key, value)
    }
  }
  /** Set a file as the request body and set method to PUT if it's
    * currently GET. */
  def <<< (file: java.io.File) = {
    val subj =
      if (subject.build.getMethod.toUpperCase == "GET")
        subject.setMethod("PUT")
      else subject
    subj.setBody(file)
  }
  /** Adds `params` as query parameters */
  def <<? (params: Traversable[(String,String)]) =
    (subject /: params) {
      case (s, (key, value)) =>
        s.addQueryParameter(key, value)
    }
}

trait AuthVerbs extends RequestVerbs {
  import com.ning.http.client.Realm.{RealmBuilder,AuthScheme}
  def as(user: String, password: String) =
    subject.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .build())
  def as_!(user: String, password: String) =
    subject.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .setUsePreemptiveAuth(true)
                     .setScheme(AuthScheme.BASIC)
                     .build())
}
