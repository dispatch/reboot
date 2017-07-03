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
  host("api.wunderground.com") / "api" / "$wkey$" /
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
    elem <- xml \\\\ "temp_c"
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
