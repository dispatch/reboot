package object dispatch {
  import com.ning.http.client.RequestBuilder

  implicit def implyRequestVerbs(builder: RequestBuilder) =
    new DefaultRequestVerbs(builder)

  implicit def implyRequestHandlerTuple(builder: RequestBuilder) =
    new RequestHandlerTupleBuilder(builder)
}
