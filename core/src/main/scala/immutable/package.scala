package dispatch

package object immutable {

  implicit def implyRequestHandlerTuple(builder: HttpRequest) =
    new RequestHandlerTupleBuilder(builder.request)

  // Probably a bad idea.
  implicit def IgnoreMe(builder: HttpRequest) =
    builder.request

  implicit def initRequestVerbs(verb: RequestVerbs): HttpRequest =
    verb()
}
