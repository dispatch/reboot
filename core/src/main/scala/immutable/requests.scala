package dispatch.immutable

import java.io.File
import dispatch.{IDNDomainHelpers, RawUri, UriEncode}
import com.ning.http.client.{FluentCaseInsensitiveStringsMap, FluentStringsMap, RequestBuilder}

object HttpRequest {
  def apply() = new HttpRequest
}

class HttpRequest(private[immutable] val request: RequestBuilder = new RequestBuilder()) {

  private[this] def url = request.build.getUrl()
  private[this] def withChange(f: RequestBuilder => RequestBuilder) = new HttpRequest(f(request))

  def setUrl(url: String): HttpRequest = {
    withChange(_.setUrl(url))
  }

  def setMethod(method: String): HttpRequest = {
    withChange(_.setMethod(method))
  }

  def setHost(host: String): HttpRequest = {
    lazy val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    withChange(_.setUrl("http://%s/".format(asciiSafeDomain)))
  }

  def setHostAndPort(host: String, port: Int): HttpRequest = {
    lazy val asciiSafeDomain = IDNDomainHelpers.safeConvert(host)
    withChange(_.setUrl("http://%s:%d/".format(asciiSafeDomain, port)))
  }

  // symbol? ::/ or :[ or...
  // def setPort(port: Int): HttpRequest

  def /(path: String) = addPath(path)
  def addPath(segment: String): HttpRequest = {
    val uri = RawUri(url)
    val encodedSegment = UriEncode.path(segment)
    val rawPath = uri.path.orElse(Some("/")).map {
      case u if u.endsWith("/") => u + encodedSegment
      case u => u + "/" + encodedSegment
    }
    withChange(_.setUrl(uri.copy(path = rawPath).toString))
  }
  // def removePath(path: String): HttpRequest

  // todo: test
  def <:<(headers: Traversable[(String, String)]): HttpRequest = addHeaders(headers)
  def addHeaders(headers: Traversable[(String, String)]): HttpRequest = {
    val headersMap = headers.foldLeft(new FluentCaseInsensitiveStringsMap) {
      case (acc, (key, value)) => acc.add(key, value)
    }
    withChange(_.setHeaders(headersMap))
  }
  // def removeHeaders(headers: Traversable[(String, String)]): HttpRequest

  def <<?(params: Traversable[(String, String)]): HttpRequest = addQueryPrams(params)
  def addQueryPrams(params: Traversable[(String, String)]): HttpRequest = {
    val paramsMap = params.foldLeft(new FluentStringsMap) {
      case (acc, (key, value)) => acc.add(key, value)
    }
    withChange(_.setQueryParameters(paramsMap))
  }
  // def removeQueryParam(params: Traversable[(String, String)]): HttpRequest

  // todo: Set the Verb to POST
  def <<(params: Traversable[(String, String)]): HttpRequest = addPostParams(params)
  def addPostParams(params: Traversable[(String, String)]): HttpRequest = {
    val paramsMap = params.foldLeft(new FluentStringsMap) {
      case (acc, (key, value)) => acc.add(key, value)
    }
    withChange(_.setMethod("POST").setParameters(paramsMap))
  }
  // def removePostParams(params: Traversable[(String, String)]): HttpRequest

  // todo: Set the Verb to PUT
  def <<<(body: String): HttpRequest = addPutBody(body)
  def addPutBody(body: String): HttpRequest = {
    withChange(_.setMethod("PUT").setBody(body))
  }

  def <<<(body: File): HttpRequest = addPutBody(body)
  def addPutBody(body: File): HttpRequest = {
    withChange(_.setMethod("PUT").setBody(body))
  }

  // todo: Merge requests
  // symbol: <&
  // def <&(other: HttpRequest): HttpRequest

  // todo: Add cookies?

  // todo: test
  def secure: HttpRequest = {
    withChange(_.setUrl(RawUri(url).copy(scheme = Some("https")).toString))
  }

  // todo: test
  def isSecure(): Boolean = url.startsWith("https")
}
