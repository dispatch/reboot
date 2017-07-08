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
  host("api.wunderground.com") / "api" / "$wkey$" /
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
element "temp_c" using the `\\\\` method of `xml.Elem`.

```scala
def extractTemp(xml: scala.xml.Elem) = {
  val seq = for {
    elem <- xml \\\\ "temp_c"
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
