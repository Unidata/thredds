---
title: Building From Source
last_updated: 2018-04-02
sidebar: netcdfJavaTutorial_sidebar
permalink: building_from_source.html
toc: false
---

## Assembly

THREDDS source code is hosted on GitHub, and—as of v4.6.1—we use Gradle to build it.
Ant and Maven builds are no longer supported.
THREDDS includes the NetCDF-Java client libraries, the TDS server, the TDM indexer, and other related packages.
To build, you need Git and JDK 7+ installed.

First, clone the THREDDS repository from Github:

~~~bash
git clone git://github.com/Unidata/thredds.git thredds
~~~

You’ll have a new folder named thredds in your working directory. Change into it:

~~~bash
cd thredds
~~~

By default, the current branch head is set to 5.0, which is our main development branch.
If you’d like to build a released version instead—v4.6.2, for example, you’ll need to checkout that version:

~~~bash
git checkout v4.6.2
~~~

Next, use the Gradle wrapper to execute the assemble task:

~~~bash
./gradlew assemble
~~~

There will be various artifacts within the `<subproject>/build/libs/` subdirectories.
For example, the TDS WAR file will be in tds/build/libs/.
The uber jars, such as `toolsUI.jar` and `netcdfAll.jar`, will be found in `build/libs/`.

## Publishing

NetCDF-Java is comprised of several modules, many of which you can use within your own projects, as described here.
At Unidata, we publish the artifacts that those modules generate to our Nexus repository.

However, it may happen that you need artifacts for the in-development version of THREDDS, which we usually don’t upload to Nexus. 
Never fear: you can build them yourself and publish them to your local Maven repository!

~~~
git checkout master
./gradlew publishToMavenLocal
~~~

You will find the artifacts in `~/.m2/repository/edu/ucar/`.
If you’re building your projects using Maven, artifacts in your local repo will be preferred over remote ones by default; you don’t have to do any additional configuration in order for them to be picked up.
If you’re building with Gradle, you’ll need to do [a little more work](https://docs.gradle.org/current/userguide/dependency_management.html#sub:maven_local){:target="_blank"}.

