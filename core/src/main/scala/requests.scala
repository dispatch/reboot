package dispatch

import com.ning.http.client.RequestBuilder

class DefaultRequestVerbs(val subject: Req)
extends MethodVerbs with UrlVerbs with ParamVerbs with AuthVerbs
with HeaderVerbs

trait HostVerbs {
  def apply(host: String) = {
    val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    Req(_.setUrl("http://%s/".format(asciiSafeDomain)))
  }

  def apply(host: String, port: Int) = {
    val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    Req(_.setUrl("http://%s:%d/".format(asciiSafeDomain, port)))
  }
}

object :/ extends HostVerbs
object host extends HostVerbs

object url extends (String => Req) {
  def apply(url: String) = {
    Req(_.setUrl(RawUri(url).toString))
  }
}

trait RequestVerbs {
  def subject: Req
}

trait MethodVerbs extends RequestVerbs {
  def HEAD    = subject(_.setMethod("HEAD"))
  def GET     = subject(_.setMethod("GET"))
  def POST    = subject(_.setMethod("POST"))
  def PUT     = subject(_.setMethod("PUT"))
  def DELETE  = subject(_.setMethod("DELETE"))
  def PATCH   = subject(_.setMethod("PATCH"))
  def TRACE   = subject(_.setMethod("TRACE"))
  def OPTIONS = subject(_.setMethod("OPTIONS"))
}

trait UrlVerbs extends RequestVerbs {
  def url = subject.build.getUrl
  def / (segment: String) = {
    val uri = RawUri(url)
    val encodedSegment = UriEncode.path(segment)
    val rawPath = uri.path.orElse(Some("/")).map {
      case u if u.endsWith("/") => u + encodedSegment
      case u => u + "/" + encodedSegment
    }
    subject(_.setUrl(uri.copy(path=rawPath).toString))
  }
  def / (segment: AnyVal): Req = segment match {
    case unit: Unit => subject
    case other      => this / other.toString
  }
  def secure = {
    subject(_.setUrl(RawUri(url).copy(scheme=Some("https")).toString))
  }
}

trait HeaderVerbs extends RequestVerbs {
  def <:< (hs: Traversable[(String,String)]) =
    (subject /: hs) {
      case (s, (key, value)) =>
        s(_.addHeader(key, value))
    }
}

trait ParamVerbs extends RequestVerbs {
  private def defaultMethod(method: String) = {
    if (subject.build.getMethod.toUpperCase == "GET")
      subject(_.setMethod(method))
    else subject
  }
  /** Adds `params` to the request body. Sets request method
   *  to POST if it is currently GET. */
  def << (params: Traversable[(String,String)]) = {
    (defaultMethod("POST") /: params) {
      case (s, (key, value)) =>
        s(_.addParameter(key, value))
    }
  }
  /** Set request body to a given string, set method to POST
   * if currently GET. */
  def << (body: String) = {
    defaultMethod("POST")(_.setBody(body))
  }
  /** Set a file as the request body and set method to PUT if it's
    * currently GET. */
  def <<< (file: java.io.File) = {
    defaultMethod("PUT")(_.setBody(file))
  }
  /** Adds `params` as query parameters */
  def <<? (params: Traversable[(String,String)]) =
    (subject /: params) {
      case (s, (key, value)) =>
        s(_.addQueryParameter(key, value))
    }
}

trait AuthVerbs extends RequestVerbs {
  import com.ning.http.client.Realm.{RealmBuilder,AuthScheme}
  def as(user: String, password: String) =
    subject(_.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .build()))
  def as_!(user: String, password: String) =
    subject(_.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .setUsePreemptiveAuth(true)
                     .setScheme(AuthScheme.BASIC)
                     .build()))
}
