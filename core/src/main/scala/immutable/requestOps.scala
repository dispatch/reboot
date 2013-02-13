package dispatch.immutable

object :/ extends HostCreation
object host extends HostCreation

trait HostCreation {
  def apply(host: String): HttpRequest =
    HttpRequest().setHost(host)

  def apply(host: String, port: Int): HttpRequest =
    HttpRequest().setHostAndPort(host, port)
}

object Http {
  def apply(req: HttpRequest) = dispatch.Http()(req.request)
}
