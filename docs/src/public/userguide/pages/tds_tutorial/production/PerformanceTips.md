---
title: Performance Tips
last_updated: 2018-10-15
sidebar: tdsTutorial_sidebar
toc: false
permalink: performance_tips.html
---

## Hardware

Throw more `$$$` at this problem, hardware is cheap, compared to people.

It would be highly unusual for the TDS to **not** be `I/O` bound, so buying a high-performance disk subsystem is much better than buying fast CPUs.
Slower, more energy efficient multicore processors are optimized for web server loads.

Typically disk access is much faster on a local drive than on an NFS mounted drive.
High performance disk subsystems like RAID or SANs will significantly improve TDS throughput.

## Operating System Configuration

Welcome to 200x - use a 64-bit OS
* \'nuff said

## Use a high-performance file system

If you have system admin resources, examine the possible file systems available for your OS. We are using the ZFS file system on our Linux systems.
We use a ZFS software RAID, which replaces a hardware RAID.

That said, it\'s been awhile since we did good ol fashion filesystem bake-off, so if you have found a better solution, please let us know! 

<img src="https://www.mememaker.net/api/bucket?path=static/img/memes/full/2014/Jul/22/13/this-is-bake-off.jpg" alt="THIS IS BAKE OFF">

### Resources:
[https://zfsonlinux.org/](https://zfsonlinux.org/){:target="_blank"}

## Setting the number of file handles

The OS typically limits the number of open file handles per process.
To check this value on a Unix based OS, use:

~~~bash
ulimit -n 
~~~

If you are using the default [TDS configuration values](tds_config_ref.html#netcdffile-objects), this value should be 1024 or greater. 
Otherwise you can tune this number based on your own settings.
For example, to set this value to 2048 in the tomcat `startup.sh` script:

~~~bash
ulimit -n 2048
~~~

This affects the number of files to keep in the [File Handle Caches](#file-handles-and-caching.

## Tomcat

### Version

We recommend the latest stable version of Tomcat 8 and JDK 1.8.

### Compression

Tomcat can be configured to automatically compress the responses, whenever the client allows that.
Compression is usually a big win, especially for bandwidth-limited sites.
Deciding when and what to compress depends on a lot of factors, however.
We use the following settings in `server.xml`:

~~~xml
<!-- non-SSL HTTP/1.1 Connector on port 8080 -->
<Connector port="8080"
           protocol="HTTP/1.1"
           maxThreads="50"
           connectionTimeout="20000"
           redirectPort="8443"
           compression="1000"
           compressableMimeType="text/html,text/xml,text/plain,application/octet-stream" />
~~~

This says to compress (`gzip` or `deflate`) when the number of bytes is `>= 1000`, for the named `mime-types`.
See the Tomcat HTTP Connector [reference page](https://tomcat.apache.org/tomcat-8.0-doc/config/http.html){:target="_blank"} for more details.

### Automatic Startup

In a production environment, Tomcat should be automatically restarted when the machine starts.
How to do this depends on what OS you are running. This [FAQ](https://wiki.apache.org/tomcat/HowTo){:target="_blank"} has a bit of info.

### Miscellaneous

Once `thredds.war` is expanded, manually copy everything in `${tomcat_home}/webapps/thredds/initialContent/root/` to `${tomcat_home}/webapps/ROOT/`.

* This sets up a `robots.txt` file to keep crawlers from wasting bandwidth.
* The `favicon.ico` file is mostly a convenience to keep browsers from constantly asking for it (substitute your own icon if you like!).

#### Resources
[Tomcat Performance FAQ](https://wiki.apache.org/tomcat/FAQ/Performance_and_Monitoring){:target="_blank"}
[Tomcat Performance paper](https://tomcat.apache.org/articles/performance.pdf){:target="_blank"} by Peter Lin

## Thredds Data Server

### File Handles and Caching

The TDS caches file handles to minimize OS overhead.
Currently the defaults assume that the tomcat process is limited to 1024 file handles.
If you can allow more, you can increase the sizes of the FileCaches for more performance.
You can change these settings in the `threddsConfig.xml` file.

These numbers limit performance, but not functionality.
For example, the number of files in an aggregation is not limited by these file handle limits.

Each `NetcdfFile` object encapsulates a file.
NcML aggregations are careful not to keep component files open.
When number of cache files `> maxElementsInMemory`, a cleanup thread starts after `100` msecs. 
So the number of cached files can get larger than `maxElementsInMemory` in the interim, but unless you are really hammering the OS by opening many files-per-second, it shouldnt get too much bigger.
Leave some cushion, depending on your expected rate of opening files.

### Consolidate cache / temporary directories

The TDS writes temporary files and caches files.
By default these are stored under `${tds.content.root.path}/cache`. These
 directories can get large. You might want to relocate them to another place, for example if `${tds.content.root.path}` has limited space.
 Also, theres no need to backup the cache directories, so they can be placed on a disk that is not backed up.
 The easiest thing to do is to create a symbolic link from `${tds.content.root.path}/cache` to wherever you want these files to live.

### OPeNDAP Memory Use

The OPeNDAP layer of the server currently has to read the entire data request into memory before sending it to the client (we hope to get a streaming I/O solution working eventually). 
Generally clients only request subsets of large files, but if you need to support large data requests, make sure that the `-Xmx` JVM parameter is set accordingly.

### Pre-indexing GRIB files

If you are serving GRIB files through any service other than the `HTTPServer`, the CDM must write indices the first time it tries to read it.
This can take several minutes for very large GRIB files.
For large aggregations and collections, this can take hours or even days.
By indexing GRIB files before they are accessed with the [TDM](tdm_ref.html), users get much faster response time.
As of TDS 4.6+, when these collections change, you _must use_ the [TDM](tdm_ref.html) to detect those changes, as the TDS will **no longer update GRIB collections on the fly**.