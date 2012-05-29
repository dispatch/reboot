Dispatch Reboot
---------------

Dispatch reboot is a rewrite of the [Dispatch][dispatch] library for
HTTP interaction in Scala, using [sonatype/async-http-client][async]
as its underlying transport.

[async]: https://github.com/sonatype/async-http-client
[dispatch]: http://dispatch.databinder.net/Dispatch.html

### Example Usage

The best examples of Dispatch reboot so far are [conscript][cs] and
[herald][herald] which use it exclusively for HTTP interaction. The
[reboot tests][tests], as ScalaCheck properties, are also a good
source of example interaction.

[cs]: https://github.com/n8han/conscript
[herald]: https://github.com/n8han/herald
[tests]: https://github.com/dispatch/reboot/tree/master/core/src/test/scala

And here is a very simple example to get you started:

```scala
import dispatch._
val raw = :/("raw.github.com").secure
val readme = raw / "dispatch" / "reboot" / "master" / "README.markdown"
val promise = Http(readme > As.string)
for (string <- promise) {
  println(string)
}
```

### Leaving the scene

Dispatch's client uses a thread-pool and, as usual when working in the
Scala console or sbt interactive mode, you will need to shut it down
manually to ensure a clean and quick exit.:

```scala
Http.shutdown()
```

Applications typically use one Dispatch executor instance and handle
it in a general shutdown routine.

### Promises

Dispatch's promises are a rich interface to the underlying client's
[ListenableFuture][lf] interface. They're designed to make it as easy
and elegant as possible to work with expected responses and the errors
that might occur, to avoid blocking operations while promoting
composability. For example, two functions that perform network
operations might both yield a Promise to avoid blocking, and these
could be composed in a third trivial function:

[lf]: https://github.com/sonatype/async-http-client/blob/master/src/main/java/com/ning/http/client/ListenableFuture.java

```scala
def add(p1: Promise[Int], p2: Promise[Int]): Promise[Int] =
  for {
    i1 <- p1
    i2 <- p2
  } yield i1 + i2
```

Since errors are known to occur, we might be wise to deal in a
different type of Promise:

```scala
def add(p1: Promise[Either[String,Int]],
        p2: Promise[Either[String,Int]])
        : Promise[Either[String,Int]] =
  for {
    i1 <- p1.right
    i2 <- p2.right
  } yield i1 + i2
```

The forthcoming Scala 2.10 defines a Future/Promise type and Dispatch
will bind to it after its release. We also support Scala 2.8.x and
2.9.x; the aim is to create an interface that works well with
existing infrastructure and with all that is coming down the pike.
