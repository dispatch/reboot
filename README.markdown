Dispatch Reboot
---------------

A rethink of Dispatch with [sonatype/async-http-client][async] as the
underlying transport library.

Currently, you can do something like this:

```scala
import dispatch._
val raw = :/("raw.github.com").secure
val readme = raw / "dispatch" / "reboot" / "master" / "README.markdown"
val promise = Http(readme -> As.string)
for (string <- promise) {
  println(string)
}

// tidy up
Http.shutdown()
```

[async]: https://github.com/sonatype/async-http-client
