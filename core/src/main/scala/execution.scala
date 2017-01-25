package dispatch

import java.util.{concurrent => juc}

import org.asynchttpclient._

import scala.concurrent.ExecutionContext
import scala.util.Try

/** Http executor with defaults */
case class Http(
                 clientBuilder: DefaultAsyncHttpClientConfig.Builder = InternalDefaults.clientBuilder
               ) extends HttpExecutor {

  import DefaultAsyncHttpClientConfig.Builder

  lazy val client = new DefaultAsyncHttpClient(clientBuilder.build)

  /*§*
    * Replaces `clientBuilder` with a new config built using the withBuilder function.
    * The current client config is the builder's prototype.
    */
  def configure(withBuilder: Builder => Builder): Http = {
    val newBuilder = new Builder(this.clientBuilder.build)
    copy(clientBuilder = withBuilder(newBuilder))
  }
}

/** Singleton default Http executor, can be used directly or altered
  * with its case-class `copy` */
object Http extends Http(
  InternalDefaults.clientBuilder
)

trait HttpExecutor {
  self =>
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

