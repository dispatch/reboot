package dispatch.immutable

import dispatch.{IDNDomainHelpers, RawUri, UriEncode}
import com.ning.http.client.{FluentStringsMap, RequestBuilder}

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

  // symbol: :/
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

  // symbol: /
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

  // symbol: <:<
  // def addHeaders(headers: Traversable[(String, String)]): HttpRequest
  // def removeHeaders(headers: Traversable[(String, String)]): HttpRequest

  // symbol: <<?
  def <<?(params: Traversable[(String, String)]): HttpRequest = addQueryPrams(params)
  def addQueryPrams(params: Traversable[(String, String)]): HttpRequest = {
    val paramsMap = params.foldLeft(new FluentStringsMap) {
      case (acc, (key, value)) => acc.add(key, value)
    }
    withChange(_.setQueryParameters(paramsMap))
  }
  // def removeQueryParam(params: Traversable[(String, String)]): HttpRequest

  // todo: Set the Verb to POST
  // symbol: <<
  // def addPostParams(params: Traversable[(String, String)]): HttpRequest
  // def removePostParams(params: Traversable[(String, String)]): HttpRequest

  // todo: Set the Verb to PUT
  // symbol: <<<
  // def addPutBody(body: String): HttpRequest
  // def addPutBody(body: File): HttpRequest

  // symbol: secure
  // def setSecure(): HttpRequest
  // def isSecure(): Boolean
}
