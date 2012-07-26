Working with multiple promises
------------------------------

If we want to compare the promised temperature in New York to Madrid,
we might apply both promises to compare the eventual values. We
certainly can't make a good comparison if only one or zero of the
values are available right now.

But if taking one hostage is bad, taking *n* hostages is worse. Higher
demands take longer to be met and the cost of monitoring each
prisoner, or applied promise, increases.

### Independent promises

Luckily, we don't have to apply promises to work with their values. We
can stage operations to occur as soon as those values are
available—even with more than one promise.

First, we'll assign some promised temperatures using the methods
defined on the last page.

```scala
val newyork = temperature("New York, USA")
val madrid = temperature("Madrid, Spain")
```

Dispatch is already working to fulfill both promises. But assuming as
we must that their values are not available, we can still lay out work
for them to do:


```scala
for {
  n <- newyork
  m <- madrid
} {
  if (n > m) println("It's hotter in New York")
  else  println("It's at least as hot in Madrid")
}
```

Like all for-expressions used with promises, this one doesn't block on
I/O at any point. We're effectively chaining callbacks for the time
when both promises say they are available.

### Yielding combined results

But this isn't a very flexible procedure. Let's generalize it by
yielding a promised value.

```scala
def tempCompare(locA: String, locB:String) = {
  val pa = temperature(locA)
  val pb = temperature(locB)
  for {
    a <- pa
    b <- pb
  } yield a.compare(b)
}
```

Now we have a method that promises an integer indicating the relative
temperatures of places *a* and *b*.

### Dependent promises and concurrency

You might be tempted to refactor the comparison method into a shorter
expression.

```scala
def sequentialTempCompare(locA: String, locB:String) =
  for {
    a <- temperature(locA)
    b <- temperature(locB)
  } yield a.compare(b)
```

It's still non-blocking, but it *doesn't perform the two requests in
parallel*. To understand why, think about the bindings of the values
*a* and *b*. They both represent promised values.

Although the above expression `temperature(locB)` doesn't reference
the value of *a*, **it could**. Since *a* is known we must be in the
future: we must be in deferred code.

And that's exactly the case. Each clause of the for-expression on a
promise represents a promise callback. This is necessary for cases
where one promised value depends on another. Independent promises
should be assigned outside for-expressions to maximize concurrency.
