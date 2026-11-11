package dispatch.as

import dispatch._

import org.asynchttpclient
import org.asynchttpclient.handler.resumable._
import java.io._
import java.nio.charset.Charset
import java.io.Closeable
import io.netty.handler.codec.http.HttpHeaders

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

abstract class FileApply {
  /**
   * @return a handler that writes the response body to `file`, replacing any
   * existing content. The file is opened, and created if needed, only once a
   * successful status arrives. A non-2xx status fails the request with
   * [[dispatch.StatusCode]] and leaves the file as it was. A failure while the
   * body is streaming can leave a partially written file.
   */
  def apply(file: java.io.File): ResumableAsyncHandler =
    new ResumableAsyncHandler
        with OkHandler[asynchttpclient.Response]
        with CloseResourcesOnThrowableHandler[asynchttpclient.Response] {
      @volatile private var fileHandler: Option[RandomAccessFile] = None

      override def closeable: Seq[Closeable] = fileHandler.toSeq

      override def onStatusReceived(status: asynchttpclient.HttpResponseStatus): asynchttpclient.AsyncHandler.State = {
        val state = super.onStatusReceived(status)
        if (state == asynchttpclient.AsyncHandler.State.CONTINUE) {
          // Reuse the file if AHC replays the request through this handler, so
          // the first descriptor isn't leaked.
          val raf = fileHandler.getOrElse {
            val opened = new RandomAccessFile(file, "rw")
            fileHandler = Some(opened)
            opened
          }
          // The listener always writes at the end of the file, so empty it
          // first to replace the old content instead of appending to it.
          raf.setLength(0)
          setResumableListener(new ResumableRandomAccessFileListener(raf))
        }
        state
      }

      override def onTrailingHeadersReceived(headers: HttpHeaders): asynchttpclient.AsyncHandler.State = super[ResumableAsyncHandler].onTrailingHeadersReceived(headers)
    }
}

object File extends FileApply
