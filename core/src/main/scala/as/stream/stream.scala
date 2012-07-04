package dispatch.as.stream

import util.control.Exception._

import scala.collection.JavaConverters._
import com.ning.http.client._
import com.ning.http.util.AsyncHttpProviderUtils.parseCharset

object Lines {
  def apply[T](f: String => T) =
    new StreamStringByLine[T] {
      @volatile private var last: T = _
      def onStringBy(string: String) {
        last = f(string)
      }
      def onCompleted = last
    }
}

trait StreamString[T] extends AsyncHandler[T] {
  private var charset = "iso-8859-1"
  def onThrowable(t: Throwable) { }
  def onCompleted(): T
  def onStatusReceived(status: HttpResponseStatus) =
    AsyncHandler.STATE.CONTINUE
  def onHeadersReceived(headers: HttpResponseHeaders) = {
    for {
      ct <- headers.getHeaders.get("content-type").asScala.headOption
      cs <- Option(parseCharset("charset"))
    } charset = cs
    AsyncHandler.STATE.CONTINUE
  }
  def onString(str: String)
  def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
    onString(new String(bodyPart.getBodyPartBytes, charset))
    AsyncHandler.STATE.CONTINUE
  }
}

trait StreamStringBy[T] extends StreamString[T] {
  @volatile private var buffer = ""

  def divider: String

  def onStringBy(string: String)

  def onString(string: String) {
    val strings = (buffer + string).split(divider, -1)
    strings.take(strings.length - 1).filter { !_.isEmpty }.foreach(onStringBy)
    buffer = strings.last
  }
}

trait StreamStringByLine[T] extends StreamStringBy[T] {
  def divider = "[\n\r]+"
}

