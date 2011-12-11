Dispatch Reboot
---------------

A rethink of Dispatch with [sonatype/async-http-client][async] as the
underlying transport library.

Currently, this much works:

```scala
import dispatch._
Http(:/("github.com").secure / "dispatch" / "reboot" >- println)
Http.shutdown
```

[async]: https://github.com/sonatype/async-http-client
