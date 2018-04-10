package unfiltered.spec

trait Cleanup {
  Cleanup.dirties += this
  def cleanup(): Unit
}

trait ServerCleanup extends Cleanup {
  def server: unfiltered.util.StartableServer
  def cleanup(): Unit = { server.stop() }
}

object Cleanup {
  import scala.collection.mutable.{HashSet,SynchronizedSet}
  private val dirties = new HashSet[Cleanup] with SynchronizedSet[Cleanup]

  def cleanup(): Unit = {
    try {
      dirties.foreach { _.cleanup() }
    } catch {
      case e: Exception =>
        println("Error on cleanup")
        e.printStackTrace
    }
  }
}
