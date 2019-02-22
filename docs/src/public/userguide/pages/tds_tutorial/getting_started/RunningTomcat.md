---
title: Running Tomcat
last_updated: 2018-10-26
sidebar: tdsTutorial_sidebar
toc: false
permalink: running_tomcat.html
---

This section examines starting/stopping the Tomcat Servlet Container as well as configuring the JVM for the TDS.


{%include note.html content="
This section assumes you have successfully installed the Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}

## Starting & Stopping Tomcat

The following example shows stopping/starting Tomcat on a linux system, as the `root` user. (This example will work on Mac OS systems as well. For a Windows installation, use the `.bat` files in place of the `.sh` scripts used in the provided examples.)

1. Tomcat isn’t currently running so we need to start it up.

   Run the `startup.sh` script in the `$TOMCAT_HOME/bin` directory (`$TOMCAT_HOME` is `/usr/local` in this example):

   ~~~ bash
   # pwd
   /usr/local/tomcat

   # bin/startup.sh
   ~~~

2. Verify Tomcat is running.

   Look and see if you have a Tomcat process running:

   ~~~bash
   # ps -ef | grep tomcat
   root   4293     1 99 14:04 pts/2    00:00:06 /usr/local/jdk/bin/java -Djava.util.logging.config.file=/usr/local/tomcat/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Djdk.tls.ephemeralDHKeySize=2048 -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -Dorg.apache.catalina.security.SecurityListener.UMASK=0027 -Dignore.endorsed.dirs= -classpath /usr/local/tomcat/bin/bootstrap.jar:/usr/local/tomcat/bin/tomcat-juli.jar -Dcatalina.base=/usr/local/tomcat -Dcatalina.home=/usr/local/tomcat -Djava.io.tmpdir=/usr/local/tomcat/temp org.apache.catalina.startup.Bootstrap start
   root   4366 23720  0 14:04 pts/2    00:00:00 grep tomcat
   ~~~

   Open a new browser window/tab and go to [http://localhost:8080/](http://localhost:8080/){:target="_blank"} to verify Tomcat is running:

   {% include image.html file="tds/tutorial/getting_started/congratulations.png" alt="Tomcat Default Home Page" caption="If you see this page, Tomcat is running!" %}


3. See if you can shutdown Tomcat.

   Run the `shutdown.sh` script in the `$TOMCAT_HOME/bin` directory (`$TOMCAT_HOME` is `/usr/local` in this example):

   ~~~bash
   # pwd
   /usr/local/tomcat
      
   # bin/shutdown.sh
   ~~~


{% include question.html content="
Which Java is Tomcat currently using? (Hint: what was sent to `STDOUT` when running `startup.sh` and `shutdown.sh`?)
" %}

{% include troubleshooting.html content="
Check the logs mostly recently generated in the `$TOMCAT_HOME/logs` directory for clues about why Tomcat failed to start or stop.
Pay particular attention to what is being reported in Tomcat's main log file: `catalina.out`.
" %}

## Setting `$JAVA_HOME`, `$JAVA_OPTS`, `$CATALINA_HOME`, `$CATALINA_BASE`, and `$CONTENT_ROOT`

We are going to create a file called `setenv.sh` in the `$TOMCAT_HOME/bin` directory to:

* allow Tomcat to reference/find the location of `$JAVA_HOME` and `$CATALINA_BASE`) during startup and shutdown;

* increase the amount of memory allocated to the JVM to enhance performance by setting `$JAVA_OPTS`; and

* add additional settings to the JVM via `$JAVA_OPTS` to enable more advanced services in the TDS (e.g, WMS, etc).

Tomcat's `$TOMCAT_HOME/bin/startup.sh` script executes the `catalina.sh` script found in the same directory.  `catalina.sh` is the main control script for the Tomcat Servlet Container which is executed on server startup and shutdown (also called from the `$TOMCAT_HOME/bin/shutdown.sh` script).
 
When executed, the `catalina.sh` script will look for a `setenv.sh` in the `$TOMCAT_HOME/bin` directory.  If it finds `setenv.sh`, it will apply the custom environment and JVM configurations specified within the file.  (Thus, saving you the trouble of directly modifying and potentially introducing errors in the important `catalina.sh` script).


{%include note.html content="
If you’re running Tomcat on an instance of Windows OS, you will want to create a `setenv.bat` file.
" %}


1. Create the `setenv.sh` file.

   Use your favorite text editor to create a new file called `setenv.sh` in the `$TOMCAT_HOME/bin` directory (`$TOMCAT_HOME` is `/usr/local` in this example):

   ~~~bash
   # cd /usr/local/tomcat/bin
   # vi setenv.sh
   ~~~
   
   Add the following information to you `setenv.sh` file and save it:

   ~~~bash
   #!/bin/sh
   #
   # ENVARS for Tomcat
   #
   export CATALINA_HOME="/usr/local/tomcat"

   export CATALINA_BASE="/usr/local/tomcat"

   export JAVA_HOME="/usr/local/jdk"

   # TDS specific ENVARS
   #
   # Define where the TDS content directory will live
   #   THIS IS CRITICAL and there is NO DEFAULT - the
   #   TDS will not start without this.
   #
   CONTENT_ROOT=-Dtds.content.root.path=/data/content

   # Set java prefs related variables (used by the wms service, for example)
   JAVA_PREFS_ROOTS="-Djava.util.prefs.systemRoot=$CONTENT_ROOT/thredds/javaUtilPrefs \
                     -Djava.util.prefs.userRoot=$CONTENT_ROOT/thredds/javaUtilPrefs"

   #
   # Some commonly used JAVA_OPTS settings:
   #
   NORMAL="-d64 -Xmx4096m -Xms512m -server -ea"
   HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError"
   HEADLESS="-Djava.awt.headless=true"

   #
   # Standard setup.
   #
   JAVA_OPTS="$CONTENT_ROOT $NORMAL $HEAP_DUMP $HEADLESS $JAVA_PREFS_ROOTS"

   export JAVA_OPTS
   ~~~

   {% include important.html content="
   Whenever possible, Unidata recommends `-Xmx4096m` (or more) for 64-bit systems.
   " %}

   The parameters we pass to `$JAVA_OPTS`:

    * `CONTENT_ROOT` is TDS-specific, and defines the location of where TDS-related configuration files will be stored. **This MUST be set!  The TDS will not start without it.**  It is also a good idea to locate this directory somewhere separate from `$TOMCAT_HOME` on your file system.
    * `-Xms` is the initial and minimum allocated memory of the JVM (for performance).
    * `-Xmx` the maximum allocated memory of the JVM (for performance).
    * `-server` tells the Hotspot compiler to run the JVM in "server" mode (for performance).
    * `-Djava.awt.headless=true` is needed to prevent graphics rendering code from assuming a graphics console exists.
      Without this, WMS code will crash the server in some circumstances.
    * `-Djava.util.prefs.systemRoot=$CONTENT_ROOT/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=$CONTENT_ROOT/thredds/javaUtilPrefs` allows the java.util.prefs of the TDS WMS to write system preferences to a location that is writable by the Tomcat user.

    {%include note.html content="
    For more information about the possible options/arguments available for `$JAVA_OPTS`, please consult the <a href=\"https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html#BABDJJFI\" target=\"_blank\">Oracle Documentation</a>.
    " %}


2. Implement your changes by restarting Tomcat.

   Restart Tomcat and examine the output generated to the terminal window by the startup script:

   ~~~bash
   # ./startup.sh
   Using CATALINA_BASE:   /usr/local/tomcat
   Using CATALINA_HOME:   /usr/local/tomcat
   Using CATALINA_TMPDIR: /usr/local/tomcat/temp
   Using JRE_HOME:        /usr/local/jdk
   Using CLASSPATH:       /usr/local/tomcat/bin/bootstrap.jar:/usr/local/tomcat/bin/tomcat-juli.jar
   Tomcat started.
   ~~~

   {% include question.html content="
   Did you notice any difference in the what is being reported to STDOUT during startup?
   " %}

   Take a look at the running Tomcat process to see the new `$JAVA_OPTS` settings:

   ~~~bash
   # ps -ef | grep tomcat
   root   7988     1 13 14:17 pts/2    00:00:05 /usr/local/jdk/bin/java -Djava.util.logging.config.file=/usr/local/tomcat/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Dtds.content.root.path=/usr/local/tomcat/content -d64 -Xmx4096m -Xms512m -server -ea -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Djava.util.prefs.systemRoot=/usr/local/tomcat/content/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=/usr/local/tomcat/content/thredds/javaUtilPrefs -Djdk.tls.ephemeralDHKeySize=2048 -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -Dorg.apache.catalina.security.SecurityListener.UMASK=0027 -Dignore.endorsed.dirs= -classpath /usr/local/tomcat/bin/bootstrap.jar:/usr/local/tomcat/bin/tomcat-juli.jar -Dcatalina.base=/usr/local/tomcat -Dcatalina.home=/usr/local/tomcat -Djava.io.tmpdir=/usr/local/tomcat/temp org.apache.catalina.startup.Bootstrap start
   root   8279 23720  0 14:18 pts/2    00:00:00 grep tomcat
   ~~~

   {% include note.html content="
   For more information on the environment variable prerequisites used by Tomcat, consult `$TOMCAT_HOME/bin/catalina.sh (or catalina.bat)` file."
   %}

## Troubleshooting

 * Some platforms may require the `$TOMCAT_HOME/bin/setenv.sh` file to have executable permissions (this issue will manifest itself as permission errors in the log files).
 * Do not forget include the `m` in your `-Xms` and `-Xmx` settings.
 * You may have allocated too much memory for the JVM settings if Tomcat fails to start and you get the following error reported in the Tomcat log `catalina.out`:

   ~~~
   Error occurred during initialization of VM
   Could not reserve enough space for object heap
   ~~~

 * Likewise, if there is an error with your JVM memory allocation syntax in the `setenv.sh` file, it will be reported to `catalina.out`:

   ~~~
   Error occurred during initialization of VM
   Incompatible minimum and maximum heap sizes specified
   ~~~

 * If you intend to use WMS and see something like the following in reported in `catalina.out`:

   ~~~
   May 25, 2010 6:28:22 PM java.util.prefs.FileSystemPreferences syncWorld
   WARNING: Couldn't flush system prefs: java.util.prefs.BackingStoreException: /etc/.java/.systemPrefs/org create failed.
   ~~~

   You will need confirm `java.util.prefs.systemRoot` system property is set in `$JAVA_OPTS` to a location that is writable by the user that Tomcat, e.g.:

   ~~~bash
   # Set java prefs related variables (used by the wms service, for example)
   JAVA_PREFS_ROOTS="-Djava.util.prefs.systemRoot=$CONTENT_ROOT/thredds/javaUtilPrefs \
                     -Djava.util.prefs.userRoot=$CONTENT_ROOT/thredds/javaUtilPrefs"
   ~~~

## Next Step

Next, we'll examine the [log files](tomcat_log_files.html) generated by the Tomcat Servlet Container and the information found in them.