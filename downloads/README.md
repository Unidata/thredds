# Downloads Layout

`tds/`: Contains the `latest.xml` file, which is read by TDS instances on startup to log current version information.

To update the artifacts server to use all of the download related files managed in this repo, run the following from the top level of the repo:

~~~shell
./gradlew :downloads:updateAll
~~~
