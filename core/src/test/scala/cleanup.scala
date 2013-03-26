package dispatch.spec

trait DispatchCleanup extends unfiltered.spec.ServerCleanup {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global
  override def cleanup() {
    super.cleanup()
    dispatch.Http.shutdown()
  }
}
