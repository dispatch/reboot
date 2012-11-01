package dispatch.stream

import com.ning.http.client._
import com.ning.http.util.AsyncHttpProviderUtils.parseCharset

trait Strings[T] extends AsyncHandler[T] {
  import AsyncHandler.STATE._

  @volatile private var charset = "iso-8859-1"
  @volatile private var state = CONTINUE

  def onThrowable(t: Throwable) { }
  def onCompleted(): T
  def onStatusReceived(status: HttpResponseStatus) = state
  def onHeadersReceived(headers: HttpResponseHeaders) = {
    for {
      ct <- Option(headers.getHeaders.getFirstValue("content-type"))
      cs <- Option(parseCharset(ct))
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
  def stop() {
    state = ABORT
  }
}

trait StringsBy[T] extends Strings[T] {
  @volatile private var buffer = ""

  def divider: String

  def onStringBy(string: String)

  def onString(string: String) {
    val strings = (buffer + string).split(divider, -1)
    strings.take(strings.length - 1).filter { !_.isEmpty }.foreach(onStringBy)
    buffer = strings.last
  }
}

trait StringsByLine[T] extends StringsBy[T] {
  def divider = "[\n\r]+"
}

