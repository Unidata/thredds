# testUtil

Contains miscellaneous utility methods for use in our test suite.

## Inclusion of third-party software

This project contains source code from [JUnit](https://github.com/junit-team/junit4), version 4.12.
The license for JUnit is available in `docs/src/private/licenses/third-party/junit/`.

### Details of use:

Copied several methods from `org.junit.Assert` that are used in the implementations of our
`Assert2.assertNearlyEquals()` family of methods.
