import com.ning.http.client.RequestBuilder

package object dispatch {
  case class Req(run:RequestBuilder => RequestBuilder){
    def underlying(next:RequestBuilder => RequestBuilder) = Req(run andThen next)
    def build() = run(new RequestBuilder).build()

    def ++ (next: Req) = Req(run andThen next.run)
  }
  /** Type alias for Response, avoid need to import */
  type Res = com.ning.http.client.Response
  /** Type alias for URI, avoid need to import */
  type Uri = java.net.URI

  implicit def implyRequestVerbs(builder: Req) =
    new DefaultRequestVerbs(builder)

  implicit def implyRequestHandlerTuple(builder: Req) =
    new RequestHandlerTupleBuilder(builder)

  implicit def implyRunnable[U](f: () => U) = new java.lang.Runnable {
    def run() { f() }
  }

  implicit def enrichFuture[T](future: Future[T]) =
    new EnrichedFuture(future)

  @deprecated("use dispatch.Future / scala.concurrent.Future", "0.10.0")
  type Promise[+T] = scala.concurrent.Future[T]

  /** Type alias to scala.concurrent.Future so you don't have to import */
  type Future[+T] = scala.concurrent.Future[T]

  val Future = scala.concurrent.Future
}
