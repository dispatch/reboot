package dispatch.spec

trait DispatchCleanup extends unfiltered.spec.ServerCleanup {
  implicit def executor = dispatch.Defaults.executor

  override def cleanup() = {
    super.cleanup()
    dispatch.Http.default.shutdown()
  }
}
