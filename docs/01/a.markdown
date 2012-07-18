Abstraction over promised information
-------------------------------------

Often, can you can the extend utility of promises with simple
abstraction. In this example we'll leverage a web service to write an
internal API that promises to tell us the temperature.

### Googling the weather

In one method we'll contain the construction of the request. In this
case it's an endpoint that only requires one parameter, the location
of interest. It's a fuzzy lookup, so we don't have to be too careful
about the input for major cities.

```scala
import dispatch._
def weatherSvc(loc: String) =
  url("http://www.google.com/ig/api").addQueryParameter("weather", loc)
```

With this method we can bind to a handler that prints out the response
in the usual way:

```scala
for (str <- Http(weatherSvc("New York, USA") OK as.String))
  println(str)
```

If you're pasting along in the Scala console, you'll see a bunch of
raw XML.

### Parsing XML

Luckily, dispatch has another built-in handler for services that
respond in this format.

```scala
def weatherXml(loc: String) =
  Http(weatherSvc(loc) OK as.xml.Elem)
```

This method returns a promise `scala.xml.Elem`. Note that Dispatch
handlers, like `as.String` and `as.xml.Elem`, mimic the name of the
type they produce. They're all under the package `dispatch.as` where
you can access them without additional imports.

### Traversing XML

At this stage we're working with a higher abstraction. The `Http`
instance used to perform the request has become an implementation
detail that `weatherXml` callers need not concern themselves with. We
can use our new method to print a nicely formatted response.

```scala
def printer = new scala.xml.PrettyPrinter(90, 2)
for (xml <- weatherXml("New York, USA"))
  println(printer.format(xml))
```

Looking at the structure of the document, we can extract the
temperature of the location in degrees Celsius by searching for the
element "temp_c" using the `\\` method of `xml.Elem`.

```scala
def extractTemp(xml: scala.xml.Elem) = {
  val seq = for {
    elem <- xml \\ "temp_c"
    attr <- elem.attribute("data") 
  } yield attr.toString.toInt
  seq.head
}
```

### Promising the temperature

With this we can create another high-level access method:

```scala
def temperature(loc: String) =
  for (xml <- weatherXml(loc))
    yield extractTemp(xml)
```

And now we have at hand the promised temperature of any location
understood by the service:


```scala
for (t <- temperature("New York, USA")) println(t)
```

The information gathering is now fully abstracted without blocking,
but what happens if we want to compare several temperatures?
