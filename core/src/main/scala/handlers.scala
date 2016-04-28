package dispatch

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
