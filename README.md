![THREDDS icon](http://www.unidata.ucar.edu/images/logos/netcdfjava_tds_150x150.png)
<br>
<br>

# Unidata's THREDDS Project

The Thematic Real-time Environmental Distributed Data Services (THREDDS) project is developing middleware to bridge the gap between data providers and data users.

The main branch of this repository is used to manage files relevant to all THREDDS projects (currently related to documentation).

`docs/`: contains top level files related to documentation hosted on docs.unidata.ucar.edu.
`downloads/`: contains top level files related to downloads hosted on downloads.unidata.ucar.edu.

To update the artifacts server to use all of the files managed in this repository, run:

~~~shell
./gradlew updateAll
~~~