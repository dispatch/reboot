Either type will do
-------------------

`Either` is a container of fixed size like `Option`, but which always
contains a value of one of two types. As an abstract type either
refers to its two possible typed values as "left" and "right".

### Either an error or success

In the particular and common case of error handling, the either's
*left* should always be used for failure information. This can be
anything from an error message to an application-specific error
object. It's the either's type *A*.

The either's *right* value of type *B* is for its content on
success. Thus, any given either used for error handling should tell
you the desired result, or the reason it has failed.

### Average or failure

As a trivial example, let's implement a method to return the average
of some integers.

```scala
def average(nums: Traversable[Int]) = {
  if (nums.isEmpty) Left("Can't average emptiness")
  else Right(nums.sum / nums.size)
}
```

This method produces an error message when given an empty collection
of integers to average, otherwise the average integer.

### Top of the class

We can use this failure-aware average method as part of a larger
calculation.

```scala
val johnny = List(85, 60, 90)
val sarah  = List(88, 65, 85)
val billy  = List.empty[Int]

for {
  j <- average(johnny).right
  s <- average(sarah).right
  b <- average(billy).right
} yield List(j, s, b).max
```

The for-expression above requires successful averages (a *right*
projection on each either) in order to yield a right result. Since
Billy's average results in a *left*, the entire expression evaluates
to that error.

    res0: Either[java.lang.String,Int] = Left(Can't average emptiness)

### Why not eject?

Of course, exceptions have the same ability demonstrated here: you can
embed information in them and act on it when they're
caught. Exceptions are easy to handle when you have a straightforward
thread of computation. In asynchronous programming, you don't.

Think of exceptions as an ejection seat. They allow you to escape from
failure without planning ahead. On the downside, some body's got to
perform the rescue operation to get you home, which could range in
difficulty from easy to impossible. With asynchronous callbacks it's
as if you're flying over enemy territory, or into orbit. The cost and
complexity of recovering an ejected body becomes prohibitive.

But the use of *either* for error handling is like having a plan to
fly home no matter what goes wrong. You may not be carrying a
successful payload but at least you'll return safely with information.

### Understanding either

If you don't understand `Either`, seek out some more explanations and
examples before continuing. Dispatch's richest forms of error handling
use this type directly and imitate it in important ways.
