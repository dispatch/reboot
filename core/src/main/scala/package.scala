package object dispatch {
  /** Type alias for RequestBuilder, our typical request definitions */
  type Req = com.ning.http.client.RequestBuilder

  implicit def implyRequestVerbs(builder: Req) =
    new DefaultRequestVerbs(builder)

  implicit def implyRequestHandlerTuple(builder: Req) =
    new RequestHandlerTupleBuilder(builder)

  implicit val durationOrdering = Ordering.by[Duration,Long] {
    _.millis
  }
}
