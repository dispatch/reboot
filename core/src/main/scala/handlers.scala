package dispatch

import java.io.Closeable
import org.asynchttpclient.{
  Response, AsyncCompletionHandler, AsyncHandler,
  HttpResponseStatus
}

/**
 * Builds tuples of (Request, AsyncHandler) for passing to Http#apply.
 * Implied in dispatch package object
 */
class RequestHandlerTupleBuilder(req: Req) {
  def OK [T](f: Response => T) =
    (req.toRequest, new OkFunctionHandler(f))
  def > [T](f: Response => T) =
    (req.toRequest, new FunctionHandler(f))
  def > [T](h: AsyncHandler[T]) =
    (req.toRequest, h)
}

case class StatusCode(code: Int)
extends Exception("Unexpected response status: %d".format(code))

/** This class is not thread safe. A new instance should be used for each callback */
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

/**
 * A trait to ensure some set of closeable resources are closed in the event of a throwable occuring
 * during a request. This can be combined with other AsyncHandlers, for example the OkHandler
 * provided by Dispatch, to ensure that any closeable resources are cleanly shut down in the event
 * of an exception.
 *
 * See the implementation of [[dispatch.as.File]] for an example of how this is used.
 */
trait CloseResourcesOnThrowableHandler[T] extends AsyncHandler[T] {
  def closeable: Seq[Closeable]

  abstract override def onThrowable(error: Throwable): Unit = {
    closeable.foreach(_.close())
    super.onThrowable(error)
  }
}
