package dispatch.spec

import scala.concurrent.ExecutionContext

trait DispatchCleanup extends unfiltered.spec.ServerCleanup {
  implicit def executor: ExecutionContext = dispatch.Defaults.executor

  override def cleanup() = {
    super.cleanup()
    dispatch.Http.default.shutdown()
  }
}
