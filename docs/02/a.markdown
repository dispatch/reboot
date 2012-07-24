Success as the only option
--------------------------

Earlier we compared promises to options. The network operation at the
center of things may or may not have completed: that's the temporal
uncertainty and it can be thought of an option, and even transformed
into one with the `completeOption` method.

Beyond that we don't know if a completed promise will produce an error
or a useful response. We can also think of that uncertainty, and model
it in code, as an option. By transferring the uncertainty from the
promise to a contained option, we make a promise we can always keep.

### Promise#option

```scala
import dispatch._
val str = Http(host("example.com") OK as.String).option
```

The type assigned is `str: Promise[Option[String]]`. When the promise
completes, its value will be a promise of `None` since the request
will fail. The failure exception is captured and discarded by the
underlying code.

With this we can write higher level interfaces that encompass the
possibility of failure.

### Optional weather

Let's make the weather interface from the previous section a little
more resilient.

```scala
def weatherSvc(loc: String) = // no change
  url("http://www.google.com/ig/api").addQueryParameter("weather", loc)
def weatherXml(loc: String) =
  Http(weatherSvc(loc) OK as.xml.Elem).option
```

Now any connection, status, or parsing error will produce a `None`.

### Optional temperature

We'll make a slight change to the extraction method.

```scala
def extractTemp(xml: scala.xml.Elem) = {
  val seq = for {
    elem <- xml \\\\ "temp_c"
    attr <- elem.attribute("data") 
  } yield attr.toString.toInt
  seq.headOption
}
```

Instead of calling `head` which throws an exception if there are no
matching elements, we call `headOption`. This meshes with a revised
temperature method.

```scala
def temperature(loc: String) =
  for (xmlOpt <- weatherXml(loc))
    yield for {
      xml <- xmlOpt
      t <- extractTemp(xml)
    } yield t
```

This promises some temperature value, or `None` if an error occurs at
any point.

### Optional hotness

And with that, we can rewrite `hottest` to promise the highest
successful result, or `None`.

```scala
def hottest(locs: String*) = {
  val temps =
    for(loc <- locs)
      yield for (tOpt <- temperature(loc))
        yield for (t <- tOpt)
          yield (t -> loc)
  for (ts <- Promise.all(temps)) yield {
    val valid = ts.flatten
    for (_ <- valid.headOption)
      yield valid.max._2
  }
}
```

If the nested for-expressions throw you for a loop, keep in mind that
promises are not themselves iterable. You're dealing with unrelated
types, even if they share some philosophical opinions. They can't be
haphazardly mixed in the same for-expression.

But as the *for*s unroll we end up with a promise of some city name,
or `None`—exactly what we want. Give it a try with some real and fake
city names.

### Unknown error

This version of temperature ranking is much more resilient than the
last, but it still leaves something to be desired. We don't know from
the result value which cities, if any, were excluded from
consideration, and we don't know why.

In the next section we'll explore `Either`, a favorite type of those
who plan for both failure and success.
