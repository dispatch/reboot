package dispatch

import org.jboss.netty.util.{TimerTask, Timeout}

import java.util.{concurrent => juc}

class SleepPromise[T](
  val httpExecutor: HttpExecutor,
  d: Duration,
  todo: => T) extends Promise[T] { self =>
  private lazy val latch = new juc.CountDownLatch(1)
  def claim = {
    latch.await()
    todo
  }
  private lazy val listeners =
    new juc.atomic.AtomicReference(List.empty[(() => Unit)])
  val sleepTimeout = httpExecutor.timer.newTimeout(new TimerTask {
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
      httpExecutor.promiseExecutor.execute(new java.lang.Runnable {
        def run() { f() }
      })
    } else {
      if (! listeners.compareAndSet(ls, f :: ls)) {
        addListener(f)
      }
    }
  }
}
