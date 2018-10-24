---
title: THREDDS Data Manager
last_updated: 2018-10-15
sidebar: tdsTutorial_sidebar
toc: false
permalink: tdm_ref.html
---

## Overview

The TDM creates indexes for GRIB featureCollections, in a process separate from the TDS. This allows lengthy file scanning and reindexing to happen in the background.
The TDS uses the existing indices until notified that new ones are ready.

The TDM shares the TDS configuration, including `threddsConfig.xml` and the server configuration catalogs.
On startup, it reads through the catalogs and finds GRIB `featureCollections` with a `<tdm>` element and adds them to a list.
It can index once or periodically, depending on how you configure the `<tdm>` element.
If you change the configuration, you must restart the TDM.

* For static datasets, let the TDM create the indexes, then start the TDS.
* For dynamic datasets, the TDM should run continually, and can send messages to the TDS when a dataset changes.

## Installing the TDM

Get the current jar linked from the [TDS Download Page](https://www.unidata.ucar.edu/downloads/thredds/index.jsp){:target="_blank"}

The TDM can be run from anywhere, but by convention we create a directory `$tds.content.root.path}/tdm`, and run the TDM from there.

Create a shell script to run the TDM, for example `runTdm.sh`:

~~~bash
<JAVA> <JVM options> -Dtds.content.root.path=<content directory> -jar <TDM jar> [-tds <tdsServers>] [-cred <user:passwd>] [-showOnly] [-log level]
~~~

for example:

~~~bash
/opt/jdk/bin/java -Xmx4g -Dtds.content.root.path=/opt/tds/content -jar tdm-5.0.jar -tds "http://thredds.unidata.ucar.edu/,http://thredds2.unidata.ucar.edu:8081/"
~~~

where:

* `<JAVA>` Large collections need a lot of memory, so use a 64-bit JVM
* `<JVM options>`
  * `-Xmx4g*` to give it 4 Gbytes of memory (for example). 
    More is better.
* `-Dtds.content.root.path=<content directory>` this passes the content directory as a system property.
  The thredds configuration catalogs and `threddsConfig.xml` are found in `<content directory>/thredds`.
  Use an absolute path.
* `-jar tdm-5.0.jar` : execute the TDM from the jar file
* `-tds <tdsServers>`: (optional) list of TDS servers to notify.
   If more than one, separate with commas, with no blanks.
   Specify only the scheme, host and optional port with a trailing slash for example: `http://localhost:8081/`
* `-cred <user:passwd>`: (optional) if you send notifications, the TDS will authenticate using this user name and password.
  If you do not include this option, you will be prompted for the password on startup, and the user name will be set to `tdm`.
* `-showOnly`: (optional) if this is present, just show the featureCollections that will be indexed and exit.
* `-log level`: (optional) set the log4j logging level = `DEBUG`, `INFO` (default), `WARN`, `ERROR`

Troubleshooting:

* Make sure that the `<JVM Options>`, including `-Dtds.content.root.path`, come before the `-jar <TDM jar>`
* The `<content directory>` does not include the `/thredds` subdirectory, e.g. `/opt/tds/content` not `/opt/tds/content/thredds`.
* You must run the TDM as a user who has read and write permission into the data directories, so it can write the index files (OR)
* If you are using [GRIB index redirection](tds_config_ref.html#grib-index-redirection), the TDM must have read access to the data directories, and write access to the index directories.

## Running the TDM:

* Upon startup, if `-tds` was used, but `-cred` was not, you will be prompted for the password for the `tdm` user password. 
  This allows you to start up the TDM without putting the password into a startup script.
  Note that user `tdm` should be given only the role of `tdsTrigger`, which only gives rights to trigger collection reloading (see [below](#sending-triggers-to-the-tds)).
* The TDM will write index files into the data directories or index directories. 
  The index files will have extensions **gbx9** and **ncx4**.
* For each `featureCollection`, a log file is created in the TDM working directory, with name `fc.<collectionName>.log`.
  Monitor these logs to look for problems with the indexing.
* If you start the TDS in a shell, its best to put in the background so it can run independent of the shell:

~~~bash
^Z  (this is Control-Z)
bg
~~~

## Sending triggers to the TDS

The TDM scans the files in the feature Collection, and when it detects that the collection has changed, rewrites the index files.
If enabled, it will send a trigger message to the TDS, and the TDS will reload that dataset.
To enable this, you must configure the TDS with the `tdsTrigger` role, and add the user `tdm` with that role.
Typically you do that by editing the `${tomcat_home}/conf/tomcat-user.xml` file, e.g.:

~~~xml
<?xml version='1.0' encoding='utf-8'?>
<tomcat-users>
  <role ... />
  <role rolename="tdsTrigger"/>
  <user ... />
  <user username="tdm" password="secret" roles="tdsTrigger"/>
</tomcat-users>
~~~

Make sure that the `tdm` user has only the `tdsTrigger` role, for security.

If you dont want to allow external triggers, for example if your datasets are static, simply don\'t enable the `tdsTrigger` role in Tomcat.
You can also set `trigger="false"` in the `update` element in your catalog:

~~~xml
<update startup="never" trigger="false" />
~~~

## Catalog Configuration Examples

Example configuration in the TDS configuration catalogs.
Point the TDM to the content directory using `-Dtds.content.root.path=<content directory>` on the TDM command line.

### Static dataset:

~~~xml
<featureCollection name="NOMADS CFSRR" featureType="GRIB2" harvest="true" 
                   path="grib/NOMADS/cfsrr/timeseries">
  <metadata inherited="true">
    <dataType>GRID</dataType>
    <dataFormat>GRIB-2</dataFormat>
  </metadata>

  <collection name="NOMADS-cfsrr-timeseries" spec="/san4/work/jcaron/cfsrr/**/.*grib2$"
                   dateFormatMark="#cfsrr/#yyyyMM" timePartition="directory"/>

  <tdm rewrite="always"/>
</featureCollection>
~~~

* `rewrite="always"` tells the TDM to index this dataset upon TDM startup.
* A log file will be written to `fc.NOMADS-cfsrr-timeseries.log` in the TDM working directory.
* The TDS will use the existing indexes, it does not monitor any changes in the dataset.

### Dynamic dataset:

~~~xml
<featureCollection name="DGEX-Alaska_12km" featureType="GRIB2" harvest="true" 
                   path="grib/NCEP/DGEX/Alaska_12km">
  <metadata inherited="true">
     <dataType>GRID</dataType>
     <dataFormat>GRIB-2</dataFormat>
  </metadata>

  <collection name="DGEX-Alaska_12km"
   spec="/data/ldm/pub/native/grid/NCEP/DGEX/Alaska_12km/.*grib2$"
   dateFormatMark="#DGEX_Alaska_12km_#yyyyMMdd_HHmm"
   timePartition="file"
   olderThan="5 min"/>

  <tdm rewrite="true" rescan="0 0/15 * * * ? *" trigger="allow"/>
  <update startup="never" trigger="allow" />
</featureCollection>
~~~

* `<tdm>` element for the TDM
  * `rewrite="test"` tells the TDM to test for dataset changes
  * `rescan="0 0/15 * * * ? *"`  rescan directories every 15 minutes.
* `<update>` element for the TDS
  * `startup="never"` tells the TDS to read in the `featureCollection` when starting up, using the existing indices
  * `trigger="allow"` enables the TDS to receive messages from the TDM when the dataset has changed

## GCPass1

This is a utility program to examine the files in a collection before actually indexing them.

### Example:

~~~bash
java -Xmx2g -classpath tdm-4.6.jar thredds.tdm.GCpass1 -spec "Q:/cdmUnitTest/gribCollections/rdavm/ds083.2/PofP/**/.*grib1" -useCacheDir "C:/temp/cache/"  > gcpass1.out
~~~

### Command line arguments:

~~~bash
Usage: thredds.tdm.GCpass1 [options]
  Options:
    -h, --help
       Display this help and exit
       Default: false
    -isGrib2
       Is Grib2 collection.
       Default: false
    -partition
       Partition type: none, directory, file
       Default: directory
    -regexp
       Collection regexp string, exactly as in the <featureCollection>.
    -rootDir
       Collection rootDir, exactly as in the <featureCollection>.
    -spec
       Collection specification string, exactly as in the <featureCollection>.
    -useCacheDir
       Set the Grib index cache directory.
    -useTableVersion
       Use Table version to make seperate variables.
       Default: false
~~~

* You must have `spec` or (`regexp` and `rootDir`).
* If `useCacheDir` is not set, indexes will be in the data directories

### Sample Output:

~~~bash
FeatureCollectionConfig name= 'GCpass1' collectionName= 'GCpass1' type= 'GRIB1' # <1>
        spec= 'B:/rdavm/ds083.2/grib1/**/.*grib1'
        timePartition= directory

#files  #records   #vars  #runtimes    #gds
 Directory B:\rdavm\ds083.2\grib1               # <2>
  Directory B:\rdavm\ds083.2\grib1\1999         # <3>
   B:\rdavm\ds083.2\grib1\1999\1999.07 total    1    244 63   1 1 1999-07-30T18:00:00Z - 1999-07-30T18:00:00Z # <4>
   B:\rdavm\ds083.2\grib1\1999\1999.08 total  119  29046 66 119 1 1999-08-01T00:00:00Z - 1999-08-31T18:00:00Z
   B:\rdavm\ds083.2\grib1\1999\1999.09 total   89  21755 66  89 1 1999-09-01T00:00:00Z - 1999-09-30T12:00:00Z
   B:\rdavm\ds083.2\grib1\1999\1999.10 total   62  15128 63  62 1 1999-10-01T00:00:00Z - 1999-10-31T12:00:00Z
   B:\rdavm\ds083.2\grib1\1999\1999.11 total   97  23816 66  97 1 1999-11-01T00:00:00Z - 1999-11-30T18:00:00Z
   B:\rdavm\ds083.2\grib1\1999\1999.12 total  120  29512 66 120 1 1999-12-01T00:00:00Z - 1999-12-31T18:00:00Z
   B:\rdavm\ds083.2\grib1\1999   total   488   119501 66 488 1 1999-07-30T18:00:00Z - 1999-12-31T18:00:00Z   # <5>

 Directory B:\rdavm\ds083.2\grib1\2000 #<3>
   B:\rdavm\ds083.2\grib1\2000\2000.01 total 124 30504 64 124 1 2000-01-01T00:00:00Z - 2000-01-31T18:00:00Z  # <4>
   B:\rdavm\ds083.2\grib1\2000\2000.02 total 116 28536 64 116 1 2000-02-01T00:00:00Z - 2000-02-29T18:00:00Z
   B:\rdavm\ds083.2\grib1\2000\2000.03 total 124 30504 64 124 1 2000-03-01T00:00:00Z - 2000-03-31T18:00:00Z
   B:\rdavm\ds083.2\grib1\2000\2000.04 total 120 29520 64 120 1 2000-04-01T00:00:00Z - 2000-04-30T18:00:00Z
...

  B:\rdavm\ds083.2\grib1\2014\2014.11 total 120  34560 76 120 1 2014-11-01T00:00:00Z - 2014-11-30T18:00:00Z
  B:\rdavm\ds083.2\grib1\2014\2014.12 total  67  19296 76  67 1 2014-12-01T00:00:00Z - 2014-12-17T12:00:00Z
  B:\rdavm\ds083.2\grib1\2014 total  1403 444544  116  1403 1 2014-01-01T00:00:00Z - 2014-12-17T12:00:00Z   #<5>
  B:\rdavm\ds083.2\grib1 total  22347 6546693  118  22347  1 1999-07-30T18:00:00Z - 2014-12-17T12:00:00Z    #<5>

             #files #records  #vars  #runtimes    #gds
grand total   22347  6546693    118      22347       1 #<6>

referenceDate (22347) #<7>
   1999-07-30T18:00:00Z - 2014-12-17T12:00:00Z: count = 22347

table version (2) #<8>
         7-0-1: count = 3188
         7-0-2: count = 6543505

variable (118)     #<9>
    5-wave_geopotential_height_anomaly_isobaric_10: count = 22076
    5-wave_geopotential_height_isobaric_10: count = 22344
    Absolute_vorticity_isobaric_10: count = 581022
    Albedo_surface_Average: count = 6922
      ...

gds (1)            # <10>
    1645598069: count = 6546693

gdsTemplate (1)    # <11>
             0: count = 6546693

vertCoordInGDS (0) # <12>

predefined (0)     # <13>

thin (0)           # <14>
~~~

1. The Feature Collection configuration
2. The top level directory
3. Subdirectory
4. Partitions - in this case these are directories because this is a _directory partition_. 
   * number of files in the partition 
   * number of records in the partition 
   * number of seperate variables in the partition. 
     _Inhomogenous partitions look more complex to the user._
   * number of runtimes in the partition
   * number of horizontal (GDS), which are turned into groups 
   * the starting and ending runtime. _Look for overlapping partitions_
5. Sum of subpartitions for this partition
6. Grand sum over all partitions
7. Summary (n, start/end) of run dates
8. list of all table versions found, count of number of records for each. _Possibility that variables that should be seperated by table version._
9. list of all variables found, count of number of records for each. _Possibility that stray records are in the collection._
10. list of all GDS hashes found, count of number of records for each. 
    **Possibility of spurious differences with GDS hashes**.
11.  list of all GDS templates found, count of number of records for each
12.  count of records that have vertical coordinates in the GDS (GRIB1 only)
13.  count of records that have predefined GDS (GRIB1 only) _Possibility of unknown predefined GDS._
14.  count of records that have _Quasi/Thin_ Grid (GRIB1 only)
