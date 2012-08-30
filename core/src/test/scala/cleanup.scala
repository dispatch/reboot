package dispatch.spec

trait DispatchCleanup extends unfiltered.spec.ServerCleanup {
  override def cleanup() {
    super.cleanup()
    dispatch.Http.shutdown()
  }
}
