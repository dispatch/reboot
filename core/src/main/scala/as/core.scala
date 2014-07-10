package dispatch.as

import dispatch._

import com.ning.http.client
import java.nio.charset.Charset

object Response {
  def apply[T](f: client.Response => T) = f
}

object String extends (client.Response => String) {
  /** @return response body as a ISO-8859-1 decoded string */
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
  def apply(file: java.io.File) =
    (new client.resumable.ResumableAsyncHandler with OkHandler[Nothing])
      .setResumableListener(
        new client.extra.ResumableRandomAccessFileListener(
          new java.io.RandomAccessFile(file, "rw")
        )
      )
}
