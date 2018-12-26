package dispatch

import java.nio.charset.Charset

import io.netty.handler.codec.http.cookie.Cookie
import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpHeaders}
import org.asynchttpclient.Realm.AuthScheme
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.request.body.generator.BodyGenerator
import org.asynchttpclient.request.body.multipart.Part
import org.asynchttpclient.{Realm, Request, RequestBuilder}

/**
  * This wrapper provides referential transparency for the
  * underlying RequestBuilder.
  */
case class Req(
  run: RequestBuilder => RequestBuilder,
  props: Req.Properties = Req.Properties()
) extends MethodVerbs with UrlVerbs with ParamVerbs
    with AuthVerbs with HeaderVerbs with RequestBuilderVerbs {

  def subject: Req = this

  /**
   * Append a transform onto the underlying AHC RequestBuilder.
   */
  def underlying(next: RequestBuilder => RequestBuilder): Req = {
    Req(run.andThen(next), props)
  }

  /**
   * Append a transform onto the underlying AHC RequestBuilder and
   * simultaneously transform the Req.Properties.
   */
  def underlying(
    nextReq: RequestBuilder => RequestBuilder,
    nextProps: Req.Properties => Req.Properties
  ): Req = {
    Req(run andThen nextReq, nextProps(props))
  }

  /**
   * Convert this to a concrete RequestBuilder setting the Content-Type for
   * String bodies if not already set.
   */
  def toRequestBuilder: RequestBuilder = {
    def requestBuilder = run(new RequestBuilder())
    //Body set from String and with no Content-Type will get a default of 'text/plain; charset=UTF-8'
    if(props.bodyType == Req.StringBody && !requestBuilder.build.getHeaders.contains("Content-Type")) {
      setContentType("text/plain", Charset.forName("UTF-8")).run(new RequestBuilder)
    } else {
      requestBuilder
    }
  }

  /**
   * Convert this to a concrete request.
   */
  def toRequest: Request = {
    toRequestBuilder.build
  }
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
  /**
   * Set the URL to target a specific hostname.
   */
  def apply(host: String): Req = {
    val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    Req(_.setUrl("http://%s/".format(asciiSafeDomain)))
  }

  /**
   * Set the url to target a specific hostname and port.
   */
  def apply(host: String, port: Int): Req = {
    val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    Req(_.setUrl("http://%s:%d/".format(asciiSafeDomain, port)))
  }
}

object :/ extends HostVerbs
object host extends HostVerbs

object url extends (String => Req) {
  /**
   * Set the hostname to target a specific, complete URL.
   */
  def apply(url: String): Req = {
    Req(_.setUrl(RawUri(url).toString))
  }
}

trait RequestVerbs {
  def subject: Req
}

trait MethodVerbs extends RequestVerbs {
  def HEAD    : Req = subject.setMethod("HEAD")
  def GET     : Req = subject.setMethod("GET")
  def POST    : Req = subject.setMethod("POST")
  def PUT     : Req = subject.setMethod("PUT")
  def DELETE  : Req = subject.setMethod("DELETE")
  def PATCH   : Req = subject.setMethod("PATCH")
  def TRACE   : Req = subject.setMethod("TRACE")
  def OPTIONS : Req = subject.setMethod("OPTIONS")
}

trait UrlVerbs extends RequestVerbs {
  /**
   * Retrieve the fully materialized URL.
   */
  def url: String = subject.toRequest.getUrl

  /**
   * Append a segment to the URL.
   */
  def / (segment: String): Req = {
    val uri = RawUri(url)
    val encodedSegment = UriEncode.path(segment)
    val rawPath = uri.path.orElse(Some("/")).map {
      case u if u.endsWith("/") => u + encodedSegment
      case u => u + "/" + encodedSegment
    }
    subject.setUrl(uri.copy(path=rawPath, query=None).toString)
  }

  /**
   * Append a segment to the URL.
   */
  def appendSegment(segment: String): Req = {
    /(segment)
  }

  /**
   * Append a segment to the URL using the toString
   * method of the provided segment.
   */
  def / (segment: AnyVal): Req = segment match {
    case _: Unit  => subject
    case other    => this / other.toString
  }

  /**
   * Append a segment to the URL using the toString
   * method of the provided segment.
   */
  def appendSegment(segment: AnyVal): Req = {
    /(segment)
  }

