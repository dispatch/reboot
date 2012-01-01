package dispatch

import com.ning.http.client.{
  RequestBuilder, Request, Response, AsyncCompletionHandler
}

trait ImplicitHandlerVerbs {
  implicit def implyHandlerVerbs(builder: RequestBuilder) =
    new DefaultHandlerVerbs(builder.build)
}

class DefaultHandlerVerbs(val subject: Request)
extends StringVerbs

trait HandlerVerbs {
  def subject: Request
  def onCompleted[T](f: Response => T) =
    Executable(
      subject,
      () => new AsyncCompletionHandler[T] {
        def onCompleted(response: Response) = f(response)
      }
    )
}

trait StringVerbs extends HandlerVerbs {
  def >- [T](f: String => T) = onCompleted { res =>
    f(res.getResponseBody("iso-8859-1"))
  }
  def as_str = >- (identity)
}
