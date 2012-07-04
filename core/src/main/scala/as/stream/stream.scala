package dispatch.as.stream

import util.control.Exception._

import scala.collection.JavaConverters._
import com.ning.http.client._
import com.ning.http.util.AsyncHttpProviderUtils.parseCharset

object Lines {
  def apply[T](f: String => T) =
    new StreamStringByLine[(String => T)] {
      def onStringBy(string: String) {
        f(string)
      }
      def onCompleted = f
    }
}

trait StreamString[T] extends AsyncHandler[T] {
  import AsyncHandler.STATE._

  @volatile private var charset = "iso-8859-1"
  @volatile private var state = CONTINUE

  def onThrowable(t: Throwable) { }
  def onCompleted(): T
  def onStatusReceived(status: HttpResponseStatus) = state
  def onHeadersReceived(headers: HttpResponseHeaders) = {
    for {
      ct <- headers.getHeaders.get("content-type").asScala.headOption
      cs <- Option(parseCharset("charset"))
    } charset = cs
    state
  }
  def onString(str: String)
  def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
    if (state == CONTINUE) {
      onString(new String(bodyPart.getBodyPartBytes, charset))
    }
    state
  }
  def abort() {
    state = ABORT
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

