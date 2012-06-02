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
  def OK [T](f: Response => T) =
    (builder.build(), new OkFunctionHandler(f))
  def > [T](f: Response => T) =
    (builder.build(), new FunctionHandler(f))
  def > [T](h: AsyncHandler[T]) =
    (builder.build(), h)
}

case class StatusCode(code: Int)
extends Exception("Unexpected response status: %d".format(code))

class FunctionHandler[T](f: Response => T) extends AsyncCompletionHandler[T] {
  def onCompleted(response: Response) = f(response)
}

class OkFunctionHandler[T](f: Response => T)
extends FunctionHandler[T](f) with OkHandler[T]

trait OkHandler[T] extends AsyncHandler[T] {
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
  def headers = As { r =>
    import collection.JavaConversions.{mapAsScalaMap, asScalaBuffer}
    (Map.empty[String, Set[String]].withDefaultValue(Set.empty) /:
      mapAsScalaMap(r.getHeaders)) {
      case (a,e) => a + (e match {
        case (k, v) => (k -> Set(asScalaBuffer(v):_*))
      })
    }
  }
  def file(file: java.io.File) =
    (new client.resumable.ResumableAsyncHandler with OkHandler[Nothing])
      .setResumableListener(
        new client.extra.ResumableRandomAccessFileListener(
          new java.io.RandomAccessFile(file, "rw")
        )
      )
}
