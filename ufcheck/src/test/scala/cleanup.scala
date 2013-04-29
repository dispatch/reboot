package unfiltered.spec

trait Cleanup {
  Cleanup.dirties += this
  def cleanup()
}

trait ServerCleanup extends Cleanup {
  def server: unfiltered.util.StartableServer
  def cleanup() { server.stop() }
}

object Cleanup {
  import scala.collection.mutable.{HashSet,SynchronizedSet}
  private val dirties = new HashSet[Cleanup] with SynchronizedSet[Cleanup]

  def cleanup() {
    try {
      dirties.foreach { _.cleanup() }
    } catch {
      case e: Throwable =>
        println("Error on cleanup")
        e.printStackTrace
    }
  }
}
