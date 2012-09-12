package dispatch.as

import dispatch._

import com.ning.http.client

object Response {
  def apply[T](f: client.Response => T) = f
}

object String extends (client.Response => String) {
  def apply(r: client.Response) = r.getResponseBody
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
