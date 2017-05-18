package dispatch.stream

import java.nio.charset.Charset

import org.asynchttpclient._
import org.asynchttpclient.util.HttpUtils

trait Strings[T] extends AsyncHandler[T] {
  import AsyncHandler.State._

  @volatile private var charset = Charset.forName("iso-8859-1")
  @volatile private var state = CONTINUE

  def onThrowable(t: Throwable) = { }
  def onCompleted(): T
  def onStatusReceived(status: HttpResponseStatus) = state
  def onHeadersReceived(headers: HttpResponseHeaders) = {
    for {
      ct <- Option(headers.getHeaders.get("content-type"))
      cs <- Option(HttpUtils.parseCharset(ct))
    } charset = cs
    state
  }
  def onString(str: String): Unit
  def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
    if (state == CONTINUE) {
      onString(new String(bodyPart.getBodyPartBytes, charset))
    }
    state
  }
  def stop() = {
    state = ABORT
  }
}

trait StringsBy[T] extends Strings[T] {
  @volatile private var buffer = ""

  def divider: String

  def onStringBy(string: String): Unit

  def onString(string: String) = {
    val strings = (buffer + string).split(divider, -1)
    strings.take(strings.length - 1).filter { !_.isEmpty }.foreach(onStringBy)
    buffer = strings.last
  }
}

trait StringsByLine[T] extends StringsBy[T] {
  def divider = "[\n\r]+"
}

