package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}

trait Promise[A] { self =>
  def get: A
  def filter(p: A => Boolean) =
    new Promise[A] {
      def get = {
        val r = self.get
        if (p(r)) r
        else throw new java.util.NoSuchElementException("Empty Promise.get")
      }
      def foreach(f: A => Unit) =
        for (a <- self)
          if (p(a)) f(a)
    }
  def map[B](f: A => B) =
    new Promise[B] {
      def get = f(self.get)
      def foreach(f2: B => Unit) =
        for (a <- self)
          f2(f(a))
    }
  def flatMap[B](f: A => Promise[B]) =
    new Promise[B] {
      def get = f(self.get).get
      def foreach(f2: B => Unit) =
        for {
          a <- self
          b <- f(a)
        } f2(b)
    }
  /** Promise to asynchronously cause some side effect */
  def foreach(f: A => Unit)
}

class ListenableFuturePromise[A](
  underlying: ListenableFuture[A],
  executor: juc.Executor
) extends Promise[A] {
  def get = underlying.get
  def foreach(f: A => Unit) =
    underlying.addListener(new Runnable {
      def run { f(get) }
    }, executor)
}

object Promise {
  def make[A](underlying: ListenableFuture[A])
             (implicit executor: juc.Executor) =
    new ListenableFuturePromise(underlying, executor)
}
