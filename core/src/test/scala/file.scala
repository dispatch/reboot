package dispatch.spec

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path => NioPath}

import org.scalacheck._

object FileSpecification
extends Properties("as.File")
with DispatchCleanup {
  import Prop.{forAllNoShrink, AnyOperators}

  private val port = unfiltered.util.Port.any
  val server = {
    import unfiltered.netty
    import unfiltered.response._
    import unfiltered.request._
    object Echo extends Params.Extract("echo", Params.first)
    netty.Server.local(port).handler(netty.cycle.Planify {
      case Path("/echo") & Params(Echo(echo)) =>
        PlainTextContent ~> ResponseString(echo)
      case Path("/missing") =>
        NotFound ~> ResponseString("not found")
    }).start()
  }

  import dispatch._

  val localhost = host("127.0.0.1", port)

  private val nonEmptyAlphaStr =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)

  /** Runs `f` with a path inside a fresh temp directory. The path does not
    * exist yet. Everything `f` needs from the filesystem must be read before it
    * returns, because the directory is deleted afterwards. */
  private def withTarget[A](f: NioPath => A): A = {
    val dir = Files.createTempDirectory("dispatch-as-file")
    val target = dir.resolve("download")
    try f(target)
    finally {
      Files.deleteIfExists(target)
      Files.delete(dir)
    }
  }

  private def write(path: NioPath, content: String): Unit = {
    Files.write(path, content.getBytes(UTF_8))
    ()
  }

  private def read(path: NioPath): Option[String] =
    if (Files.exists(path)) Some(new String(Files.readAllBytes(path), UTF_8))
    else None

  private def download(path: String, target: NioPath, params: Map[String, String] = Map.empty) =
    Http.default(
      localhost / path <<? params > as.File(target.toFile)
    ).either.apply()

  property("replaces the contents of an existing file") =
    forAllNoShrink(nonEmptyAlphaStr, Gen.alphaStr) { (existing, body) =>
      val contents = withTarget { target =>
        write(target, existing)
        download("echo", target, Map("echo" -> body))
        read(target)
      }
      contents ?= Some(body)
    }

  property("writes the body to a file that does not exist yet") =
    forAllNoShrink(Gen.alphaStr) { body =>
      val contents = withTarget { target =>
        download("echo", target, Map("echo" -> body))
        read(target)
      }
      contents ?= Some(body)
    }

  property("does not create a file on an error status") =
    forAllNoShrink(Gen.const("missing")) { path =>
      val (result, contents) = withTarget { target =>
        val result = download(path, target)
        (result, read(target))
      }
      (result ?= Left(StatusCode(404))) && (contents ?= None)
    }

  property("leaves an existing file untouched on an error status") =
    forAllNoShrink(nonEmptyAlphaStr) { existing =>
      val (result, contents) = withTarget { target =>
        write(target, existing)
        val result = download("missing", target)
        (result, read(target))
      }
      (result ?= Left(StatusCode(404))) && (contents ?= Some(existing))
    }
}
