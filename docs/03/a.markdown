HTTP methods and parameters
---------------------------

Having defined a request endpoint using either `url`, or `host` and
the path-appending verb, you may now wish to change the HTTP method
from its default of GET.

### HTTP methods

Methods may be specified with correspondingly named request-building
methods.

```scala
def myPost = myRequest.POST
```

Other HTTP methods can be specified in the same way.

```scala
HEAD
GET
POST
PUT
DELETE
PATCH
TRACE
OPTIONS
```

### POST parameters

To add form-encoded parameters to the request body, you can use
`RequestBuilder#addParameter` method.

```scala
def myPostWithParams = myPost.addParameter("key", "value")
```

### POST verb

The `<<` verb sets the request method to POST and adds form-encoded
parameters to the body at once:

```scala
def myPostWithParams = myRequest << Map("key" -> "value")
```

You can also POST an arbitrary string. Be sure to set MIME media type
and character encoding:

```scala
def myRequestAsJson = myRequest.setContentType("application/json", "UTF-8")
def myPostWithBody = myRequestAsJson << """{"key": "value"}"""
```

### Query parameters

Query parameters can be appended to request paths regardless of the
method. These should be added after all path elements.

```scala
def myRequestWithParams = myRequest.addQueryParameter("key", "value")
```

You can also add query parameters with the `<<?` verb.

```scala
def myRequestWithParams = myRequest <<? Map("key" -> "value")
```

### PUT a file

Similar to the POST verb, Dispatch supplies a `<<<` verb to apply the
PUT method and set a `java.io.File` as the request body.

```scala
def myPut = myRequest <<< myFile
```

If you wish to supply a string instead of a file, use a `setBody`
method of the [RequestBuilder][rb] class. Its variants support a
number of input types and do not imply a particular HTTP method.

### General verbs
Request headers can be added directly to the request through the `addHeader(key, value)` method on the request itself or through a scala method `<:<`.

```scala
val token = "xxx"
val myGetRequest = myRequest <:< Map("Authorization" -> ("Bearer " + token))
```
Using `<:<` iterates through the map and calls `addHeader` on the request. You get a new request object back because the dispatch request object conceptually is a referentially transparent wrapper around the async-http-client request object. You can build up a basic request object with the headers that you want and re-use it throughout your program. 


[rb]: http://asynchttpclient.github.com/async-http-client/apidocs/com/ning/http/client/RequestBuilder.html
