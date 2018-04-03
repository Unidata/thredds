---
title: Installation of Java and Tomcat
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: install_java_tomcat.html
---

## System requirements

{% include note.html content="
Users of GCJ and OS-provided packages (linux) for Java and/or Tomcat may want to reference the [THREDDS mailing list](http://www.unidata.ucar.edu/mailing_lists/archives/thredds/) for installation help."
%}

* Oracle Java 8 (latest version)
* Apache Tomcat 8.x

While there are different distributors of Java and servlet containers, Unidata develops, uses and tests the THREDDS Data Server using _Oracle Java_ and the _Apache Tomcat_ servlet container.

## Installing Java

1.  http://www.oracle.com/technetwork/java/javase/downloads/[Download] current Java SE Developer Kit (JDK) from Oracle. Use the latest 1.8 version of the JDK.

2.  Install the JDK as per the Oracle [installation instructions](http://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (/usr/local/tds/ in this example):

    ~~~ bash
    $ pwd
    /usr/local/tds
    $ cp Downloads/jdk-8u51-linux-x64.tar.gz .

    $ ls -l
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Desktop
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Documents
    drwxr-xr-x  2 tds workshop     4096 Jul 22 17:29 Downloads
    -rw-r--r-- 1 tds workshop 96316511 Jul 15 09:30 jdk-8u51-linux-x64.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Music
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Pictures
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Public
    drwxr-xr-x  3 tds workshop     4096 Jul 15 10:45 tdsMonitor
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Templates
    -rwxrwxr-x  1 tds workshop 27866303 Jul 15 10:02 toolsUI-4.3.18.jar
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Videos
    ~~~

    Unpack the archive file:

    ~~~ bash
    tar zxvf jdk-8u51-linux-x64.tar.gz
    ~~~

    This will extract the JDK in the installation directory:

    ~~~ bash
    $ ls -l
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Desktop
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Documents
    drwxr-xr-x  2 tds workshop     4096 Jul 23 14:40 Downloads
    drwxr-xr-x 8 tds workshop 4096 Jun 5 22:07 jdk1.8u51
    -rw-r--r--  1 tds workshop 96316511 Jul 15 09:30 jdk-8u51-linux-x64.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Music
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Pictures
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Public
    drwxr-xr-x  3 tds workshop     4096 Jul 15 10:45 tdsMonitor
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Templates
    -rwxrwxr-x  1 tds workshop 27866303 Jul 15 10:02 toolsUI-4.3.18.jar
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Videos
    ~~~

    {% include important.html content="
    Depending on your OS you may need install either the 32-bit or 64-bit version of the JDK.
    But, we *really, really, really recommend* you use a 64-bit OS if you're planning to run the THREDDS Data Server.
    " %}

## Installing Tomcat

{% include note.html content="
For more information about installing Tomcat on Windows OS, see the [Tomcat setup guide](http://tomcat.apache.org/tomcat-8.0-doc/setup.html#Windows) for installation on different platforms.
" %}

1.  [Download](http://tomcat.apache.org/download-80.cgi){:target="_blank"} current version of the Tomcat 8 servlet container.
2.  Install Tomcat as per the Apache Tomcat [installation instructions](http://tomcat.apache.org/tomcat-8.0-doc/setup.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (/usr/local/tds/ in this example):

    ~~~ bash
    $ pwd
    /usr/local/tds

    $ ls -l
    -rw-r--r-- 1 tds workshop 7955948 Jul 15 09:35 apache-tomcat-8.0.24.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Desktop
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Documents
    drwxr-xr-x  2 tds workshop     4096 Jul 23 14:40 Downloads
    drwxr-xr-x  8 tds workshop     4096 Jun  5 22:07 jdk1.8u51
    -rw-r--r--  1 tds workshop 96316511 Jul 15 09:30 jdk-8u51-linux-x64.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Music
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Pictures
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Public
    drwxr-xr-x  3 tds workshop     4096 Jul 15 10:45 tdsMonitor
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Templates
    -rwxrwxr-x  1 tds workshop 27866303 Jul 15 10:02 toolsUI-4.3.18.jar
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Videos
    ~~~

    Unpack the archive file:

    $ tar xvzf apache-tomcat-8.0.24.tar.gz
    This will create a Tomcat directory:

    ~~~ bash
    $ ls -l
    drwxr-xr-x 9 tds workshop 4096 Jul 15 09:37 apache-tomcat-8.0.24
    -rw-r--r-- 1 tds workshop  7955948 Jul 15 09:35 apache-tomcat-8.0.24.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Desktop
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Documents
    drwxr-xr-x  2 tds workshop     4096 Jul 23 14:40 Downloads
    drwxr-xr-x  8 tds workshop     4096 Jun  5 22:07 jdk1.8u51
    -rw-r--r--  1 tds workshop 96316511 Jul 15 09:30 jdk-8u51-linux-x64.tar.gz
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Music
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Pictures
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Public
    drwxr-xr-x  3 tds workshop     4096 Jul 15 10:45 tdsMonitor
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Templates
    -rwxrwxr-x  1 tds workshop 27866303 Jul 15 10:02 toolsUI-4.3.18.jar
    drwxr-xr-x  2 tds workshop     4096 Jul 15 10:33 Videos
    ~~~
