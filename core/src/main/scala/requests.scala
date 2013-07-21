package dispatch

import com.ning.http.client.RequestBuilder

/** This wrapper provides referential transparency for the
  underlying RequestBuilder. */
case class Req(run: RequestBuilder => RequestBuilder) {
  def underlying(next: RequestBuilder => RequestBuilder) =
    Req(run andThen next)
  def toRequestBuilder = run(new RequestBuilder)
  def toRequest = toRequestBuilder.build
}

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
  def HEAD    = subject underlying (_.setMethod("HEAD"))
  def GET     = subject underlying (_.setMethod("GET"))
  def POST    = subject underlying (_.setMethod("POST"))
  def PUT     = subject underlying (_.setMethod("PUT"))
  def DELETE  = subject underlying (_.setMethod("DELETE"))
  def PATCH   = subject underlying (_.setMethod("PATCH"))
  def TRACE   = subject underlying (_.setMethod("TRACE"))
  def OPTIONS = subject underlying (_.setMethod("OPTIONS"))
}

trait UrlVerbs extends RequestVerbs {
  def url = subject.toRequest.getUrl
  def / (segment: String) = {
    val uri = RawUri(url)
    val encodedSegment = UriEncode.path(segment)
    val rawPath = uri.path.orElse(Some("/")).map {
      case u if u.endsWith("/") => u + encodedSegment
      case u => u + "/" + encodedSegment
    }
    subject underlying(_.setUrl(uri.copy(path=rawPath).toString))
  }
  def / (segment: AnyVal): Req = segment match {
    case unit: Unit => subject
    case other      => this / other.toString
  }
  def secure = {
    subject underlying (_.setUrl(RawUri(url).copy(scheme=Some("https")).toString))
  }
}

trait HeaderVerbs extends RequestVerbs {
  def <:< (hs: Traversable[(String,String)]) =
    (subject /: hs) {
      case (s, (key, value)) =>
        s underlying (_.addHeader(key, value))
    }
}

trait ParamVerbs extends RequestVerbs {
  private def defaultMethod(method: String) = {
    if (subject.toRequest.getMethod.toUpperCase == "GET")
      subject underlying (_.setMethod(method))
    else subject
  }
  /** Adds `params` to the request body. Sets request method
   *  to POST if it is currently GET. */
  def << (params: Traversable[(String,String)]) = {
    (defaultMethod("POST") /: params) {
      case (s, (key, value)) =>
        s underlying (_.addParameter(key, value))
    }
  }
  /** Set request body to a given string, set method to POST
   * if currently GET. */
  def << (body: String) = {
    defaultMethod("POST") underlying (_.setBody(body))
  }
  /** Set a file as the request body and set method to PUT if it's
    * currently GET. */
  def <<< (file: java.io.File) = {
    defaultMethod("PUT") underlying (_.setBody(file))
  }
  /** Adds `params` as query parameters */
  def <<? (params: Traversable[(String,String)]) =
    (subject /: params) {
      case (s, (key, value)) =>
        s underlying (_.addQueryParameter(key, value))
    }
}

trait AuthVerbs extends RequestVerbs {
  import com.ning.http.client.Realm.{RealmBuilder,AuthScheme}
  def as(user: String, password: String) =
    subject underlying (_.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .build()))
  def as_!(user: String, password: String) =
    subject underlying (_.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .setUsePreemptiveAuth(true)
                     .setScheme(AuthScheme.BASIC)
                     .build()))
}
