---
title: FAQ
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: true
permalink: tds_faq.html
---

## General

### I have a strange problem and I need help figuring out whats going on!

Here is what we need from you in order to deal with hard problems:

1. Run the latest stable release.
   Sorry, we don't have the resources to keep older versions running, so we need to deal with just the latest code.
2. Get a clean set of logs that capture the problem:
   1. Stop the tomcat server.
   2. Install the latest release if needed.
   3. Remove all files from {tomcat}/logs and `${tds.content.root.path}`/thredds/logs
   4. Restart the server
   5. Make the problem happen
   6. Zip up everything in {tomcat}/logs and `${tds.content.root.path}/thredds/logs`.
   7. Send the logs and a detailed description of what you do to make the problem happen, and what the problem looks like.
   8. If it took a while to get the problem to happen, note what time it happened so that we can correlate with the logs.

### I made changes to my catalog and restarted tomcat but nothing changes.

1. look in `catalina.out` for a message that tomcat did not shut down:
   ~~~java
   java.net.BindException: Address already in use:8080
   ~~~
2. make sure tomcat really gets stopped:
   1. `ps -ef | grep java` to find the process id
   2. `kill <pid>` or `kill -9 <pid>`
   3. `ps -ef | grep java` to verify that the process goes away.
3. Restart tomcat:
   1. `sh ./startup.sh`
   2. Check `catalina.out` that tomcat started correctly
   3. `ps -ef | grep java` to verify that there is a new tomcat process.

### I'm building my own maven project and want to use your jar files. Where is the Unidata Repository?

In order to configure your maven project correctly, you need to edit your pom.xml file to reflect the location of Unidata's repository.
To do this, you need to add the following pom snippet to your pom.xml file:
~~~xml
<repositories>
  <repository>
    <id>unidata-releases</id>
    <name>UNIDATA Releases</name>
    <url>https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/</url>
  </repository>
</repositories>
~~~
Alternatively, you can configure your settings.xml file to reflect the repository location.
Information on doing either is located here:

