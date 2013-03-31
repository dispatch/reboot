package dispatch.spec

import org.scalacheck._

object RetrySpecification
extends Properties("Failure Handling")
with DispatchCleanup {
  import Prop._
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
  import Http.promise

  def localhost = host("127.0.0.1", server.port)

  // wrapping num in Option because scalacheck is
  // determined to test 0 if the type is int
  val smallNums = Gen.choose(0 , 10)

  class RetryCounter {
    private val retried = new java.util.concurrent.atomic.AtomicInteger
    def succeedOn(successRetry: Int)() = {
      Http(localhost << Map("echo" -> retried.getAndIncrement.toString)
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

  property("succeed on the max retry") = forAll(smallNums) { maxRetries =>
    val rc = new RetryCounter
    val p = retry.Directly(maxRetries)(rc.succeedOn(maxRetries))
    p() ?= Right(maxRetries)
  }

  property("fail after max retries") = forAll(smallNums) { maxRetries =>
    val rc = new RetryCounter
    val p = retry.Directly(maxRetries)(rc.succeedOn(maxRetries + 1))
    p() ?= Left(maxRetries + 1)
  }
}
