Dispatch Reboot
---------------

[![Build Status](https://travis-ci.org/dispatch/reboot.svg?branch=master)](https://travis-ci.org/dispatch/reboot)

Latest version: [![Maven][mavenImg]][mavenLink]

[mavenImg]: https://img.shields.io/maven-central/v/net.databinder.dispatch/dispatch-core_2.12.svg
[mavenLink]: https://mvnrepository.com/artifact/net.databinder.dispatch/dispatch-core_2.12


Dispatch reboot is a rewrite of the Dispatch library for
HTTP interaction in Scala, using [async-http-client][async]
as its underlying transport. For more info, see the
[Dispatch documentation site][docs].

[docs]: http://dispatch.databinder.net/Dispatch.html
[async]: https://github.com/AsyncHttpClient/async-http-client

### Supported Versions & AHC

There are two currently supported versions of Dispatch. The version of Async HTTP Client you need
to use will largely dictate what version you should use in larger projects. Those versions are:

* [`0.12.x`](https://github.com/dispatch/reboot/tree/0.12.x) series: Uses AHC 1.9.x
* [`0.13.x`](https://github.com/dispatch/reboot/tree/master) series: Uses AHC 2.0.x

We are also publishing snapshots of the [`0.14.x`](https://github.com/dispatch/reboot/tree/master_with_ahc2.1)
series to Sonatype Snapshots that are built against the AHC 2.1.x alpha builds.

### Dependencies
* [Async HTTP client](https://github.com/AsyncHttpClient/async-http-client)
* JDK 8, as required by the Async HTTP client library

### Mailing List

There's a [mailing list for Dispatch][mail]. Please mail the list **before opening
github issues** for problems that are not obvious, reproducible bugs.

[mail]: https://groups.google.com/forum/?fromgroups#!forum/dispatch-scala
