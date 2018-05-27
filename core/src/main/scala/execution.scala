package dispatch

import java.util.{concurrent => juc}

import org.asynchttpclient._

import scala.concurrent.ExecutionContext
import scala.util.Try

/** Http executor with defaults */
case class Http(
  clientBuilder: DefaultAsyncHttpClientConfig.Builder
) extends HttpExecutor {
  import DefaultAsyncHttpClientConfig.Builder

  lazy val client = new DefaultAsyncHttpClient(clientBuilder.build)

  private[this] def unsafeConfigure(withBuilder: Builder => Builder): Http = {
    val newBuilder = new Builder(this.clientBuilder.build)
    copy(clientBuilder = withBuilder(newBuilder))
  }

  /**
   * Returns a new instance replacing the underlying `clientBuilder` with a new instance that is
   * configured using the `withBuilder` provided. The underlying client for this instance is closed
   * before the new instance is created in order to avoid resource leaks.
   */
  def closeAndConfigure(withBuilder: Builder => Builder): Http = {
    client.close()
    unsafeConfigure(withBuilder)
  }
}

/**
 * Singleton helper for vending Http instances.
 *
 * In past versions of Dispatch, this singleon was, itself, an Http executor. That could lead to
 * a few code traps were it was possible to unintentionally allocate additional Http pools without
 * realizing it because `Http.xxxx` and `Http().xxxx` both look very similar - yet do very different
 * things.
 *
 * In the interest of avoiding such code traps in future releases of Dispatch, `Http` was changed
 * to a helper in 0.13.x that is capable of vending a default `Http` executor instance or
 * of configuring a custom one with its `withConfiguration` method.
 *
 * If you relied on the default `Http` instance in your code you can easily port
 * your code to 0.13.x by simply invoking the `Http.default` method. Such as...
 *
 * {{{
 * Http.default(localhost / "split" << Seq("str" -> str) > as.String)
 * }}}
 */
object Http {
  import DefaultAsyncHttpClientConfig.Builder

  val defaultClientBuilder = InternalDefaults.clientBuilder

  /**
   * The default executor.
   */
  lazy val default: Http = {
    Http(defaultClientBuilder)
  }

  /**
   * Create an Http executor with some custom configuration defined by the `withBuilder` function
   * that will mutate the underlying `Builder` from AHC.
   */
  def withConfiguration(withBuilder: Builder => Builder): Http = {
    val newBuilder = new Builder(defaultClientBuilder.build)
    Http(withBuilder(newBuilder))
  }
}

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
