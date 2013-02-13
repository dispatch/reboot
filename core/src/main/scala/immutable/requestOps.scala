package dispatch.immutable

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

  def apply(): HttpRequest = HttpRequest().setMethod(this.getClass.getName.replaceAll("""\$""", ""))
}

object HEAD    extends RequestVerbs
object GET     extends RequestVerbs
object POST    extends RequestVerbs
object PUT     extends RequestVerbs
object DELETE  extends RequestVerbs
object TRACE   extends RequestVerbs
object OPTIONS extends RequestVerbs
