Arbitrarily many promises
-------------------------

The last page dealt with fixed numbers of promises. In the real world,
we often have to work with unknown quantities.

### Iterables of promises

Once again using the `temperature` method defined before, we'll create
a higher-level method to work with its promised values. First, we can
work with Scala collections in familiar ways.

```scala
val locs = List("New York, USA",
                "Madrid, Spain",
                "Seoul, Korea")
val temps =
  for(loc <- locs)
    yield (temperature(loc) -> loc)
```

Now we have a list of promised city names and temperatures:
`List[Promise[(Int, String)]]`. But if we want to compare them
together, again without blocking, we want a combined promise of all
temps.

### Promise.all

```scala
val hottest =
  for (ts <- Promise.all(temps))
    yield ts.max
```

The value `ts` is a promise of `Iterable[(Int, String)]`; it is not
available until all the component promises have completed.

### Implicit ordering

In the body of the for expression we're using `max` on this iterable
of pairs. Tuples derive thir orderings from their component parts,
so the max of our tuple `(Int, String)` will be a pair with the
highest temperature.

### Promising to report the hottest

We can generalize this now into a single method which promises to
return the name of the hottest city that you give it.

```scala
def hottest(locs: String*) =
  val temps =
    for(loc <- locs)
      yield (temperature(loc) -> loc)
  for (ts <- Promise.all(temps))
    yield ts.max._2
}
```

When everything goes as expected, that promise is fulfilled. The next
section is for when things don't go as expected.
