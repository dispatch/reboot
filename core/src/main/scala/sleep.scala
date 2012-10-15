package dispatch

import org.jboss.netty.util.{TimerTask, Timeout}

import java.util.{concurrent => juc}

class SleepPromise[T](d: Duration, todo: => T) extends Promise[T] { self =>
  private lazy val latch = new juc.CountDownLatch(1)
  def claim = {
    latch.await()
    todo
  }
  // note: super calls addListener before listeners can be initialized
  private lazy val listeners =
    new juc.atomic.AtomicReference(List.empty[(() => Unit)])
  val sleepTimeout = Http.timer.newTimeout(new TimerTask {
    def run(timeout: Timeout) {
      latch.countDown()
      val ls = listeners.getAndSet(Nil)
      for (l <- ls) l()
    }
  }, d.length, d.unit)
  def isComplete = latch.getCount == 0
  val timeout = d
  def addListener(f: () => Unit) {
    val ls = listeners.get
    if (isComplete) {
      f() // should really happen in a background thread
    } else {
      if (! listeners.compareAndSet(ls, f :: ls)) {
        addListener(f)
      }
    }
  }
}
