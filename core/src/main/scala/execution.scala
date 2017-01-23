package dispatch

import com.ning.http.client.{
  AsyncHttpClient, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}

import java.util.{concurrent => juc}
import scala.concurrent.{ExecutionContext}

/** Http executor with defaults */
case class Http(
  builder: AsyncHttpClientConfig.Builder = InternalDefaults.builder
) extends HttpExecutor {
  import AsyncHttpClientConfig.Builder

  lazy val client = new AsyncHttpClient(builder.build)

  /** Replaces `builder` with a new config built using the withBuilder
      function. The current client config is the builder's prototype.  */
  def configure(withBuilder: Builder => Builder) = {
    val newBuilder = new Builder(this.builder.build)
    copy(builder = withBuilder(newBuilder))
  }
}

/** Singleton default Http executor, can be used directly or altered
 *  with its case-class `copy` */
object Http extends Http(
  InternalDefaults.builder
)

trait HttpExecutor { self =>
  def client: AsyncHttpClient

  def apply(req: Req)
           (implicit executor: ExecutionContext): Future[Response] =
    apply(req.toRequest -> new FunctionHandler(identity))

  def apply[T](pair: (Request, AsyncHandler[T]))
              (implicit executor: ExecutionContext): Future[T] =
    apply(pair._1, pair._2)

  def apply[T]
    (request: Request, handler: AsyncHandler[T])
    (implicit executor: ExecutionContext): Future[T] = {
    val lfut = client.executeRequest(request, handler)
    val promise = scala.concurrent.Promise[T]()
    lfut.addListener(
      () => promise.complete(util.Try(lfut.get())),
      new juc.Executor {
        def execute(runnable: Runnable) {
          executor.execute(runnable)
        }
      }
    )
    promise.future
  }

  def shutdown() {
    client.close()
  }
}