[Maven Repository information](http://maven.apache.org/pom.html#Repositories){:target="_blank"}

and here:

[Maven Settings information](http://maven.apache.org/settings.html){:target="_blank"}

### I'm submitting a paper for publication and want to include a citation for the TDS. What reference should I use?

The following can be used as a citation:

~~~
   Unidata, (year): THREDDS Data Server (TDS) version tds_version [software].
   Boulder, CO: UCAR/Unidata. (http://doi.org/10.5065/D6N014KG)
~~~

where year is the year in which the work being described was done and tds_version is the version of the TDS used.
For example:

~~~
   Unidata, (2017): THREDDS Data Server (TDS) version 4.6.8 [software].
   Boulder, CO: UCAR/Unidata. (http://doi.org/10.5065/D6N014KG)
~~~

## Catalogs

### How do I construct the URLs I find in a THREDDS Catalog?

Heres the general idea in the tutorial and the catalog spec docs.
If you are using the CDM library, you can call

~~~java
InvAccess.getUrlPath()
~~~

### How do I eliminate the extra dataset when using a Catalog Reference?

Make the name of the catalogRef the same as the "top" dataset in the referenced catalog.
In the following example the name is New Point Data".

In the referencing catalog:

~~~xml
<catalogRef xlink:href="idd/newPointObs.xml" xlink:title="New Point Data" name="" />
~~~

In the referenced catalog:

~~~xml
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
 xmlns:xlink="http://www.w3.org/1999/xlink" name="THREDDS-IDD OPeNDAP Data Server - New Station Data"
 version="1.0.3">

  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
  <dataset name="New Point Data">
    ...
  </dataset>
</catalog>
~~~

### How do I provide an external service, one that is not provided by the TDS?

Make up your own service element, with an absolute URL for its base, in the configuration catalog.
For example, suppose you want to use the THREDDS OPeNDAP server, and an external WMS server:

~~~xml
<?xml version="1.0" ?>  
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" >
  <service name="both" serviceType="compound" base="">
   <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
   <service name="extWMS" serviceType="WMS" base="http://myserver:8080/wms" />
  </service>

  <dataset name="SAGE III Ozone 2006-10-31" serviceName="both" urlPath="sage/20061031.nc" ID="20061031.nc"/>
</catalog>
~~~

The problem is communicating the correct URL to your external WMS server.

## TDS

### How do I keep client requests from overwhelming my server?

There is currently no per-client resource throttle, unfortunately, but we are aware of the need for that.
Any given request is single-threaded, so can't hog too many resources.
One can limit the size of opendap responses, which tends to be the main problem on some servers.
See the OPeNDAP section of the threddsConfig.xml page for details.

### Can aggregations of many files cause "too many files open" problems?

Union type aggregations will open all the files in the aggregation at once.
The other types (joinNew, joinExisting) only open one file at a time, and then close it, so these can't cause "too many file" problems.

If you have "too many open files" errors and you are not using large Union aggregations, then either theres a file leak (which we would like to know about), or you have your file cache limit set too high relative to your OS file handle limit.

To debug file leaks:

1. Check number of open files with `ulimit -a`.
2. Restart Tomcat to close open files.
3. Monitor open files with `/usr/proc/bin/pfiles [Tomcat Process ID]`
4. Recreate the problem with minimal number of steps so we can reproduce, then send pfiles output to support.

### What do the non-HTTP status codes in the threddsServlet.log files mean?

The _Request Completed_ messages in the threddsServlet.log files contain several fields including a status code, the HTTP status code returned in a completed response.
If a request is forwarded to another internal service, a _1000 (Forwarded)_ or _1001 (Going Away)_

~~~
2009-06-17T13:25:54.451 -0600 [     28949][      11] INFO
  - thredds.server.catalogservice.LocalCatalogServiceController
  - handlePublicDocumentRequest(): Request Completed - 1001 - -1 - 32
~~~

### I'm seeing the error _Inconsistent array length read: 538976288 != 1668244581_ when I open the dataset in the IDV. Why?

The error _Inconsistent array length read_ only tells you that there was an error on the server in the middle of responding to an OPeNDAP request.
You then must look in the `threddsServlet.log` and find the error to know why.

###: Why am I getting lots of `java.util.prefs.BackingStoreException warning` messages?

If you allow and use the TDS WMS service, you may be seeing warning messages in your Tomcat `catalina.out` log file that look something like this:

~~~
May 25, 2010 6:28:22 PM java.util.prefs.FileSystemPreferences syncWorld
WARNING: Couldn't flush system prefs: java.util.prefs.BackingStoreException: /etc/.java/.systemPrefs/org create failed.
~~~

You can get rid of these messages by setting the `java.util.prefs.systemRoot` system property to a location that is writable by the user that Tomcat runs under.

Here is what we do on our servers:

Create a directory at `${tds.content.root.path}/thredds/javaUtilPrefs/.systemPrefs`, e.g.,

~~~bash
cd ${tds.content.root.path}/thredds
mkdir javaUtilPrefs
mkdir javaUtilPrefs/.systemPrefs
mkdir javaUtilPrefs/.userPrefs
~~~

Make sure that the `.systemPrefs` and `.userPrefs` directories are writable by the user under which Tomcat runs
Add the following to `JAVA_OPTS` in the `${tomcat_home}/bin/setenv.sh` file:

~~~
-Djava.util.prefs.systemRoot=${tds.content.root.path}/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=${tds.content.root.path}/thredds/javaUtilPrefs
~~~

If you are interested in more details of the problem, here are two useful links:

Sun bug [#4751177](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4751177){:target="_blank"} ("Preferences storage placed unavailable to non-root users")
[Disabling Sun's Java 1.4.x Preferences Subsystem](http://allaboutbalance.com/articles/disableprefs/){:target="_blank"}

We have this TDS issue in our bug tracking system and plan to address it.

### My TDS server is behind a proxy server. Why do some TDS generated URLs point to my TDS server instead of my proxy server?

Most TDS generated URLs are relative to the server (e.g., `/thredds/dodsC/`) or relative to the the current document's base URL.
There are only a few places where it is necessary to generate absolute URLs.
In those cases, the TDS uses information from the incoming HTTP request to construct the generated URLs.
It is up to the proxy to send the correct information to the proxied server so the request information will be correct.

For more information, see our web page on [running Tomcat behind a proxy server](updateme).
It contains links to Tomcat documentation on both `mod_proxy` and `mod_jk` as well as some user contributed documentation on setting up `mod_proxy`.

### I have modified my configuration of a `JoinExisting` Aggregation dataset, but nothing has changed.

The files and coordinates in a JoinExisting Aggregations are cached, and in some circumstances won't get updated.
The default location for the cache is `${tds.content.root.path}/thredds/cache/agg/` unless you change it in the threddsConfig.xml file.
Go to that directory, there will be files with the name of the cached dataset(s).
Delete the file for the dataset that needs updating and restart Tomcat.

### What happened to the long list of CRSs in my WMS GetCapabilities documents?

In TDS 4.1, each WMS `GetCapabilities` document listed 100s of available CRS.
This made for very large GetCapabilities documents.
As of TDS 4.2, this list is limited to a select few CRSs.
We hope to make this list configurable in a future release.

In the mean time if you need a CRS that isn't listed, try specifying it in the `GetMap` request.
The underlying library that handles CRS (Geotoolkit) still supports a large number of CRS and the TDS WMS should still support any of those CRS when requested.

### Why are TDS web forms not working?

Look in `${tomcat_home}/logs/localhost.logs` for error messages like:

~~~
SEVERE: Servlet.service() for servlet GridSubsetService threw exception
    javax.xml.transform.TransformerFactoryConfigurationError: Provider net.sf.saxon.TransformerFactoryImpl not found
~~~

If you find these, the likely problem is that another webapp running in the same Tomcat container has set the XSLT parser with javax.xml.transform.TransformerFactory, which is global for the JVM.
The above example shows that the Saxon parser has been set, but is not being found by the TDS.
We saw this happening with the _OOSTethys_ webapp.

The solution is to move the other webapp to its own Tomcat instance, or to move the required jar (eg saxon.jar) into Tomcat's lib directory, where it is available to all webapps.
TDS does very simple XSLT to create its web forms, so its likely that it can work with any decent XSLT library.
By default it uses the JDK's built-in XSLT library.

### What does the TDS do at startup to read the configuration catalogs? What gets cached? Does it have a way to know a referenced catalog is unchanged? When do referenced catalogs get scanned?

The TDS reads in all the config catalogs at startup.
It caches all of them, and uses the "expires" attribute on the catalog to decide if/when it needs to re-read a catalog.
It must read all catalogs, including catalogRefs, because it has to know what the possible dataset URLs are, and there is no contract that a client has to read a catalog before accessing the dataset.

### How do I change how the TDS logs?

When the TDS is deployed for the first time, the contents of thredds.war will be "exploded" (i.e. extracted) to `${tomcat_home}/webapps/thredds`.
To change how the TDS logs, you'll need to modify `${tomcat_home}/webapps/thredds/WEB-INF/classes/log4j2.xml`, which is a _Log4j 2_ configuration file.

For example, suppose that instead of overwriting server startup messages in `${tds.content.root.path}/thredds/logs/serverStartup.log` from a previous run with new ones (the default behavior), you want to create a new log file for each startup.
In that case, you'd change:

~~~xml
<File name="serverStartupAppender" fileName="${tds.log.dir}/serverStartup.log" append="false">
    <PatternLayout pattern="%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%10r][%8X{ID}] %-5p %c: %m%n"/>
</File>
~~~

to something like:

~~~xml
<RollingFile name="serverStartupAppender" fileName="${tds.log.dir}/serverStartup.log"
      filePattern="${tds.log.dir}/serverStartup.%d{yyyy-MM-dd}_%i.log">
  <PatternLayout pattern="%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%10r][%8X{ID}] %-5p %c: %m%n"/>
  <Policies>
    <OnStartupTriggeringPolicy />
  </Policies>
</RollingFile>
~~~

Be aware that if you install a new `thredds.war` to `${tomcat_home}/webapps`, the exploded directory—including all changes you made to `log4j2.xml` will be removed and the webapp will be redeployed from the new `thredds.war`. We suggest you copy `log4j2.xml` to a different location for the deployment and then copy it back over afterwards.

## Caching

### We use compressed netcdf files and the very first access to them are quite slow, although subsequent accesses are much faster, then become slow again after a while. I can see that TDS uncompress these files to the cdm cache directory, but then they must get deleted. Is there a way to keep them in the cache permanently?

Essentially this is a tradeoff between storage space and the time to decompress.
We assume you don't want to store the files uncompressed, so you have to pay the price of that.
To control how these files are cached, see CDM library Disk cache.
We would suggest that you use:

~~~xml
<DiskCache>
  <alwaysUse>true</alwaysUse>
  <scour>1 hour</scour>
  <maxSize>10 Gb</maxSize>
</DiskCache>
~~~

and choose `maxSize` carefully.
The trick is to make `maxSize` big enough to keep the _working set_ uncompressed, i.e. if there is a relatively small _hot_ set of files that get accessed a lot, you want to give enough cache space to keep them uncompressed in the cache.
Monitor the cache directory closely to see what files stay uncompressed, and how old they are, and modify `maxSize` as needed.

### Since I upgraded, my `joinExisting` aggregation is now very slow. It used to be fast!

`JoinExisting` aggregations need to open each file and extract the coordinates the first time the aggregation is accessed.
The information is cached (by default) in `${tds.content.root.path}/thredds/cache/agg`, so subsequent reads will be much faster.

A change to the default behavior of `DiskCache2` may cause a need to re-read the files.
The `4.3` default was to put all cache files into a single directory, but `4.6` default makes nested directories, because having thousands of files in a single directory is Considered Harmful.
If you need to, you can control that behavior in `threddsConfig.xml`, but better is to pay the price and redo the cache with nested directories.

Note that to get everything in the `joinExisting` cache ahead of time, you just need to make a request for the aggregation coordinate (usually time) values.
You could do it with an OPeNDAP request, or just open the file as a Grid (eg `WMS`, `WCS`, `NCSS`, from `ToolsUI`, `IDV`, etc) which will automatically request all coordinates.
A script to do so is also easy enough, using `wget` or `python` or whatever you like.

Upgrading to a new version is a good time to clear out your caches, if you are installing on top of your old TDS.
Just go to your cache directory (default is `${tds.content.root.path}/thredds/cache`), and delete the entire directory, or if you have the inclination, go and selectively delete old stuff (but then you have to think harder).
Then trigger a repopulation as above.

## TDS Install Errors

### ERROR - TdsContext.init(): Content directory does not exist and could not be created

The TDS needs to create the directory `${tds.content.root.path}/thredds` but it does not have permission.
Make sure `${tds.content.root.path}` is owned and writable by the tomcat user.

### On starting up TDS, I get the error "SEVERE: Error listenerStart" and "SEVERE: Context [/thredds] startup failed due to previous errors", and TDS wont start.

Startup output looks something like:

~~~
1)
log4j:WARN No appenders could be found for logger (org.apache.commons.digester.Digester.sax).
log4j:WARN Please initialize the log4j system properly.

2)
INFO: HTMLManager: start: Starting web application at '/thredds'
TdsConfigContextListener.contextInitialized(): start.


3)
Jul 11, 2011 2:22:12 PM org.apache.catalina.core.StandardContext start SEVERE: Error listenerStart

Jul 11, 2011 2:22:12 PM org.apache.catalina.core.StandardContext start SEVERE: Context [/thredds] startup failed due to previous errors
~~~

where:

1. Harmless log4j warnings. Someday we'll figure out how to get rid of it.
2. Various initialization info messages
3. This is the problem, but it doesn't actually contain enough information to know whats going on.
   It usually means theres an error in how you set up Tomcat.

### What does this error mean: `log4j:ERROR Attempted to append to closed appender named [foobar]`?

The `log4j.xml` file has 2 loggers with the same name, that uses the appender _foobar_.
You must delete one of the loggers.

## Tomcat

### Im getting the error "java.lang.OutOfMemoryError: Java heap space". Whats up?

If you reload the `thredds.war` webapp enough times without restarting Tomcat, you will eventually run into `java.lang.OutOfMemoryError`.
This is a known bug in JDK/Tomcat. The only thing to do is to stop and restart Tomcat.

The other possibility is that you haven't given the TDS enough heap space.\
The default heap size is quite small, so you need to always set this JVM Option (for example, in `setenv.sh`):

~~~
-Xmx4g
~~~

### Im getting the error "java.lang.OutOfMemoryError: PermGen space". Whats up?

The good news is that this problem goes away with Java 8, and we recommend that you switch to Java 8 NOW.

Before Java 8, if you reload the `thredds.war` webapp enough times without restarting Tomcat, you will eventually run into `java.lang.OutOfMemoryError: PermGen space`.
This is a known bug in JDK/Tomcat.
The only thing to do is to stop and restart Tomcat.

You can increase PermGen using this JVM Option (for example, in `setenv.sh`):

~~~
-XX:MaxPermSize=256m
~~~

In Java 6 and 7, the default is `64m`.
However, with enough redeploys , you will eventually run out of PermGen space no matter what your `MaxPermSize` setting is.
We have gotten into the habit of restarting Tomcat on our production server whenever we redeploy.
Lots of redeploys only happen on our test server.

Resources:

["Classloader leaks"](http://blogs.sun.com/fkieviet/entry/classloader_leaks_the_dreaded_java){:target="_blank"} (sun blog) (2006-10-16)
["Return of the PermGen"](http://my.opera.com/karmazilla/blog/2007/09/29/return-of-the-permgen){:target="_blank"} (2007-09-29)
["PermGen Strikes Back"](http://my.opera.com/karmazilla/blog/2007/03/15/permgen-strikes-back){:target="_blank"} (2007-03-15)
["Good Riddance PermGen OutOfMemoryError"](http://my.opera.com/karmazilla/blog/2007/03/13/good-riddance-permgen-outofmemoryerror){:target="_blank"} (2007-03-13)

### During shutdown I'm getting messages about threads (ThreadLocal) having to be shut down to prevent memory leaks. Whats up?

Tomcat memory leak detection code started logging these messages as of Tomcat 6.0.24.
From various posts (see Spring Forum: ["ThreadLocal forcefully removed"](http://forum.springsource.org/showpost.php?p=282738&postcount=3){:target="_blank"} comment #3) it appears that these messages are not a problem but instead a matter of Tomcat finding these objects before they get garbage collected.

Here are a number of related links:

* Spring Forum: ["ThreadLocal forcefully removed"](http://forum.springsource.org/showthread.php?p=282738#post282738){:target="_blank"}. Comment #3 provides an answer to the post.
* [Tomcat Memory Leak Prevention](http://wiki.apache.org/tomcat/MemoryLeakProtection){:target="_blank"} page (in particular, see the ["Custom ThreadLocal"](http://wiki.apache.org/tomcat/MemoryLeakProtection#customThreadLocal){:target="_blank"} section)
* A Tomcat 7 issue on ["Improving ThreadLocal memory leak clean-up"](https://issues.apache.org/bugzilla/show_bug.cgi?id=%2049159){:target="_blank"}

NOTE: We will monitor the status of this Tomcat issue.
For now, we do not consider this a TDS bug and will not be working to fix this issue in TDS.

### Who is accessing my server?

When you examine the TDS access logs, you can see who is accessing the TDS by IP address. Use `nslookup <ip address>` to find out the host name.

### How can I control whether I want Web crawlers to access my server?

Well-behaved web crawlers are supposed to look for a robots.txt file on the server and follow its instructions.
To set up a robots.txt file that excludes web crawlers from crawling your server, [follow these directions](http://www.unidata.ucar.edu/software/thredds/current/tds/reference/Performance.html#robots){:target="_blank"}.

### How can I prevent someone from accessing my server?

If your server is being overwhelmed by requests from a particular user/computer, it is best to exclude them using their IP address rather than their hostname (this avoids having to perform a DNS lookup for each request).
To do so, edit the `${tomcat_home}/conf/server.xml` file and find the `<localhost>` Host element.
Add a RemoteAddrValve Valve element as follows:

~~~xml
<Host name="localhost" debug="0" appBase="webapps" .. >
  <Valve className="org.apache.catalina.valves.RemoteAddrValve" deny="18\.83\.0\.150" />
  ...
</Host>
~~~

The value of the deny attribute must be one or more (comma delimited) regular expressions each of which will be compared to the remote clients IP addresses.
For instance:

~~~xml
deny="18\.83\.0\.150,128\.100\.34\.99,128\.117\.140\..*"
~~~

NOTE: You need to restart the server before this will take effect.

### How do I remove Servlet Autodeploy?

Its recommended to remove autodetection of changes while Tomcat is running, for performance reasons.
In a production environment, its better to explicitly redeploy the application:

~~~xml
<Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false"
  xmlValidation="false" xmlNamespaceAware="false">
  ...
</Host>
~~~

### How do I remove port 8009 when using tomcat in standalone mode?

Unless you are using Tomcat with the Apache server, comment out this line in server.xml:

~~~xml
<Connector port="8009" enableLookups="false" redirectPort="8443" protocol="AJP/1.3" />
~~~

### Manager fails to upload new `thredds.war` with `SizeLimitExceededException`

You are using the Tomcat Manager, which limits the size of war file that can be uploaded.
You can install the war file directly into the tomcat webapps directory (using ssh/scp, for example), or you can change the manager configuration:

~~~
cd ${tomcat_home}/webapps/manager/WEB-INF/
vi web.xml
~~~

change  `<max-file-size>` and  `<max-request-size>` to be larger than the size of the `thredds.war` file.

You must restart tomcat for this to take effect.

### Logging is not working

You must use a version of Tomcat >= 7.0.43. See [log4j2 docs](http://logging.apache.org/log4j/2.0/manual/webapp.html){:target="_blank"}.
