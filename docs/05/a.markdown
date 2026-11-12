Limiting and sharing threads
----------------------------

AHC's config builder also controls the threads an executor uses. Like
any other setting, you change them with `Http.withConfiguration`.

### Capping I/O threads

`setIoThreadsCount` sets the most I/O threads an executor will start:

```scala
val http = Http.withConfiguration(_.setIoThreadsCount(2))
```

Each I/O thread serves many connections at once, so a capped executor
can still run many requests in parallel.

### Sharing threads between executors

If you need several executors with different settings, they can share
one set of I/O threads and one timer. Create a Netty event loop group
and timer yourself, then pass both to each executor:

```scala
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.util.HashedWheelTimer

val ioThreads = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory())
val timer = new HashedWheelTimer()

val apiHttp = Http.withConfiguration(
  _.setEventLoopGroup(ioThreads).setNettyTimer(timer).setFollowRedirect(true)
)
val cdnHttp = Http.withConfiguration(
  _.setEventLoopGroup(ioThreads).setNettyTimer(timer).setMaxConnections(200)
)
```

Together, these two executors use at most four I/O threads and one
timer thread. Each still has its own connection pool.

Because you created the event loop group and timer, shutting down an
executor doesn't stop them. Shut down every executor that uses them,
then stop them yourself:

```scala
apiHttp.shutdown()
cdnHttp.shutdown()
ioThreads.shutdownGracefully()
timer.stop()
```

### Naming threads

`setThreadPoolName` sets the prefix of an executor's thread names, so
you can tell executors apart in a thread dump:

```scala
val http = Http.withConfiguration(_.setThreadPoolName("billing-api"))
```

Its threads are then named like `billing-api-12-1` and
`billing-api-timer-11-1`. Without this setting, every executor's
threads start with `AsyncHttpClient`. The name only applies to threads
the executor creates itself, not to a shared event loop group or
timer.
