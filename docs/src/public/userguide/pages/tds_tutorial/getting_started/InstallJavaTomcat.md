---
title: Installation of Java and Tomcat
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: install_java_tomcat.html
---

## System requirements

{% include note.html content="
Users of GCJ and OS-provided packages (linux) for Java and/or Tomcat may want to reference the <a href=\"http://www.unidata.ucar.edu/mailing_lists/archives/thredds/\" target=\"_blank\">THREDDS mailing list</a> for installation help."
%}

* Oracle Java 8 (latest version)
* Apache Tomcat 8.x

While there are different distributors of Java and servlet containers, Unidata develops, uses and tests the THREDDS Data Server using _Oracle Java_ and the _Apache Tomcat_ servlet container.

## Installing Java

The following example shows installation on a linux system.

1.  [Download](http://www.oracle.com/technetwork/java/javase/downloads/){:target="_blank"} current Java SE Developer Kit (JDK) from Oracle. Use the latest 1.8 version of the JDK.

2.  Install the JDK as per the Oracle [installation instructions](http://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (/usr/local/tds/ in this example):

    ~~~ bash
    $ pwd
    /usr/local/tds
    $ cp ~/downloads/jdk-8u192-linux-x64.tar.gz .

    $ ls -l
    total 187268
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Unpack the archive file:

    ~~~ bash
    tar xvfz jdk-8u192-linux-x64.tar.gz 
    ~~~

    This will extract the JDK in the installation directory:

    ~~~ bash
    $ ls -l
    total 187272
    drwxr-xr-x 7 oxelson ustaff      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    {% include important.html content="
    Depending on your OS you may need install either the 32-bit or 64-bit version of the JDK.
    But, we *really, really, really recommend* you use a 64-bit OS if you're planning to run the THREDDS Data Server.
    " %}

## Installing Tomcat

{% include note.html content="
For more information about installing Tomcat on Windows OS, see the <a href=\"http://tomcat.apache.org/tomcat-8.5-doc/setup.html#Windows\" target=\"_blank\">Tomcat setup guide</a> for installation on different platforms.
" %}

The following example shows installation on a linux system (also will work for Mac OS):

1.  [Download](http://tomcat.apache.org/download-80.cgi){:target="_blank"} current version of the Tomcat 8.5 servlet container.

2.  Install Tomcat as per the Apache Tomcat [installation instructions](http://tomcat.apache.org/tomcat-8.5-doc/setup.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (/usr/local/tds/ in this example):

    ~~~ bash
    $ pwd
    /usr/local/tds
    $ cp ~/downloads/apache-tomcat-8.5.34.tar.gz .

    $ ls -l
    total 196676
    -rw-r--r-- 1 oxelson ustaff   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 oxelson ustaff      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Unpack the archive file:

    ~~~ bash
    $ tar xvfz apache-tomcat-8.5.34.tar.gz
    ~~~

    This will create a Tomcat directory:

    ~~~ bash
    $ ls -l
    total 196680
    drwxr-xr-x 9 oxelson ustaff      4096 Oct 24 13:29 apache-tomcat-8.5.34
    -rw-r--r-- 1 oxelson ustaff   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 oxelson ustaff      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

## Create Symbolic Links

Use symbolic links for both tomcat and the JDK to make your life easier so that Tomcat and JDK upgrades will not require changes to configuration files.

    ~~~ bash
    $ pwd
    /usr/local/tds
    
    $ ln -s apache-tomcat-8.5.34 tomcat
    
    $ ls -l
    total 196680
    drwxr-xr-x 9 oxelson ustaff      4096 Oct 24 13:29 apache-tomcat-8.5.34
    -rw-r--r-- 1 oxelson ustaff   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 oxelson ustaff      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    lrwxrwxrwx 1 oxelson ustaff        20 Oct 24 13:58 tomcat -> apache-tomcat-8.5.34    
      
    $ ln -s jdk1.8.0_192 jdk
    
    $ ls -l 
    total 196680
    drwxr-xr-x 9 oxelson ustaff      4096 Oct 24 13:29 apache-tomcat-8.5.34
    -rw-r--r-- 1 oxelson ustaff   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    lrwxrwxrwx 1 oxelson ustaff        12 Oct 24 13:59 jdk -> jdk1.8.0_192
    drwxr-xr-x 7 oxelson ustaff      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

{%include note.html content="
Windows users can consult the <a href=\"https://docs.microsoft.com/en-us/windows/desktop/fileio/symbolic-links\" target=\"_blank\">Microsoft Documentation</a> for creating symbolic links on Windows systems.
" %}
