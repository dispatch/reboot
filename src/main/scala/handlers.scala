package dispatch

import com.ning.http.client
import client.{
  RequestBuilder, Request, Response, AsyncCompletionHandler, AsyncHandler,
  HttpResponseStatus
}

/**
 * Builds tuples of (Request, AsyncHandler) for passing to Http#apply.
 * Implied in dispatch package object
 */
class RequestHandlerTupleBuilder(builder: RequestBuilder) {
  def > [T](f: Response => T) =
    (builder.build(), new OkayFunctionHandler(f))
  def > [T](h: AsyncHandler[T]) =
    (builder.build(), h)
}

case class StatusCode(code: Int)
extends Exception("Unexpected response status: %d".format(code))

class FunctionHandler[T](f: Response => T) extends AsyncCompletionHandler[T] {
  def onCompleted(response: Response) = f(response)
}

class OkayFunctionHandler[T](f: Response => T)
extends FunctionHandler[T](f) with OkayHandler[T]

trait OkayHandler[T] extends AsyncHandler[T] {
  abstract override def onStatusReceived(status: HttpResponseStatus) = {
    if (status.getStatusCode / 100 == 2)
      super.onStatusReceived(status)
    else
      throw StatusCode(status.getStatusCode)
  }
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
