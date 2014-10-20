package dispatch

import com.ning.http.client.RequestBuilder

/** This wrapper provides referential transparency for the
  underlying RequestBuilder. */
case class Req(
  run: RequestBuilder => RequestBuilder,
  props: Req.Properties = Req.Properties()
) extends MethodVerbs with UrlVerbs with ParamVerbs
with AuthVerbs with HeaderVerbs with RequestBuilderVerbs {
  def subject = this
  def underlying(next: RequestBuilder => RequestBuilder) =
    Req(run andThen next, props)
  def underlying(nextReq: RequestBuilder => RequestBuilder, nextProps: Req.Properties => Req.Properties) =
    Req(run andThen nextReq, nextProps(props))

  def toRequestBuilder = {
    val requestBuilder = run(new RequestBuilder)
    //Body set from String and with no Content-Type will get a default of 'text/plain; charset=UTF-8'
    if(props.bodyType == Req.StringBody && !requestBuilder.build.getHeaders.containsKey("Content-Type")) {
      setContentType("text/plain", "UTF-8").run(new RequestBuilder)
    } else {
      requestBuilder
    }
  }
  def toRequest = toRequestBuilder.build
}

object Req {
  final case class Properties(bodyType: BodyType = NoBody)

  trait BodyType
  final case object NoBody extends BodyType
  final case object StringBody extends BodyType
  final case object ByteArrayBody extends BodyType
  final case object EntityWriterBody extends BodyType
  final case object FileBody extends BodyType
}

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
  def HEAD    = subject.setMethod("HEAD")
  def GET     = subject.setMethod("GET")
  def POST    = subject.setMethod("POST")
  def PUT     = subject.setMethod("PUT")
  def DELETE  = subject.setMethod("DELETE")
  def PATCH   = subject.setMethod("PATCH")
  def TRACE   = subject.setMethod("TRACE")
  def OPTIONS = subject.setMethod("OPTIONS")
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
    subject.setUrl(uri.copy(path=rawPath, query=None).toString)
  }
  def / (segment: AnyVal): Req = segment match {
    case unit: Unit => subject
    case other      => this / other.toString
  }

  def /? (segmentOpt: Option[String]): Req =
    segmentOpt.map(this / _) getOrElse subject

  def secure = {
    subject.setUrl(RawUri(url).copy(scheme=Some("https")).toString)
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
  private def defaultMethod(method: String): Req = {
    if (subject.toRequest.getMethod.toUpperCase == "GET")
      subject.setMethod(method)
    else subject
  }

  /** Adds `params` to the request body. Sets request method
   *  to POST if it is currently GET. */
  def << (params: Traversable[(String,String)]) = {
    (defaultMethod("POST") /: params) {
      case (s, (key, value)) =>
        s.addParameter(key, value)
    }
  }
  /** Set request body to a given string,
   *  - set method to POST if currently GET,
   *  - set HTTP Content-Type to "text/plain; charset=UTF-8" if unspecified. */
  def << (body: String) = {
    defaultMethod("POST").setBody(body)
  }
  /** Set a file as the request body and set method to PUT if it's
    * currently GET. */
  def <<< (file: java.io.File) = {
    defaultMethod("PUT").setBody(file)
  }
  /** Adds `params` as query parameters */
  def <<? (params: Traversable[(String,String)]) =
    (subject /: params) {
      case (s, (key, value)) =>
        s.addQueryParameter(key, value)
    }
}

trait AuthVerbs extends RequestVerbs {
  import com.ning.http.client.Realm, Realm.{RealmBuilder,AuthScheme}
  def as(user: String, password: String): Req =
    this.as(new RealmBuilder()
      .setPrincipal(user)
      .setPassword(password)
      .build())
  /** Basic auth, use with care. */
  def as_!(user: String, password: String): Req =
    this.as(new RealmBuilder()
      .setPrincipal(user)
      .setPassword(password)
      .setUsePreemptiveAuth(true)
      .setScheme(AuthScheme.BASIC)
      .build())

  def as(realm: Realm) = subject.setRealm(realm)

}

trait RequestBuilderVerbs extends RequestVerbs {
  import com.ning.http.client.{ FluentStringsMap, Part, ProxyServer, Realm, Request }
  import com.ning.http.client.cookie.Cookie
  import Request.EntityWriter
  import scala.collection.JavaConverters._
  import java.util.Collection

  def addBodyPart(part: Part) =
    subject.underlying { _.addBodyPart(part) }
  def addCookie(cookie: Cookie) =
    subject.underlying { _.addCookie(cookie) }
  def addHeader(name: String, value: String) =
    subject.underlying { _.addHeader(name, value) }
  def addParameter(key: String, value: String) =
    subject.underlying { _.addParameter(key, value) }
  def addQueryParameter(name: String, value: String) =
    subject.underlying { _.addQueryParameter(name, value) }
  def setQueryParameters(params: Map[String, Seq[String]]) =
    subject.underlying { _.setQueryParameters(new FluentStringsMap(
      params.mapValues{ _.asJava: Collection[String] }.asJava
    )) }
  def setBody(data: Array[Byte]) =
    subject.underlying(rb => rb.setBody(data), p => p.copy(bodyType = Req.ByteArrayBody))
  def setBody(dataWriter: EntityWriter, length: Long) =
    subject.underlying(rb => rb.setBody(dataWriter, length), p => p.copy(bodyType = Req.EntityWriterBody))
  def setBody(dataWriter: EntityWriter) =
    subject.underlying(rb => rb.setBody(dataWriter), p => p.copy(bodyType = Req.EntityWriterBody))
  def setBody(data: String) =
    subject.underlying(rb => rb.setBody(data), p => p.copy(bodyType = Req.StringBody))
  def setBody(file: java.io.File) =
    subject.underlying(rb => rb.setBody(file), p => p.copy(bodyType = Req.FileBody))
  def setBodyEncoding(charset: String) =
    subject.underlying { _.setBodyEncoding(charset) }
  def setContentType(mediaType: String, charset: String) =
    subject.underlying {
      _.setHeader("Content-Type", mediaType + "; charset=" + charset).
      setBodyEncoding(charset)
    }
  def setHeader(name: String, value: String) =
    subject.underlying { _.setHeader(name, value) }
  def setHeaders(headers: Map[String, Seq[String]]) =
    subject.underlying { _.setHeaders(
      headers.mapValues { _.asJava: Collection[String] }.asJava
    ) }
  def setParameters(parameters: Map[String, Seq[String]]) =
    subject.underlying { _.setParameters(
      parameters.mapValues { _.asJava: Collection[String] }.asJava
    ) }
  def setMethod(method: String) =
    subject.underlying { _.setMethod(method) }
  def setUrl(url: String) =
    subject.underlying { _.setUrl(url) }
  def setProxyServer(proxyServer: ProxyServer) =
    subject.underlying { _.setProxyServer(proxyServer) }
  def setVirtualHost(virtualHost: String) =
    subject.underlying { _.setVirtualHost(virtualHost) }
  def setFollowRedirects(followRedirects: Boolean) =
    subject.underlying { _.setFollowRedirects(followRedirects) }
  def addOrReplaceCookie(cookie: Cookie) =
    subject.underlying { _.addOrReplaceCookie(cookie) }
  def setRealm(realm: Realm) =
    subject.underlying { _.setRealm(realm) }

  @deprecated("Use `toRequest`", since="0.11.0")
  def build() = subject.toRequest
}
