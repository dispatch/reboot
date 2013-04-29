package dispatch.immutable
import scala.concurrent.ExecutionContext.Implicits.global

object :/ extends HostCreation
object host extends HostCreation

object /:\ {
  def apply = HttpRequest
}

trait HostCreation {
  def apply(host: String): HttpRequest =
    HttpRequest().setHost(host)

  def apply(host: String, port: Int): HttpRequest =
    HttpRequest().setHostAndPort(host, port)
}

object url {
  def apply(url: String) = HttpRequest().setUrl(url)
}

object Http {
  def apply(req: HttpRequest) = dispatch.Http()(req.request)
}

protected[this] trait RequestVerbs extends (() => HttpRequest) {
  this: Any =>

  def apply(): HttpRequest
  def apply(host: String): HttpRequest = apply().setHost(host)
}

object HEAD    extends RequestVerbs { def apply() = HttpRequest().setMethod("HEAD") }
object GET     extends RequestVerbs { def apply() = HttpRequest().setMethod("GET") }
object POST    extends RequestVerbs { def apply() = HttpRequest().setMethod("POST") }
object PUT     extends RequestVerbs { def apply() = HttpRequest().setMethod("PUT") }
object DELETE  extends RequestVerbs { def apply() = HttpRequest().setMethod("DELETE") }
object TRACE   extends RequestVerbs { def apply() = HttpRequest().setMethod("TRACE") }
object OPTIONS extends RequestVerbs { def apply() = HttpRequest().setMethod("OPTIONS") }
