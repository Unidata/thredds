---
title: Installation of Java and Tomcat
last_updated: 2018-10-26
sidebar: tdsTutorial_sidebar
toc: false
permalink: install_java_tomcat.html
---

This section demonstrates the installation of the Java JDK and the Tomcat Servlet Container binaries.  

{% include note.html content="
Users of OS-provided packages via package management systems for Java and/or Tomcat may want to reference the <a href=\"https://www.unidata.ucar.edu/mailing_lists/archives/thredds/\" target=\"_blank\">THREDDS mailing list</a> for installation help."
%}

## System Requirements

* Oracle Java 8 (latest version)
* Apache Tomcat 8.x

While there are different distributors of Java and servlet containers, Unidata develops, uses and tests the THREDDS Data Server using _Oracle Java_ and the _Apache Tomcat_ servlet container.


## Installing Java JDK

The following example shows the JDK installation on a linux system.  The installation is being performed as the `root` user.

{% include note.html content="
For installation of the JDK on Windows or Mac OS, see the <a href=\"https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html\" target=\"_blank\">JDK Installation Guide</a>.
" %}

1.  [Download](http://www.oracle.com/technetwork/java/javase/downloads/){:target="_blank"} current Java SE Developer Kit (JDK) from Oracle. Use the latest 1.8 version of the JDK.

2.  Install the JDK as per the Oracle [installation instructions](http://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html){:target="_blank"}.

    Copy the binary `tar.gz` file into the installation directory (`/usr/local` in this example):

    ~~~bash
    # pwd
    /usr/local
    
    # cp /tmp/jdk-8u192-linux-x64.tar.gz .

    # ls -l
    total 187268
    -rw-r--r-- 1 root root 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Unpack the archive file:

    ~~~bash
    # tar xvfz jdk-8u192-linux-x64.tar.gz 
    ~~~

    This will extract the JDK in the installation directory:

    ~~~bash
    # ls -l
    total 187272
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    -rw-r--r-- 1 root root 191757099 Oct 24 13:19 jdk-8u192-linux-x64.tar.gz
    ~~~

    Remove the remaining binary `tar.gz` file when the installation is complete.
   
    ~~~bash
    # rm jdk-8u192-linux-x64.tar.gz
    # ls -l
    total 187279
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    {% include important.html content="
    Depending on your OS you may need install either the 32-bit or 64-bit version of the JDK.
    But, we *really, really, really recommend* you use a 64-bit OS if you're planning to run the THREDDS Data Server.
    " %}

<a name="tomcat"></a>
## Installing the Tomcat Servlet Container

The following example shows Tomcat installation on a linux system. (This type of installation will work on Mac OS systems as well.) The installation is being performed as the `root` user.

{% include note.html content="
For installation of Tomcat on Windows, see the <a href=\"http://tomcat.apache.org/tomcat-8.5-doc/setup.html#Windows\" target=\"_blank\">Tomcat Setup Guide</a>.
" %}

1.  [Download](http://tomcat.apache.org/download-80.cgi){:target="_blank"} current version of the Tomcat 8.5 servlet container.

2.  Install Tomcat as per the Apache Tomcat [installation instructions](http://tomcat.apache.org/tomcat-8.5-doc/setup.html){:target="_blank"}.

    Copy the binary tar.gz file into the installation directory (`/usr/local` in this example):

    ~~~bash
    # pwd
    /usr/local
    
    # cp /tmp/apache-tomcat-8.5.34.tar.gz .

    # ls -l
    total 196676
    -rw-r--r-- 1 root root   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    Unpack the archive file:

    ~~~bash
    # tar xvfz apache-tomcat-8.5.34.tar.gz
    ~~~

    This will create a Tomcat directory:

    ~~~bash
    # ls -l
    total 196680
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    -rw-r--r-- 1 root root   9625824 Oct 24 13:27 apache-tomcat-8.5.34.tar.gz
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

    Remove the remaining binary `tar.gz` file when the installation is complete.
   
    ~~~bash
    # rm apache-tomcat-8.5.34.tar.gz
    # ls -l
    total 187282
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

## Create Symbolic Links

Adding symbolic links for both the Tomcat and the JDK installations will allow for upgrades of both packages without having to change to configuration files and server startup/shutdown scripts.

The following example shows creating symbolic links for the Tomcat and JDK installation on a linux system. (This type of installation will work on Mac OS systems as well.) The installation is being performed as the `root` user.

{%include note.html content="
Windows users can consult the <a href=\"https://docs.microsoft.com/en-us/windows/desktop/fileio/symbolic-links\" target=\"_blank\">Microsoft Documentation</a> for creating symbolic links on Windows systems.
" %}

1. Create symbolic links for the Tomcat and the JDK installations:

    ~~~ bash
    # pwd
    /usr/local
    
    # ln -s apache-tomcat-8.5.34 tomcat 
    # ln -s jdk1.8.0_192 jdk
    # ls -l 
    total 196684
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 tomcat -> apache-tomcat-8.5.34
    drwxr-xr-x 9 root root      4096 Oct 24 13:29 apache-tomcat-8.5.34
    lrwxrwxrwx 1 root root        12 Oct 24 13:59 jdk -> jdk1.8.0_192
    drwxr-xr-x 7 root root      4096 Oct  6 07:58 jdk1.8.0_192
    ~~~

## Next Step

Next, we'll do a quick tour of the relevant elements of the [Tomcat Directory Structure](tomcat_dir_structure_qt.html) and how these elements relate to the TDS.