  /**
   * Append a segment that may or may not occur to the URL.
   */
  def /? (segmentOpt: Option[String]): Req =
    segmentOpt.map(this / _) getOrElse subject

  /**
   * Append a segment that may or may not occur to the URL.
   */
  def appendOptionalSegment(segmentOpt: Option[String]): Req =
    /?(segmentOpt)

  /**
   * Ensure the request is using the https scheme.
   */
  def secure: Req = {
    subject.setUrl(RawUri(url).copy(scheme=Some("https")).toString)
  }
}

trait HeaderVerbs extends RequestVerbs {
  /**
   * Append a collection of headers to the headers already
   * on the request.
   */
  def <:< (hs: Iterable[(String,String)]): Req = {
    hs.foldLeft(subject) {
      case (s, (key, value)) =>
        s.addHeader(key, value)
    }
  }

  /**
   * Append a collection of headers to the headers already
   * on the request.
   */
  def appendHeaders (hs: Iterable[(String, String)]): Req =
    <:<(hs)
}

trait ParamVerbs extends RequestVerbs {
  /**
   * Adds `params` to the request body.
   * Sets request method to POST unless it has been explicitly set.
   */
  def << (params: Iterable[(String,String)]): Req = {
    params.foldLeft(subject.implyMethod("POST")) {
      case (s, (key, value)) =>
        s.addParameter(key, value)
    }
  }

  /**
   * Adds `params` to the request body.
   * Sets request method to POST unless it has been explicitly set.
   */
  def appendBodyParams(params: Iterable[(String, String)]): Req =
    <<(params)

  /**
   * Set request body to a given string,
   *  - set method to POST unless explicitly set otherwise
   *  - set HTTP Content-Type to "text/plain; charset=UTF-8" if unspecified.
   */
  def << (body: String): Req = {
    subject.implyMethod("POST").setBody(body)
  }

  /**
   * Set request body to a given string,
   *  - set method to POST unless explicitly set otherwise
   *  - set HTTP Content-Type to "text/plain; charset=UTF-8" if unspecified.
   */
  def setStringBody(body: String): Req =
    <<(body)

  /**
   * Set a file as the request body and set method to PUT if it's
   * not explicitly set.
   */
  def <<< (file: java.io.File): Req = {
    subject.implyMethod("PUT").setBody(file)
  }

  /**
   * Set a file as the request body and set method to PUT if it's
   * not explicitly set.
   */
  def setFileBody(file: java.io.File): Req =
    <<<(file)

  /**
   * Adds `params` as query parameters
   */
  def <<? (params: Iterable[(String,String)]): Req = {
    params.foldLeft(subject) {
      case (s, (key, value)) =>
        s.addQueryParameter(key, value)
    }
  }

  /**
   * Adds `params` as query parameters
   */
  def appendQueryParams(params: Iterable[(String, String)]): Req = {
    <<?(params)
  }
}

trait AuthVerbs extends RequestVerbs {
  def as(user: String, password: String, scheme: AuthScheme): Req =
    this.as(new Realm.Builder(user, password).setScheme(scheme).build())

  /** Basic auth, use with care. */
  def as_!(user: String, password: String): Req =
    this.as(new Realm.Builder(user, password)
      .setUsePreemptiveAuth(true)
      .setScheme(AuthScheme.BASIC)
      .build())

  def as(realm: Realm): Req = subject.setRealm(realm)

}

trait RequestBuilderVerbs extends RequestVerbs {
  import scala.collection.JavaConverters._

  /**
   * Add a new body part to the request.
   */
  def addBodyPart(part: Part): Req = {
    subject.underlying(_.addBodyPart(part))
  }

  /**
   * Add a new cookie to the request.
   */
  def addCookie(cookie: Cookie): Req = {
    subject.underlying(_.addCookie(cookie))
  }

  /**
   * Add a new header to the request.
   */
  def addHeader(name: String, value: String): Req = {
    subject.underlying(_.addHeader(name, value))
  }

  /**
   * Add a new body parameter to the request.
   */
  def addParameter(key: String, value: String): Req = {
    subject.underlying(_.addFormParam(key, value))
  }

  /**
   * Add a new query parameter to the request.
   */
  def addQueryParameter(name: String, value: String): Req = {
    subject.underlying(_.addQueryParam(name, value))
  }

