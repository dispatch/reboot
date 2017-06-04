Dispatch Reboot
---------------

[![Build Status](https://travis-ci.org/dispatch/reboot.svg?branch=master)](https://travis-ci.org/dispatch/reboot)

Dispatch reboot is a rewrite of the Dispatch library for HTTP interaction in Scala, using
[async-http-client][async], commonly called AHC, as its underlying transport. For more info, see the
[Dispatch documentation site][docs].

Dispatch requires that you use Java 8 as AHC requires it.

[docs]: http://dispatch.databinder.net/Dispatch.html
[async]: https://github.com/AsyncHttpClient/async-http-client

## Getting Dispatch

Stable releases of Dispatch are published to Maven Central. As such, you can pull in the current
stable release by simply adding a library dependency to your project for the correct version.

In SBT you can add the following one-liner to get Dispatch 0.12.1 into your project:

```scala
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.12.1"
```

Or, if you're interested in using the 0.13.x milestone builds:

```scala
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.13.0-M1"
```

### Snapshot releases

We're also currently publishing snapshot releases for 0.14.x-SNAPSHOT to Sonatype snapshots.
Currently, the 0.14.x series tracks the API for 0.13.x exactly, with the difference that it is
built on the early alphas of AHC 2.1. If you'd like to test your code with AHC 2.1 before it's
final, taking one of these snapshots out for a spin is the way to do it.

The following instructions will get you your snapshot:

```scala
resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.14.0-SNAPSHOT"
```

## Versioning and Support

Dispatch version numbers loosely follow SemVer. The version number format is:

```
[major].[minor].[patch]
```

A distinct (major, minor) combination is called a "release series" or "series" for our purposes.

Two stable series of Dispatch are supported at a time, with one series prior to that receiving
"Critical" security updates in Dispatch itself or critical security fixes in AHC that have broken
binary compatibility. All earlier versions of Dispatch are officially unsupported and will not
receive any fixes or changes.

The following chart outlines what versions of Dispatch support what versions of AHC and Scala and
their current support status:

|Version           | AHC Version  |Scala Versions |Support       |Branch
|------------------|--------------|---------------|--------------|---------------------------------|
|0.10.0            |1.7.11        |2.9.3,2.10     |None          |                                 |
|0.11.2            |1.8.10        |2.9.3,2.10,2.11|Critical only |                                 |
|0.11.3            |1.9.11        |2.10,2.11      |Critical only |                                 |
|0.12.1            |1.9.11        |2.11,2.12      |Full support  |[0.12.x][012branch]              |
|0.13.0-M1         |2.0.32        |2.11,2.12      |Full support  |[master][masterbranch]           |
|0.14.0-SNAPSHOT   |2.1.x-alpha   |2.11,2.12      |Pre-release   |[master_with_ahc2.1][masterahc21]|

[012branch]: (https://github.com/dispatch/reboot/tree/0.12.x)
[masterbranch]: (https://github.com/dispatch/reboot/tree/master)
[masterahc21]: (https://github.com/dispatch/reboot/tree/master_with_ahc2.1)

## Getting Help and Contributing

Please see our [Contributing Guide][contributing] for information on how to contribute and get help
with Dispatch.

[contributing]: https://github.com/dispatch/reboot/blob/master/CONTRIBUTING.md
