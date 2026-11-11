package dispatch.stream

import java.nio.charset.Charset
import java.util.regex.Pattern

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import org.asynchttpclient._
import org.asynchttpclient.util.HttpUtils

trait Strings[T] extends AsyncHandler[T] {
  import AsyncHandler.State._

  @volatile private var charset = Charset.forName("iso-8859-1")
  @volatile private var state = CONTINUE

  def onThrowable(t: Throwable) = { }
  def onCompleted(): T
  def onStatusReceived(status: HttpResponseStatus) = state
  def onHeadersReceived(headers: HttpHeaders) = {
    for {
      ct <- Option(headers.get(CONTENT_TYPE))
      cs <- Option(HttpUtils.extractContentTypeCharsetAttribute(ct))
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

/**
 * Thrown by [[StringsBy]] when a line, the text between two dividers, is
 * longer than `maxLineLength` characters. Throwing it from the handler fails
 * the request and closes the connection.
 */
case class LineTooLong(maxLineLength: Int)
extends Exception("Line exceeded the maximum length of %d characters".format(maxLineLength))

object StringsBy {
  /** The default value of [[StringsBy.maxLineLength]]: 1,048,576 characters. */
  val DefaultMaxLineLength: Int = 1024 * 1024
}

trait StringsBy[T] extends Strings[T] {
  @volatile private var buffer = ""

  def divider: String

  /**
   * The longest line, in characters, this handler accepts. A longer line,
   * including one still waiting for its divider, fails the request with
   * [[LineTooLong]]. Override to change the limit.
   */
  def maxLineLength: Int = StringsBy.DefaultMaxLineLength

  def onStringBy(string: String): Unit

  def onString(string: String) = {
    val strings = (buffer + string).split(divider, -1)
    strings.init.foreach { line =>
      checkLength(line.length)
      if (!line.isEmpty) onStringBy(line)
    }
    val pending = strings.last
    if (pending.length > maxLineLength) checkLength(pendingLineLength(pending))
    buffer = pending
  }

  private def checkLength(length: Int): Unit =
    if (length > maxLineLength) throw LineTooLong(maxLineLength)

  /**
   * The length of the line in `pending`, which holds no complete divider, not
   * counting a trailing partial divider that the next chunk may complete. That
   * is the first index at which the divider matches or reaches the end of the
   * input while matching.
   */
  private def pendingLineLength(pending: String): Int = {
    val matcher = Pattern.compile(divider).matcher(pending)
      .useTransparentBounds(true)
      .useAnchoringBounds(false)
    (0 until pending.length).find { start =>
      matcher.region(start, pending.length)
      matcher.lookingAt() || matcher.hitEnd()
    }.getOrElse(pending.length)
  }
}

trait StringsByLine[T] extends StringsBy[T] {
  def divider = "[\n\r]+"
}
