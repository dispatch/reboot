package dispatch

import com.ning.http.client.{
  RequestBuilder, Request, Response, AsyncCompletionHandler
}

class FunctionHandler[T](f: Response => T) extends AsyncCompletionHandler[T] {
  def onCompleted(response: Response) = f(response)
}

object As {
  def apply[T](f: Response => T) = f
  val string = As { _.getResponseBody }
  val bytes = As { _.getResponseBodyAsBytes }
}
