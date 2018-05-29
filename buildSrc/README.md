# buildSrc

Contains custom [Task](https://docs.gradle.org/current/dsl/org.gradle.api.Task.html)s
and utilities for use in the building of THREDDS. For reference, see
[this](https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_sources).

## Inclusion of third-party software

This project contains source code from [Gretty](https://github.com/akhikhl/gretty), version 1.4.2.
The license for Gretty is available in `docs/src/private/licenses/third-party/gretty/`.

### Details of use:

Copied and modified `org.akhikhl.gretty.GradleUtils`.
