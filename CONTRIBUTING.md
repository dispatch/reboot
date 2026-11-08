# Dispatch Contribution Guidelines

We're glad to hear that you'd like to contribute to Dispatch! Please review
this document for information about how you can contribute to Dispatch.

## Getting support

Dispatch maintains a [mailing
list](https://groups.google.com/forum/?fromgroups#!forum/dispatch-scala)
where you're free to ask any questions you have about how to use Dispatch.
The mailing list is monitored by [@farmdawgnation][fdn], Dispatch's current
mainainer, and a handful of Dispatch users.

You're also free to [open an issue](https://github.com/dispatch/reboot/issues)
on the project and we'll be happy to look into your issue.

## Making contributions

If you're interested in making contributions to Dispatch, we'd be glad to have your help! For
anything beyond obvious bug fixes, before you're off to the races, please make sure there's an open
issue representing the work you'd like to do and that [@farmdawgnation][fdn] has agreed to have you
do the work. This is largely to ensure that your work isn't a) something that someone else is
already working on and b) is in line with the long term plans for Dispatch.

By making a contribution you agree to license that contribution to us, and everyone else under
the terms of the LGPL Version 3.

### Ownership of Contribution

A condition of contributing to Dispatch is that you own your contribution. If you do not own your
contribution you should obtain written permission from the owner of the contribution and present
that in an email to [@farmdawgnation][fdn] _before_ signing the CLA.

[fdn]: https://github.com/farmdawgnation

## Publishing a release

Dispatch publishes to Maven Central through the [Sonatype Central
Portal](https://central.sonatype.com/) using sbt's built-in support. Maintainers
need a Central Portal user token for the `org.dispatchhttp` namespace and a GPG
key published to a public keyserver.

Put the token in `~/.sbt/1.0/.credentials`, which the build loads
automatically:

```
realm=Sonatype Nexus Repository Manager
host=central.sonatype.com
user=<token username>
password=<token password>
```

Alternatively, sbt also reads the `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
environment variables.

Then sign and stage every cross-built module, and upload the bundle:

```
sbt +publishSigned sonaRelease
```

`sonaRelease` uploads the staged bundle and publishes it automatically once
validation passes. Use `sonaUpload` instead to stop after upload and release it
by hand from the Central Portal. Snapshot versions skip staging and go straight
to the Central snapshots repository with `sbt +publishSigned`; snapshots must
be enabled for the namespace in the Central Portal first.
