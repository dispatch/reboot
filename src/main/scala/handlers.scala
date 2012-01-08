package dispatch

import com.ning.http.client
import client.{
  RequestBuilder, Request, Response, AsyncCompletionHandler, AsyncHandler
}

/**
 * Builds tuples of (Request, AsyncHandler) for passing to Http#apply.
 * Implied in dispatch package object
 */
class RequestHandlerTupleBuilder(builder: RequestBuilder) {
  def > [T](f: Response => T) =
    (builder.build(), new FunctionHandler(f))
  def > [T](h: AsyncHandler[T]) =
    (builder.build(), h)
}

class FunctionHandler[T](f: Response => T) extends AsyncCompletionHandler[T] {
  def onCompleted(response: Response) = f(response)
}

object As {
  def apply[T](f: Response => T) = f
  val string = As { _.getResponseBody }
  val bytes = As { _.getResponseBodyAsBytes }
  def file(file: java.io.File) =
    new client.resumable.ResumableAsyncHandler()
      .setResumableListener(
        new client.extra.ResumableRandomAccessFileListener(
          new java.io.RandomAccessFile(file, "rw")
        )
      )
}
