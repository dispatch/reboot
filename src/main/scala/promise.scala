package dispatch

import com.ning.http.client.ListenableFuture
import java.util.{concurrent => juc}
import scala.util.control.Exception.allCatch

trait Promise[+A] { self =>
  def getEither = allCatch.either(get)
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
  def flatMap[B,GA[_]](f: A => GA[Promise[B]])
                    (implicit guarantor: Guarantor[GA]) =
    new Promise[GA[B]] {
      def get = guarantor.promise(f(self.get)).get
      def foreach(f2: GA[B] => Unit) =
        for {
          a <- self
          b <- guarantor.promise(f(a))
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

  def all[A](promises: Traversable[Promise[A]]) =
    new Promise[Traversable[A]] { self =>
      def get = promises.map { _.get }
      def foreach(f: Traversable[A] => Unit) = {
        val count = new juc.atomic.AtomicInteger(promises.size)
        for (p <- promises; a <- p)
          if (count.decrementAndGet == 0)
            f(self.get)
      }
    }
}

trait Guarantor[A[_]] {
  def promise[T](underlying: A[Promise[T]]): Promise[A[T]]
}

object Guarantor {
  implicit val traversable =
    new Guarantor[Traversable] {
      def promise[T](underlying: Traversable[Promise[T]]) = sys.error("hi")
    }
}

object Test {
  import Guarantor.traversable
  def test(p1: Promise[Int], sp2: Traversable[Promise[Int]])
  :Promise[Traversable[Int]] =
    p1.flatMap { i1: Int =>
      sp2.map { p2 =>
        p2.map { i2 =>
          i1 + i2
        }
      }
    }
  def test2(p1: Promise[Int], sp2: Traversable[Promise[Int]])
  :Promise[Traversable[Int]] =
    for {
      i1: Int <- p1
      p2 <- sp2
    } yield p2
}
