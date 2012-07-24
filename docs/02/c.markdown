Promising Either
----------------

Now that you understand *either*, you can use it within a Dispatch
promise to fully control and represent error conditions.

### Promise#either

Much like `Promise#option`, the either method returns a promise that
catches any exception that occurrs in performing the request and
handling its response. But unlike option, either holds on to its
captured exception.

### Weather with either

Let's take up our weather service one more time and write an app that
can not only fail gracefully but tell you what went wrong. First we
define the service endpoint, as before.

```scala
import dispatch._
def weatherSvc(loc: String) =
  url("http://www.google.com/ig/api").addQueryParameter("weather", loc)
```

### Projections on promises

A promise of either doesn't know whether it's a *left* or *right*
until it is completed, so it can't have methods like `isLeft` and
`isRight`.

What you can do is project against eventual leftness and rightness.
All promises of either have methods `left` and `right` which act much
the same as those methods on either itself. They return a projection
which you then use to transform one side of the either.

The example below uses a left projection. Bulky type annotations are
included in this text for clarity.

```scala
def weatherXml(loc: String):
  Promise[Either[String, xml.Elem]] = {
  val res: Promise[Either[Throwable, xml.Elem]] =
    Http(weatherSvc(loc) OK as.xml.Elem).either
  for (exc <- res.left)
    yield "Can't connect to weather service: \n" +
      exc.getMessage
}
```

In this updated `weatherXml` method, we get a promise of either as
`res`. Then, we act on a left projection of that promise to transform
any exception into a string error message.

### Handling missing input

Next, we'll issue a useful error message if we fail to find the
expected temperature element.

```scala
def extractTemp(xml: scala.xml.Elem):
  Promise[Either[String,Int]] = {
  val seq = for {
    elem <- xml \\\\ "temp_c"
    attr <- elem.attribute("data") 
  } yield attr.toString.toInt
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
def temperature(loc: String) =
  for (xmlEither <- weatherXml(loc))
    yield for {
      xml <- xmlEither.right
      t <- extractTemp(xml).right
    } yield t
```

This is fairly similar to the version created with option. You'll
recall that we can't haphazardly mix promises with other types in for
expressions, because a promise *is not* an iterable or an
either. However, if you want to be a little bit fancy you can condense
these operations by making everything a promise.

### Composing promises of either

When everything is a promise of either, you can compose with a single
for expression. We can't make promises into their contained type
without blocking, but we can go the other way: anything can be made
into a promise of itself with `Promise#apply`.

```scala
def temperature(loc: String):
  Promise[Either[String,Int]] = {
  for {
    xml <- weatherXml(loc).right
    t <- Promise(extractTemp(xml)).right
  } yield t
```

Composing with a single for-expression is *awesome*, but don't get too
stuck on the idea. Sometimes it's just not possible or worth the
trouble. But in this case, it provides the nicest error handling yet.

### Testing the error handling

You can try out the new method to see how it behaves with valid and
invalid input.

    scala> temperature("New York, NY")()
    res8: Either[String,Int] = Right(25)

    scala> temperature("nowhere")()
    res9: Either[String,Int] =
      Left(Temperature missing in service response)

For an unknown city name, we got back a response without a usable
temperature element. Good to know!

### Hottness to the max

Now we'll bring it all together with an error-aware hotness method.

```scala
def hottest(locs: String*) = {
  val temps =
    for(loc <- locs)
      yield for (tEither <- temperature(loc))
        yield (loc, tEither)
  for (ts <- Promise.all(temps)) yield {
    val valid = for ((loc, Right(t)) <- ts)
      yield (t, loc)
    val max = for (_ <- valid.headOption)
      yield valid.max._2
    val errors = for ((loc, Left(err)) <- ts)
      yield (loc, err)
    (max, errors)
  }
}
```

This method returns a promise of a 2-tuple, including an option of the
max and iterable of any errors. With this you can know which city was
the hottest, as well as which inputs failed and why.

### Testing the hottest

To make sure this all works, give it some valid and invalid cities.

    scala> hottest("New York, USA",
                   "Madrid, Spain",
                   "nowhere",
                   "Seoul, Korea")()
    res19: (Option[String], Iterable[(String, String)]) =
    (Some(Madrid, Spain),
     ArrayBuffer((nowhere,
                  Temperature missing in service response)))

In real applications, string is not usually a rich enough error
type; you may want the app to behave differently for different kinds
of errors. For that you can bubble up case classes and objects that
represent the kind of error and retain any useful data.
