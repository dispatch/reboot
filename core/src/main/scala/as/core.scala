package dispatch.as

import dispatch._

import com.ning.http.client
import com.ning.http.client.resumable._
import com.ning.http.client.extra._
import java.io._
import java.nio.charset.Charset

object Response {
  def apply[T](f: client.Response => T) = f
}

object String extends (client.Response => String) {
  /** @return response body as a string decoded as either the charset provided by
   *  Content-Type header of the response or ISO-8859-1 */
  def apply(r: client.Response) = r.getResponseBody

  /** @return a function that will return response body decoded in the provided charset */
  case class charset(set: Charset) extends (client.Response => String) {
    def apply(r: client.Response) = r.getResponseBody(set.name)
  }

  /** @return a function that will return response body as a utf8 decoded string */
  object utf8 extends charset(Charset.forName("utf8"))
}

object Bytes extends (client.Response => Array[Byte]) {
  def apply(r: client.Response) = r.getResponseBodyAsBytes
}

object File extends {
  def apply(file: java.io.File) = {
    val fileHandler = new RandomAccessFile(file, "rw")

    val resumableHandler = new ResumableAsyncHandler[client.Response]
        with OkHandler[client.Response]
        with CloseResourcesOnThrowableHandler[client.Response] {
      override lazy val closeable = Seq(fileHandler)
    }

    resumableHandler
      .setResumableListener(new ResumableRandomAccessFileListener(fileHandler))
  }
}
