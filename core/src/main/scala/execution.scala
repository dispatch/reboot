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

  /**
   * Returns a new instance replacing the underlying `client` with a new instance that is configured
   * using the `withBuilder` provided. The current client config is the builder's prototype.
   *
   * As of Dispatch 0.12.2, it is recommended that you use [[closeAndConfigure]] instead to prevent
   * the automatic resource link that using this method will cause. However, if you expect to be
   * able to ''continue'' using this Http instance after
   *
   * In Dispatch 0.13.x, this will be changed such that it only causes a resource link if you've
   * actually used the Http client, but the method is still deprecated and is one that we're
   * planning to remove. If you need this functionality in the long term, it is recommended that you
   * change your code to invoke the `.copy` method on the `Http` case class directly.
   */
  @deprecated("This method is deprecated and will be removed in a future version of dispatch. This method is known to cause a resource leak in Dispatch 0.12.x. If you don't need to continue using the original Http instance after invoking this, you should switch to using closeAndConfigure.", "0.12.2")
  def configure(withBuilder: Builder => Builder): Http = {
    unsafeConfigure(withBuilder)
  }

  // Internal, unsafe method that wraps the previous behavior of configure s othat we can invoke
  // it from closeAndConfigure without triggering our own deprecation warning.
  private[this] def unsafeConfigure(withBuilder: Builder => Builder): Http ={
    copy(client =
      new AsyncHttpClient(withBuilder(
        new AsyncHttpClientConfig.Builder(client.getConfig)
      ).build)
    )
  }

  /**
   * Returns a new instance replacing the underlying `client` with a new instance that is configured
   * using the `withBuilder` provided. The current client config is the builder's prototype. The
   * underlying client for this instance is closed before the new instance is created.
   */
  def closeAndConfigure(withBuilder: Builder => Builder): Http = {
    client.close()
    unsafeConfigure(withBuilder)
  }
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
