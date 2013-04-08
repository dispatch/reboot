Arbitrarily many futures
------------------------

The last page dealt with fixed numbers of futures. In the real world,
we often have to work with unknown quantities.

### Iterables of futures

Once again using the `temperature` method defined before, we'll create
a higher-level method to work with its future values. First, we can
work with Scala collections in familiar ways.

```scala
val locs = List(Location("New York", "NY"),
                Location("Los Angeles", "CA"),
                Location("Chicago", "IL"))
val temps =
  for(loc <- locs)
    yield for (t <- temperature(loc))
      yield (t -> loc)
```

Now we have a list of future city names and temperatures:
`List[Future[(Float, Location)]]`. But if we want to compare them
together, again without blocking, we want a combined future of all
temps.

### Future.sequence

```scala
import scala.concurrent.Future
val hottest =
  for (ts <- Future.sequence(temps))
    yield ts.maxBy { _._1 }
hottest()
```

The value `ts` is a future of `List[(Float, Location)]`; it is not
available until all the component futures have completed. In the body
of the for expression we're using `maxBy` to find the highest
temperature, the first element of the tuple.

### A future of the hottest

We can generalize this now into a single method which futures to
return the name of the hottest city that you give it.

```scala
def hottest(locs: Location*) = {
  val temps =
    for(loc <- locs)
      yield for (t <- temperature(loc))
       yield (t -> loc)
  for (ts <- Future.sequence(temps))
    yield ts.maxBy { _._1 }._2
}
```

When everything goes as expected, that future is fulfilled. The next
section is for when things don't go as expected.
