Dispatch
========

*Dispatch* is a library for asynchronous HTTP interaction. It provides
 a Scala vocabulary for Java's [async-http-client][ahc]. The latest
 release version is [**0.13.1**](https://github.com/dispatch/reboot/releases/tag/v0.13.1).

[ahc]: https://github.com/AsyncHttpClient/async-http-client

> This documentation walks through basic functionality of the
> library. You may also want to refer to its
> [scaladocs](http://www.javadoc.io/doc/net.databinder.dispatch/dispatch-core_2.11/0.13.1).

### Diving in

To start playing with Dispatch on a console you can use one of two tools. The
[Ammonite REPL][ammrepl] or [sbt][sbt]'s console functionality. When you're ready to include
Dispatch in an actual project, just follow the instructions for adding the Dispatch dependencies
to `build.sbt` below.

[ammrepl]: http://ammonite.io/#Ammonite-REPL

[sbt]: http://www.scala-sbt.org/

### Ammonite REPL

To get started with Dispatch in the Ammonite REPL, execute `amm` at your shell and then paste in
the following.

```scala
# only include this first line if you want all
# of the debugging log output, otherwise omit
import $ivy.`ch.qos.logback:logback-classic:1.2.3`

import $ivy.`net.databinder.dispatch::dispatch-core:0.13.1`
```

Your environment now has everything in scope you need to play with dispatch in the console.

### SBT

Once you have sbt installed, Dispatch is two steps away. Open a
shell and change to an empty or unimportant directory, then add the following
content to a file named `build.sbt`:

```
libraryDependencies ++= Seq(
  // For the console exercise, the logback dependency
  // is only important if you want to see all the
  // debugging output. If you don't want that, simply
  // omit it.
  "ch.qos.logback"          %  "logback-classic" % "1.2.3",
  "net.databinder.dispatch" %% "dispatch-core"   % "0.13.1"
)
```

Then invoke `sbt console` from your shell. After "the internet" has downloaded, you're good to go.
the above settings in `build.sbt` are also the settings you'll use to add dispatch to your project
when it comes time to actually use it in a production application.

### Defining requests

We'll start with a very simple request.

```scala
import dispatch._, Defaults._
val svc = url("http://api.hostip.info/country.php")
val country = Http.default(svc OK as.String)
```

The above defines and initiates a request to the given host where 2xx
responses are handled as a string. Since Dispatch is fully
asynchronous, `country` represents a *future* of the string rather
than the string itself.

### Deferring action

You can act on the response once it's available with a
*for-expression*.

```scala
for (c <- country)
  println(c)
```

This for-expression applies to any *successful* response that is
eventually produced. If no successful response is produced, nothing is
printed. This is how for-expressions work in general. Consider a more
familiar example:

```scala
val opt: Option[String] = None
for (o <- opt)
  println(o)
```

An *option* may or may not contain a value, just like a future may or
may not produce a successful response. But while any given option
already knows what it is, a future may not. So the future behaves
asynchronously in for-expressions, to avoid holding up operations
subsequent that do not depend on its value.

### Demanding answers

As with options, you can require that a future value be available at
any time:

```scala
val c = country()
```

But the wise use of futures defers this operation as long as is
practical, or doesn't perform it at all. To see how, keep reading.

Bargaining with futures
------------------------

Applying a future is like taking a hostage. Your demands might be met
in time, but until they are you're sitting around doing nothing other
than guarding a prisoner.

So we don't like to take hostages or apply futures, but what good is
a future if you can't do anything with its value? Luckily, you can do
plenty. You just have to be flexible about when things happen.

### Transformations

A future is like an option that doesn't know what it is yet; that
doesn't stop it from transforming into something else. We could
transform an option of a string into an option of its length. Same
goes for futures.

```scala
import dispatch._, Defaults._
val svc = url("http://api.hostip.info/country.php")
val country = Http.default(svc OK as.String)
val length = for (c <- country) yield c.length
```
The `length` value is a future of integer.

### Future#print

If you pasted the above into a console, you probably saw something
like this in the output:

    country: scala.concurrent.Future[String] =
      scala.concurrent.impl.Promise$DefaultPromise@4929b5a5
    length: scala.concurrent.Future[Int] =
      scala.concurrent.impl.Promise$DefaultPromise@581fa0fe

Not too helpful right? The `print` method makes a nicer string:

    scala> country.print
    res0: String = Future(US)

If the future value isn't available, `print` won't wait:

    scala> Http.default(svc OK as.String).print
    res1: String = Future(-incomplete-)

> **Note:** `print` and some other `Future` methods in this documentation
  are provided implicitly by `dispatch.EnrichedFuture`

### Future#completeOption

How does `print` work on unknown values? It uses an option. You can
use the same technique to access the integer value, *if it's
available*.

```scala
val lengthNow = length.completeOption.getOrElse(-1)
```

But most of the time, you want to operate on values that are known to
be available. In the next pages we'll see how far we can go in this
direction by transforming futures.

Abstraction over future information
-------------------------------------

Often, you can extend the utility of futures with simple
abstraction. In this example we'll leverage a web service to write an
internal API that will tell us the temperature in a US city.

### Palling around with Weather Underground

In one method we'll contain the construction of the request. In this
case it's an endpoint with all of the parameters in path elements.

```scala
import dispatch._, Defaults._

case class Location(city: String, state: String)

def weatherSvc(loc: Location) = {
  host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
    "conditions" / "q" / loc.state / (loc.city + ".xml")
}
```

> **Note:** Yes, that's an API key. Use it sparingly to learn
  Dispatch in the Scala console, but
  [get your own key](http://www.wunderground.com/weather/api/) if you
  are building some kind of actual weather application. We may reset
  this key at any time.

With this method we can bind to a handler that prints out the response
in the usual way:

```scala
val nyc = Location("New York", "NY")
for (str <- Http.default(weatherSvc(nyc) OK as.String))
  println(str)
```

If you're pasting along in the Scala console, you'll see a bunch of
raw XML.

### Parsing XML

Luckily, dispatch has another built-in handler for services that
respond in this format.

```scala
def weatherXml(loc: Location) =
  Http.default(weatherSvc(loc) OK as.xml.Elem)
```

This method returns a future `scala.xml.Elem`. Note that Dispatch
handlers, like `as.String` and `as.xml.Elem`, mimic the name of the
type they produce. They're all under the package `dispatch.as` where
you can access them without additional imports.

### Traversing XML

At this stage we're working with a higher abstraction. The `Http.default`
instance used to perform the request has become an implementation
detail that `weatherXml` callers need not concern themselves with. We
can use our new method to print a nicely formatted response.

```scala
def printer = new scala.xml.PrettyPrinter(90, 2)
for (xml <- weatherXml(nyc))
  println(printer.format(xml))
```

Looking at the structure of the document, we can extract the
temperature of the location in degrees Celsius by searching for the
element "temp_c" using the `\\` method of `xml.Elem`.

```scala
def extractTemp(xml: scala.xml.Elem) = {
  val seq = for {
    elem <- xml \\ "temp_c"
  } yield elem.text.toFloat
  seq.head
}
```

### Temperature of the future

With this we can create another high-level access method:

```scala
def temperature(loc: Location) =
  for (xml <- weatherXml(loc))
    yield extractTemp(xml)
```

And now we have at hand the future temperature of any location
understood by the service:


```scala
val la = Location("Los Angeles", "CA")
for (t <- temperature(la)) println(t)
```

The information gathering is now fully abstracted without blocking,
but what happens if we want to compare several temperatures?

Working with multiple futures
-----------------------------

If we want to compare the future temperature in New York to Madrid,
we might apply both futures to compare the eventual values. We
certainly can't make a good comparison if only one or zero of the
values are available right now.

But if taking one hostage is bad, taking *n* hostages is worse. Higher
demands take longer to be met and the cost of monitoring each
prisoner, or applied future, increases.

### Independent futures

Luckily, we don't have to apply futures to work with their values. We
can stage operations to occur as soon as those values are
available—even with more than one future.

First, we'll assign some future temperatures using the methods
defined on the last page.

```scala
val nycTemp = temperature(nyc)
val laTemp = temperature(la)
```

Dispatch is already working to fulfill both futures. But assuming as
we must that their values are not available, we can still lay out work
for them to do:

```scala
for {
  n <- nycTemp
  m <- laTemp
} {
  if (n > m) println("It's hotter in New York")
  else  println("It's at least as hot in L.A.")
}
```

Like all for-expressions used with futures, this one doesn't block on
I/O at any point. We're effectively chaining callbacks for the time
when both futures say they are available.

### Yielding combined results

But this isn't a very flexible procedure. Let's generalize it by
yielding a future value.

```scala
def tempCompare(locA: Location, locB: Location) = {
  val pa = temperature(locA)
  val pb = temperature(locB)
  for {
    a <- pa
    b <- pb
  } yield a.compare(b)
}
```

Now we have a method for the future of an integer indicating the
relative temperatures of places *a* and *b*.

### Dependent futures and concurrency

You might be tempted to refactor the comparison method into a shorter
expression.

```scala
def sequentialTempCompare(locA: Location, locB: Location) =
  for {
    a <- temperature(locA)
    b <- temperature(locB)
  } yield a.compare(b)
```

It's still non-blocking, but it *doesn't perform the two requests in
parallel*. To understand why, think about the bindings of the values
*a* and *b*. They both represent future values.

Although the above expression `temperature(locB)` doesn't reference
the value of *a*, **it could**. Since *a* is known we must be in the
future: we must be in deferred code.

And that's exactly the case. Each clause of the for-expression on a
future represents a future callback. This is necessary for cases
where one future value depends on another. Independent futures
should be assigned outside for-expressions to maximize concurrency.

Arbitrarily many futures
------------------------

The last page dealt with fixed numbers of futures. In the real world,
we often have to work with unknown quantities.

### Iterables of futures

Once again using the `temperature` method defined before, we'll create
a higher-level method to work with its future values. First, we can
work with Scala collections in familiar ways.

```scala
val locs = List(Location("New York", "NY"),
                Location("Los Angeles", "CA"),
                Location("Chicago", "IL"))
val temps =
  for(loc <- locs)
    yield for (t <- temperature(loc))
      yield (t -> loc)
```

Now we have a list of future city names and temperatures:
`List[Future[(Float, Location)]]`. But if we want to compare them
together, again without blocking, we want a combined future of all
temps.

### Future.sequence

```scala
val hottest =
  for (ts <- Future.sequence(temps))
    yield ts.maxBy { _._1 }
hottest()
```

The value `ts` is a future of `List[(Float, Location)]`; it is not
available until all the component futures have completed. In the body
of the for expression we're using `maxBy` to find the highest
temperature, the first element of the tuple.

### A future of the hottest

We can generalize this now into a single method which futures to
return the name of the hottest city that you give it.

```scala
def hottest(locs: Location*) = {
  val temps =
    for(loc <- locs)
      yield for (t <- temperature(loc))
       yield (t -> loc)
  for (ts <- Future.sequence(temps))
    yield ts.maxBy { _._1 }._2
}
```

When everything goes as expected, that future is fulfilled. The next
section is for when things don't go as expected.

A future of success and failure
-------------------------------

So far we've made a lot of futures depending on network operations
that might fail, and remote services that may not care for our
input. If things don't go as planned, the futures will fail.

### Failed futures

Failed futures are messy. You may have already seen the mess created
in playing around with the previous examples. Here we'll make a big
mess to see what happens, and how bad it can get.

```scala
import dispatch._, Defaults._
val str = Http.default(host("example.com") OK as.String)
```

So far, so good? We've made a request that will fail the *OK* test
with a redirect status code, but this failure hasn't happened yet
from the software's perspective.

### Future#print for failed futures

If we have the console print its string representation a moment later,
we'll see the problem:

    scala> str.print
    res0: String = Future(!Unexpected response status: 302!)

### Applying failed futures

But we're still holding have a future of string. What happens if we
demand the string?

    scala> str()
    dispatch.StatusCode: Unexpected response status: 302
        at dispatch.OkHandler$class.onStatusReceived(handlers.scala:37)
        at dispatch.OkFunctionHandler.onStatusReceived(handlers.scala:29)
        ...

The exception was thrown in the thread that demanded the value, since
there is no way to supply it.

### Transforming broken futures

Broken futures carry their exceptions through transformations:

    val length = for (s <- str) yield s.length
    length.print

Printing yields the same result as before.

    res54: String = Future(!Unexpected response status: 302!)

### Deferred failed futures

And if you ask for operations on the completed future, nothing
happens.

    scala> for (s <- str) println(s)

How can we safely build on futures that depend on uncertain network
operations?

### Planning for failure

The solution is to avoid breaking futures and throwing exceptions by
planning for failure. In the next pages we'll see very simple and very
rich ways of doing that.

Success as the only option
--------------------------

Earlier we compared futures to options. The network operation at the
center of things may or may not have completed: that's the temporal
uncertainty and it can be thought of an option, and even transformed
into one with the `completeOption` method.

Beyond that we don't know if a completed future will produce an error
or a useful response. We can also think of that uncertainty, and model
it in code, as an option. By transferring the uncertainty from the
future to a contained option, we make a future that will never fail.

### Future#option

```scala
import dispatch._, Defaults._
val str = Http.default(host("example.com") OK as.String).option
```

The type assigned is `str: Future[Option[String]]`. When the future
completes, its value will be a future of `None` since the request
will fail. The failure exception is captured and discarded by the
underlying code.

With this we can write higher level interfaces that encompass the
possibility of failure.

### Optional weather

Let's make the weather interface from the previous section a little
more resilient.

```scala
case class Location(city: String, state: String)
def weatherSvc(loc: Location) = {
  host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
    "conditions" / "q" / loc.state / (loc.city + ".xml")
}
def weatherXml(loc: Location) =
  Http.default(weatherSvc(loc) OK as.xml.Elem).option
```

Now any connection, status, or parsing error will produce a `None`.

### Optional temperature

We'll make a slight change to the extraction method.

```scala
def extractTemp(xml: scala.xml.Elem) = {
  val seq = for {
    elem <- xml \\ "temp_c"
  } yield elem.text.toFloat
  seq.headOption
}
```

Instead of calling `head` which throws an exception if there are no
matching elements, we call `headOption`. This meshes with a revised
temperature method.

```scala
def temperature(loc: Location) =
  for (xmlOpt <- weatherXml(loc))
    yield for {
      xml <- xmlOpt
      t <- extractTemp(xml)
    } yield t
```

This returns the future of some temperature value, or `None` if an
error occurs at any point.

### Optional hotness

And with that, we can rewrite `hottest` to provide the highest
successful result, or `None`.

```scala
def hottest(locs: Location*) = {
  val temps =
    for(loc <- locs)
      yield for (tOpt <- temperature(loc))
        yield for (t <- tOpt)
          yield (t -> loc)
  for (ts <- Future.sequence(temps)) yield {
    val valid = ts.flatten
    for (_ <- valid.headOption)
      yield valid.maxBy { _._1 }
  }
}
```

If the nested for-expressions throw you for a loop, keep in mind that
futures are not themselves Iterable. You're dealing with unrelated
types, even if they share some philosophical opinions. They can't be
haphazardly mixed in the same for-expression.

But as the *for*s unroll we end up with a future of some city name,
or `None`—exactly what we want. Give it a try with some real and fake
city names.

### Unknown error

This version of temperature ranking is much more resilient than the
last, but it still leaves something to be desired. We don't know from
the result value which cities, if any, were excluded from
consideration, and we don't know why.

In the next section we'll explore `Either`, a favorite type of those
who plan for both failure and success.

Either type will do
-------------------

`Either` is a container of fixed size like `Option`, but which always
contains a value of one of two types. As an abstract type either
refers to its two possible typed values as "left" and "right".

### Either an error or success

In the particular and common case of error handling, the either's
*left* should always be used for failure information. This can be
anything from an error message to an application-specific error
object. It's the either's type *A*.

The either's *right* value of type *B* is for its content on
success. Thus, any given either used for error handling should tell
you the desired result, or the reason it has failed.

### Average or failure

As a trivial example, let's implement a method to return the average
of some integers.

```scala
def average(nums: Traversable[Int]) = {
  if (nums.isEmpty) Left("Can't average emptiness")
  else Right(nums.sum / nums.size)
}
```

This method produces an error message when given an empty collection
of integers to average, otherwise the average integer.

### Top of the class

We can use this failure-aware average method as part of a larger
calculation.

```scala
val johnny = List(85, 60, 90)
val sarah  = List(88, 65, 85)
val billy  = List.empty[Int]

for {
  j <- average(johnny).right
  s <- average(sarah).right
  b <- average(billy).right
} yield List(j, s, b).max
```

The for-expression above requires successful averages (a *right*
projection on each either) in order to yield a right result. Since
Billy's average results in a *left*, the entire expression evaluates
to that error.

    res0: Either[java.lang.String,Int] = Left(Can't average emptiness)

### Why not eject?

Of course, exceptions have the same ability demonstrated here: you can
embed information in them and act on it when they're
caught. Exceptions are easy to handle when you have a straightforward
thread of computation. In asynchronous programming, you don't.

Think of exceptions as an ejection seat. They allow you to escape from
failure without planning ahead. On the downside, somebody's got to
perform the rescue operation to get you home, which could range in
difficulty from easy to impossible. With asynchronous callbacks it's
as if you're flying over enemy territory, or into orbit. The cost and
complexity of recovering an ejected body becomes prohibitive.

But the use of *either* for error handling is like having a plan to
fly home no matter what goes wrong. You may not be carrying a
successful payload but at least you'll return safely with information.

### Understanding either

If you don't understand `Either`, seek out some more explanations and
examples before continuing. Dispatch's richest forms of error handling
use this type directly and imitate it in important ways.

Either Future
-------------

Now that you understand *either*, you can use it within a Dispatch
future to fully control and represent error conditions.

### Future#either

Much like `Future#option`, the either method returns a future that
catches any exception that occurs in performing the request and
handling its response. But unlike option, either holds on to its
captured exception.

### Weather with either

Let's take up our weather service one more time and write an app that
can not only fail gracefully but tell you what went wrong. First we
define the service endpoint, as before.

```scala
import dispatch._, Defaults._
case class Location(city: String, state: String)
def weatherSvc(loc: Location) = {
  host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
    "conditions" / "q" / loc.state / (loc.city + ".xml")
}
```

### Projections on futures

A future of either doesn't know whether it's a *left* or *right*
until it is completed, so it can't have methods like `isLeft` and
`isRight`.

What you can do is project against eventual leftness and rightness.
All futures of either have methods `left` and `right` which act much
the same as those methods on either itself. They return a projection
which you then use to transform one side of the either.

The example below uses a left projection. Bulky type annotations are
included in this text for clarity.

```scala
def weatherXml(loc: Location):
  Future[Either[String, xml.Elem]] = {
  val res: Future[Either[Throwable, xml.Elem]] =
    Http.default(weatherSvc(loc) OK as.xml.Elem).either
  for (exc <- res.left)
    yield "Can't connect to weather service: \n" +
      exc.getMessage
}
```

In this updated `weatherXml` method, we get a future of either as
`res`. Then, we act on a left projection of that future to transform
any exception into a string error message.

### Handling missing input

Next, we'll issue a useful error message if we fail to find the
expected temperature element.

```scala
def extractTemp(xml: scala.xml.Elem):
  Either[String,Float] = {
  val seq = for {
    elem <- xml \\ "temp_c"
  } yield elem.text.toFloat
  seq.headOption.toRight {
    "Temperature missing in service response"
  }
}
```

This uses the handy `Option#toRight` method which bridges the gap
between options and eithers.

### Composing with either

Finally, we can write a smarter `temperature` method that composes the
smarter low-level methods.


```scala
def temperature(loc: Location) =
  for (xmlEither <- weatherXml(loc))
    yield for {
      xml <- xmlEither.right
      t <- extractTemp(xml).right
    } yield t
```

This is fairly similar to the version created with option. You'll
recall that we can't haphazardly mix futures with other types in for
expressions, because a future *is not* an Iterable or an
either. However, if you want to be a little bit fancy you can condense
these operations by making everything a future.

### Composing futures of either

When everything is a future of either, you can compose with a single
for expression. We can't make futures into their contained type
without blocking, but we can go the other way: anything can be made
into a future of itself with `Future#apply`.

```scala
def temperature(loc: Location):
Future[Either[String,Float]] = {
  for {
    xml <- weatherXml(loc).right
    t <- Future.successful(extractTemp(xml)).right
  } yield t
}
```

Composing with a single for-expression is *awesome*, but don't get too
stuck on the idea. Sometimes it's just not possible or worth the
trouble. But in this case, it provides the nicest error handling yet.

### Testing the error handling

You can try out the new method to see how it behaves with valid and
invalid input.

    scala> temperature(Location("New York","NY"))()
    res8: Either[String,Float] = Right(11.9)

    scala> temperature(Location("nowhere","NO"))()
    res5: Either[String,Float] =
      Left(Temperature missing in service response)

For an unknown city name, we got back a response without a usable
temperature element. Good to know!

### Hottness to the max

Now we'll bring it all together with an error-aware hotness method.

```scala
def hottest(locs: Location*) = {
  val temps =
    for(loc <- locs)
      yield for (tEither <- temperature(loc))
        yield (loc, tEither)
  for (ts <- Future.sequence(temps)) yield {
    val valid = for ((loc, Right(t)) <- ts)
      yield (t, loc)
    val max = for (_ <- valid.headOption)
      yield valid.maxBy { _._1 }._2
    val errors = for ((loc, Left(err)) <- ts)
      yield (loc, err)
    (max, errors)
  }
}
```

This method returns a future of a 2-tuple, including an option of the
max and Iterable of any errors. With this you can know which city was
the hottest, as well as which inputs failed and why.

### Testing the hottest

To make sure this all works, give it some valid and invalid cities.

    scala> hottest(Location("New York","NY"),
                   Location("Chicago", "IL"),
                   Location("nowhere", "NO"),
                   Location("Los Angeles", "CA"))()
    res6: (Option[Location], Seq[(Location, String)]) =
    (Some(Location(Los Angeles,CA)),
    ArrayBuffer((Location(nowhere,NO),
                Temperature missing in service response)))

In real applications, string is not usually a rich enough error
type; you may want the app to behave differently for different kinds
of errors. For that you can bubble up case classes and objects that
represent the kind of error and retain any useful data.

Defining requests
-----------------

Dispatch requests are defined using the [RequestBuilder][rb] class of
the underlying library. Everything that can be expressed with
Dispatch's builders and "verbs" can be performed directly on that
lower level interface.

[rb]: http://asynchttpclient.github.com/async-http-client/apidocs/com/ning/http/client/RequestBuilder.html


### Domains and paths

Request definitions are initialized with a URL or domain name.

#### Free-form URLs

The function `url` belongs to the `dispatch` package. It is typically
imported by wildcard. If it becomes shadowed by a local `url` value,
you can always refer to it as `dispatch.url`.

```scala
val myRequest = url("http://example.com/some/path")
```

With this builder it is up to the application to construct valid URLs.

#### Explicit host builder

To dynamically build up requests, Dispatch provides a number of
builders and verbs (symbolic methods). First, you need a host.

```scala
val myHost = host("example.com")
```

A port can be specified as a second parameter.

```scala
val myHost = host("example.com", 8888)
```

When no port is specified, the protocol default is used.

#### Using HTTPS

When using the host builder, the `secure` method specifies that the
HTTPS must be used for the request.

```scala
val mySecureHost = host("example.com").secure
```

#### Appending path elements

Path elements may be added to requests with the `/` method.

```scala
val myRequest = myHost / "some" / "path"
```

Each added element is URL-encoded, so that spaces and non-ASCII
letters may be added freely. A forward-slash will also be encoded such
that it does not serve as a path-separator; the `/` method is for
appending *single* path elements.

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

Query parameter names can repeat in case you need provide multiple values
for a query parameter key.

```scala
def myRequestWithParams = myRequest
  .addQueryParameter("key", "value1")
  .addQueryParameter("key", "value2")
```

You can also add query parameters with the `<<?` verb.

```scala
def myRequestWithParams = myRequest <<? Map("key" -> "value")
```

The `<<?` verb can consume any kind of `Traversable` that contains a
`(String, String)`, so if you'd like to use the verb form to add multiple
query parameters with the same key, you'd just switch to using a `List`:

```scala
def myRequestWithParams = myRequest <<? List(
  ("key", "value1"),
  ("key", "value2")
)
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

[rb]: http://asynchttpclient.github.com/async-http-client/apidocs/com/ning/http/client/RequestBuilder.html

Unraveling for-expressions
--------------------------

So far, this documentation has relied exclusively on for-expressions
for transforming futures, composing futures, and deferring side
effects. These provide a compact syntax that smooths the rough edges
of dense, nested function literals. Dispatch is mostly coded, tested,
and documented with for-expressions to ensure that everything can be
expressed neatly.

On the flip side, for-expressions **can seem like black magic**. They're
extremely powerful and incorporate features of the Scala language and
standard library. What's *really happening* won't be at all apparent
to beginners.  If it compiles it tends to work, but when it doesn't
compile the type errors can be a great mystery.

### Read about for-expressions

It's never too early or too late to learn more about
for-expressions. Chapter 10 of [Scala by Example][ex] provides an
explanation that is both gentle and comprehensive. You can't read it
enough.

[ex]: http://www.scala-lang.org/sites/default/files/linuxsoft_archives/docu/files/ScalaByExample.pdf

#### What are for-comprehensions?

For-expressions and for-comprehensions are the same thing. The
preferred term these days is for-comprehensions.

### Break apart complex problems

For non-trivial future operations, especially when trying to mix with
Iterables, it may be easier to start with the lower level map,
flatMap, foreach, and many other methods that for-expressions
translate into.

Once you get things working with these, you can probably translate it
into a for-expression. Maybe by rereading Chapter 10 of *Scala by
Example*. Or you can leave it using the lower level methods. There are
no for-expression police to hunt you down.

### For-expression (in)completeness

For-expressions can do so many different things that Dispatch futures
and projections don't support them all. If your cool for-expression
doesn't work for this reason, feel free to contribute the missing
methods to Disptach.
