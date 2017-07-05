package dispatch.as

import dispatch._

import org.asynchttpclient
import org.asynchttpclient.handler.resumable._
import java.io._
import java.nio.charset.Charset

object Response {
  def apply[T](f: asynchttpclient.Response => T) = f
}

object String extends (asynchttpclient.Response => String) {
  /** @return response body as a string decoded as either the charset provided by
   *  Content-Type header of the response or ISO-8859-1 */
  def apply(r: asynchttpclient.Response) = r.getResponseBody

  /** @return a function that will return response body decoded in the provided charset */
  case class charset(set: Charset) extends (asynchttpclient.Response => String) {
    def apply(r: asynchttpclient.Response) = r.getResponseBody(set)
  }

  /** @return a function that will return response body as a utf8 decoded string */
  object utf8 extends charset(Charset.forName("utf8"))
}

object Bytes extends (asynchttpclient.Response => Array[Byte]) {
  def apply(r: asynchttpclient.Response) = r.getResponseBodyAsBytes
}

object File extends {
  def apply(file: java.io.File) = {
    val fileHandler = new RandomAccessFile(file, "rw")

    val resumableHandler = new ResumableAsyncHandler
        with OkHandler[asynchttpclient.Response]
        with CloseResourcesOnThrowableHandler[asynchttpclient.Response] {
      override lazy val closeable = Seq(fileHandler)
    }

    resumableHandler
      .setResumableListener(new ResumableRandomAccessFileListener(fileHandler))
  }
}
