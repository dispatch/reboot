package dispatch

import com.ning.http.client.{
  AsyncHttpClient, Request, Response, AsyncHandler,
  AsyncHttpClientConfig
}

import java.util.{concurrent => juc}
import scala.concurrent.{ExecutionContext}

/** Http executor with defaults */
case class Http(
  client: AsyncHttpClient = InternalDefaults.client
) extends HttpExecutor {
  import AsyncHttpClientConfig.Builder

  /** Replaces `client` with a new instance configured using the withBuilder
      function. The current client config is the builder's prototype.  */
  def configure(withBuilder: Builder => Builder) =
    copy(client =
      new AsyncHttpClient(withBuilder(
        new AsyncHttpClientConfig.Builder(client.getConfig)
      ).build)
    )
}

/** Singleton default Http executor, can be used directly or altered
 *  with its case-class `copy` */
object Http extends Http(
  InternalDefaults.client
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

