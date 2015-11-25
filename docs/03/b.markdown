HTTP filtering
-----------------------

The async-http-client provides lower level facilities that can be used from the scala layer.
Filtering is one example.

### Request and response filtering

Filters can be added to the async-http-client and used for each request or response. The method
adding filters is the same as in the async-http-client library but you can use the dispatch layer
to configure the filters. You could handle filtering by writing a scala function
that modifies a request but then you will need to call the function every time you
create a request and that may be inconvenient.

```scala
import com.ning.http.client.filter._
import java.util.concurrent.atomic.AtomicLong

case class EchoFilter() extends RequestFilter {
  val counter = new AtomicLong(0L)
  def filter[T](ctx: FilterContext[T]): FilterContext[T] = {
    // This just illustrates deriving a new request object
    // from the exsiting. The request object here is the async-http-client
    // request object.
    val builder = new RequestBuilder(ctx.getRequest)
    // Potentially modify the original request...
    val newRequest = builder.build
    val requestNum = counter.incrementAndGet

    // Print the request to the console.
    println(s"Request \${requestNum} of length: \${newRequest.getContentLength}").

    // Return a new FilterContext that will be used for the request.
    new FilterContext.Builder(ctx).request(newRequest).build
  }
}
```
You need to be careful about handling state in your filter as the filter
is used asynchronously.

In your scala code, you need to add the filter to the async-http-client. You can do
this by configuring a new Http executor.

```scala
val myhttp = Http.configure(_.addRequestFilter(EchoFilter()))
...
val responseFuture = myhttp(someRequest OK as.)
```

