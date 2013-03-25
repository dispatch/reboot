import scala.concurrent.Future
import scala.concurrent.duration.Duration

package object dispatch {
  /** Type alias for RequestBuilder, our typical request definitions */
  type Req = com.ning.http.client.RequestBuilder
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

  implicit val durationOrdering = Ordering.by[Duration,Long] {
    _.toMillis
  }

  implicit def enrichFuture[T](future: Future[T]) =
    new EnrichedFuture(future)
}
