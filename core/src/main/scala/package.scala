import com.ning.http.client.RequestBuilder

package object dispatch {
  /** Type alias for Response, avoid need to import */
  type Res = com.ning.http.client.Response
  /** Type alias for URI, avoid need to import */
  type Uri = java.net.URI

  @deprecated("Use `RequestBuilder.underlying` to preserve referential transparency",
    since="0.11.0")
  implicit def implyRequestBuilder(req: Req) = req.toRequestBuilder

  @deprecated("Use `RequestBuilder.underlying` to preserve referential transparency",
    since="0.11.0")
  implicit def implyReq(builder: RequestBuilder) = Req(_ => builder)

  implicit class DefaultRequestVerbs(val subject: Req)
  extends MethodVerbs with UrlVerbs with ParamVerbs with AuthVerbs
  with HeaderVerbs

  /**
   * Builds tuples of (Request, AsyncHandler) for passing to Http#apply.
   */
  implicit class RequestHandlerTupleBuilder(req: Req) {
    import com.ning.http.client.{AsyncHandler,Response}
    def OK [T](f: Response => T) =
      (req.toRequest, new OkFunctionHandler(f))
    def > [T](f: Response => T) =
      (req.toRequest, new FunctionHandler(f))
    def > [T](h: AsyncHandler[T]) =
      (req.toRequest, h)
  }

  implicit class RunnableFunction[U](f: () => U) extends java.lang.Runnable {
    def run() { f() }
  }

  implicit class ImplicitEnrichedFuture[T](future: Future[T])
  extends EnrichedFuture(future)

  @deprecated("use dispatch.Future / scala.concurrent.Future", "0.10.0")
  type Promise[+T] = scala.concurrent.Future[T]

  /** Type alias to scala.concurrent.Future so you don't have to import */
  type Future[+T] = scala.concurrent.Future[T]

  val Future = scala.concurrent.Future
}