  /**
   * Set query parameters, overwriting any pre-existing query parameters.
   */
  def setQueryParameters(params: Map[String, Seq[String]]): Req = {
    subject.underlying(_.setQueryParams(params.iterator.map { case (k, v) => k -> v.toList.asJava } .toMap.asJava))
  }

  /**
   * Set the request body from a byte array.
   */
  def setBody(data: Array[Byte]): Req = {
    subject.underlying(rb => rb.setBody(data), p => p.copy(bodyType = Req.ByteArrayBody))
  }

  /**
   * Set the request body using a BodyGenerator and length.
   */
  def setBody(dataWriter: BodyGenerator, length: Long): Req = {
    subject.underlying(rb => rb.setBody(dataWriter), p => p.copy(bodyType = Req.EntityWriterBody))
  }

  /**
   * Set the request body using a BodyGenerator.
   */
  def setBody(dataWriter: BodyGenerator): Req = {
    subject.underlying(rb => rb.setBody(dataWriter), p => p.copy(bodyType = Req.EntityWriterBody))
  }

  /**
   * Set the request body using a string.
   */
  def setBody(data: String): Req = {
    subject.underlying(rb => rb.setBody(data), p => p.copy(bodyType = Req.StringBody))
  }

  /**
   * Set the request body to the contents of a File.
   */
  def setBody(file: java.io.File): Req = {
    subject.underlying(rb => rb.setBody(file), p => p.copy(bodyType = Req.FileBody))
  }

  /**
   * Set the body encoding to the specified charset.
   */
  def setBodyEncoding(charset: Charset): Req = {
    subject.underlying(_.setCharset(charset))
  }

  /**
   * Set the content type and charset for the request.
   */
  def setContentType(mediaType: String, charset: Charset): Req = {
    subject.underlying {
      _.setHeader("Content-Type", mediaType + "; charset=" + charset).
      setCharset(charset)
    }
  }

  /**
   * Set a header
   */
  def setHeader(name: String, value: String): Req = {
    subject.underlying(_.setHeader(name, value))
  }

  /**
   * Set multiple headers
   */
  def setHeaders(headers: Map[String, Seq[String]]): Req = {
    subject.underlying {
      val httpHeaders: HttpHeaders = new DefaultHttpHeaders()
      headers.foreach(h => httpHeaders.add(h._1, h._2.asJava))
      _.setHeaders(httpHeaders)
    }
  }

  /**
   * Set form parameters
   */
  def setParameters(parameters: Map[String, Seq[String]]): Req = {
    subject.underlying(_.setFormParams(
      parameters.iterator.map { case (k, v) => k -> (v.asJava: java.util.List[String]) } .toMap.asJava
    ))
  }

  private[this] var methodExplicitlySet: Boolean = false

  /**
   * Explicitly set the method of the request.
   */
  def setMethod(method: String): Req = {
    methodExplicitlySet = true
    subject.underlying(_.setMethod(method))
  }

  /**
   * Set method unless method has been explicitly set using [[setMethod]].
   */
  def implyMethod(method: String): Req = {
    if (! methodExplicitlySet) {
      subject.underlying(_.setMethod(method))
    } else {
      subject
    }
  }

  /**
   * Set the url of the request.
   */
  def setUrl(url: String): Req = {
    subject.underlying(_.setUrl(url))
  }

  /**
   * Set the proxy server for the request
   */
  def setProxyServer(proxyServer: ProxyServer): Req = {
    subject.underlying(_.setProxyServer(proxyServer))
  }

  /**
   * Set the virtual hostname
   */
  def setVirtualHost(virtualHost: String): Req = {
    subject.underlying(_.setVirtualHost(virtualHost))
  }

  /**
   * Set the follow redirects setting
   */
  def setFollowRedirects(followRedirects: Boolean): Req = {
    subject.underlying(_.setFollowRedirect(followRedirects))
  }

  /**
   * Add ore replace a cookie
   */
  def addOrReplaceCookie(cookie: Cookie): Req = {
    subject.underlying(_.addOrReplaceCookie(cookie))
  }

  /**
   * Set auth realm
   */
  def setRealm(realm: Realm): Req = {
    subject.underlying(_.setRealm(realm))
  }
}
