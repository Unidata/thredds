# Docs Layout

`common/`: Generic index page and corresponding javascript file to programmatically generate a top level documentation index page.
For example: <https://docs.unidata.ucar.edu/netcdf-java/index.html>.

`ncml/`, `netcdf-java/`, and `tds/`: Contains the version-info.json files specific to the respective project.
The format of the file follows [this spec](https://github.com/Unidata/unidata-jekyll-theme/issues/15#issuecomment-842700747).

To update the artifacts server to use all of the documentation related files managed in this repo, run the following from the top level of the repo:

~~~shell
./gradlew :docs:updateAll
~~~
