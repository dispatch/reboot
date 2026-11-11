package dispatch.spec

import java.util.concurrent.ConcurrentLinkedQueue

import org.scalacheck._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Try}

object StreamSpecification
extends Properties("Stream")
with DispatchCleanup {
  import Prop._

  import dispatch._
  import dispatch.stream.{LineTooLong, StringsBy, StringsByLine}

  private val defaultLimit = StringsBy.DefaultMaxLineLength

  private val port = unfiltered.util.Port.any
  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    netty.Server.local(port).handler(netty.cycle.Planify {
      case Path("/lines") =>
        PlainTextContent ~> ResponseString("one\ntwo\r\nthree\n")
      case Path("/noline") =>
        PlainTextContent ~> ResponseString("a" * (defaultLimit + 1))
    }).start()
  }

  val localhost = host("127.0.0.1", port)

  /** A line handler with a small limit that records the lines it receives. */
  private class Recorder(limit: Int) extends StringsByLine[Unit] {
    val lines = new ConcurrentLinkedQueue[String]
    override def maxLineLength = limit
    def onStringBy(string: String) = { lines.add(string); () }
    def onCompleted() = ()
    def received = lines.asScala.toList
  }

  /** A [[Recorder]] whose divider is two characters long. */
  private class PairRecorder(limit: Int) extends Recorder(limit) {
    override def divider = "::"
  }

  /** A [[Recorder]] that splits on `pattern`. */
  private class PatternRecorder(limit: Int, pattern: String) extends Recorder(limit) {
    override def divider = pattern
  }

  property("Lines splits a response into lines") = {
    val lines = new ConcurrentLinkedQueue[String]
    val res = Http.default(localhost / "lines" > as.stream.Lines(lines.add))
    res()
    lines.asScala.toList ?= List("one", "two", "three")
  }

  property("Lines rejects a line longer than the default limit") = {
    val handler = as.stream.Lines(_ => ())
    val chunk = "a" * 8192
    val result = Try {
      (1 to (defaultLimit / chunk.length + 1)).foreach(_ => handler.onString(chunk))
    }
    result ?= Failure(LineTooLong(defaultLimit))
  }

  property("Lines fails the request when a line is longer than the default limit") = {
    val res = Http.default(localhost / "noline" > as.stream.Lines(_ => ())).either
    res() ?= Left(LineTooLong(defaultLimit))
  }

  property("A line at the limit is accepted across chunks") = {
    val handler = new Recorder(4)
    handler.onString("ab")
    handler.onString("cd")
    handler.onString("\nef\n")
    handler.received ?= List("abcd", "ef")
  }

  property("A partial line over the limit is rejected") = {
    val handler = new Recorder(4)
    handler.onString("ab\nabc")
    val result = Try(handler.onString("de"))
    (result ?= Failure(LineTooLong(4))) && (handler.received ?= List("ab"))
  }

  property("A divider split across chunks does not count toward the limit") = {
    val handler = new PairRecorder(4)
    handler.onString("abcd:")
    handler.onString(":ef::")
    handler.received ?= List("abcd", "ef")
  }

  property("A partial divider that is not completed counts toward the limit") = {
    val handler = new PairRecorder(4)
    handler.onString("abcd:")
    val result = Try(handler.onString("x"))
    (result ?= Failure(LineTooLong(4))) && (handler.received ?= Nil)
  }

  property("A partial divider of unbounded length is accepted within the limit") = {
    val handler = new PatternRecorder(4, "\\s*\\n")
    handler.onString("abcd  ")
    handler.onString("  \nef\n")
    handler.received ?= List("abcd", "ef")
  }

  property("A partial divider of unbounded length cannot grow past the limit") = {
    val handler = new PatternRecorder(4, "\\s*\\n")
    val result = Try((1 to 10).foreach(_ => handler.onString("   ")))
    (result ?= Failure(LineTooLong(4))) && (handler.received ?= Nil)
  }

  property("A zero-width divider cannot grow a line past the limit") = {
    val handler = new PatternRecorder(4, "(?=\\{)")
    handler.onString("{ab}{cd")
    val result = Try(handler.onString("ef"))
    (result ?= Failure(LineTooLong(4))) && (handler.received ?= List("{ab}"))
  }

  property("A complete line over the limit is rejected") = {
    val handler = new Recorder(4)
    val result = Try(handler.onString("ab\nabcde\ncd\n"))
    (result ?= Failure(LineTooLong(4))) && (handler.received ?= List("ab"))
  }
}
