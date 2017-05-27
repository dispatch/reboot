package dispatch.spec

import org.scalacheck._

object RetrySpecification
extends Properties("Retry Handling")
with DispatchCleanup {
  import Prop.{forAll,AnyOperators}
  import Gen._

  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Http.anylocal.handler(netty.cycle.Planify {
      case Params(Echo(echo)) =>
        PlainTextContent ~> ResponseString(echo)
    }).start()
  }

  import dispatch._
  import scala.concurrent.duration.Duration
  import java.util.concurrent.TimeUnit

  import io.netty.util.{Timer, HashedWheelTimer}
  // We're using a very fine grained timer, and short retry intervals,
  // to keep the tests fast. These are unlikely to be good settings
  // for an application.
  implicit val timer: Timer =
    new HashedWheelTimer(1, TimeUnit.MILLISECONDS)

  val localhost = host("127.0.0.1", server.port)

  // wrapping num in Option because scalacheck is
  // determined to test 0 if the type is int
  val smallNums = Gen.choose(0, 10)

  class RetryCounter {
    private val retried = new java.util.concurrent.atomic.AtomicInteger
    def succeedOn(successRetry: Int)() = {
      Http.default(localhost << Map("echo" -> retried.getAndIncrement.toString)
           OK as.String).either.map { eth =>
        eth.right.flatMap { numstr =>
          val num = numstr.toInt
          if (num == successRetry)
            Right(num)
          else
            Left(num)
        }
      }
    }
  }

  property("succeed on the first request") = forAll(smallNums) { maxRetries =>
    val rc = new RetryCounter
    val p = retry.Backoff(maxRetries)(rc.succeedOn(0))
    p() ?= Right(0)
  }

  property("succeed on the max retry") = forAll(smallNums) { maxRetries =>
    val rc = new RetryCounter
    val p = retry.Directly(maxRetries)(rc.succeedOn(maxRetries))
    p() ?= Right(maxRetries)
  }

  property("fail after max retries") = forAll(smallNums) { maxRetries =>
    val rc = new RetryCounter
    val p = retry.Directly(maxRetries)(rc.succeedOn(maxRetries + 1))
    p() ?= Left(maxRetries)
  }

  property("succeed on the max backoff retry") = forAll(smallNums) { max =>
    val rc = new RetryCounter
    val p = retry.Backoff(
      max,
      Duration(2, TimeUnit.MICROSECONDS)
    )(rc.succeedOn(max))
    p() ?= Right(max)
  }

  property("fail after max pause retry") = forAll(smallNums) { max =>
    val rc = new RetryCounter
    val p = retry.Pause(
      max,
      Duration(500, TimeUnit.MICROSECONDS)
    )(rc.succeedOn(max + 1))
    p() ?= Left(max)
  }
}
