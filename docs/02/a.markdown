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
  host("api.wunderground.com") / "api" / "$wkey$" /
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
    elem <- xml \\\\ "temp_c"
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
