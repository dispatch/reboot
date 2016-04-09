package dispatch

import org.asynchttpclient._
import java.util.{concurrent => juc}

import scala.concurrent.ExecutionContext
import scala.util.Try

/** Http executor with defaults */
case class Http(
  client: AsyncHttpClient = InternalDefaults.client
) extends HttpExecutor {
  import DefaultAsyncHttpClientConfig.Builder

  /** Replaces `client` with a new instance configured using the withBuilder
      function. */
  def configure(withBuilder: Builder => Builder) =
    copy(client =
      new DefaultAsyncHttpClient(withBuilder(
        new DefaultAsyncHttpClientConfig.Builder(InternalDefaults.config)
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
      () => promise.complete(Try(lfut.get())),
      new juc.Executor {
        def execute(runnable: Runnable) = {
          executor.execute(runnable)
        }
      }
    )
    promise.future
  }

  def shutdown() = {
    client.close()
  }
}

