package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}

trait Promise[A] { self =>
  def claim: A
  def map[B](f: A => B) =
    new Promise[B] {
      def claim = f(self.claim)
      def foreach(f2: B => Unit) =
        for (a <- self)
          f2(f(a))
    }
  def flatMap[B](f: A => Promise[B]) =
    new Promise[B] {
      def claim = f(self.claim).claim
      def foreach(f2: B => Unit) =
        for {
          a <- self
          b <- f(a)
        } f2(b)
    }
  /** Promise to asynchronously cause some side effect */
  def foreach(f: A => Unit)
}

object Promise {
  def make[A](underlying: ListenableFuture[A])
             (implicit executor: juc.Executor) =
    new Promise[A] {
      def claim = underlying.get
      def foreach(f: A => Unit) =
        underlying.addListener(new Runnable {
          def run { f(claim) }
        }, executor)
    }
}
