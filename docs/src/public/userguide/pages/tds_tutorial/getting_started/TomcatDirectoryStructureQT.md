---
title: Tomcat Directory Structure - Quick Tour
last_updated: 2018-10-26
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_dir_structure_qt.html
---

This section examines the directory structure and files found in the Tomcat Servlet Container installation and how they relate to the TDS.

{%include note.html content="
This section assumes you have successfully installed the Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}

## Exploring The Tomcat Directory Structure

Examine the Tomcat directory structure, a.k.a. `#TOMCAT_HOME`.  Move into `$TOMCAT_HOME` and do a long listing (`/usr/local` in this example):
    
~~~bash
# cd /usr/local/tomcat
# ls -l

total 144
drwxr-x--- 2 root root  4096 Oct 24 13:29 bin
-rw-r----- 1 root root 19539 Sep  4 16:30 BUILDING.txt
drwx------ 2 root root  4096 Sep  4 16:30 conf
-rw-r----- 1 root root  6090 Sep  4 16:30 CONTRIBUTING.md
drwxr-x--- 2 root root  4096 Oct 24 13:29 lib
-rw-r----- 1 root root 57092 Sep  4 16:30 LICENSE
drwxr-x--- 2 root root  4096 Sep  4 16:28 logs
-rw-r----- 1 root root  1726 Sep  4 16:30 NOTICE
-rw-r----- 1 root root  3255 Sep  4 16:30 README.md
-rw-r----- 1 root root  7142 Sep  4 16:30 RELEASE-NOTES
-rw-r----- 1 root root 16262 Sep  4 16:30 RUNNING.txt
drwxr-x--- 2 root root  4096 Oct 24 13:29 temp
drwxr-x--- 7 root root  4096 Sep  4 16:29 webapps
drwxr-x--- 2 root root  4096 Sep  4 16:28 work
~~~

Familiarize yourself with these important directories:

#### `bin/`

* Contains `startup.sh`, `shutdown.sh` and other scripts/programs.
* The `*.sh` files (for Unix and Mac OS systems) are functional duplicates of the `*.bat` files (for Windows systems).

#### `conf/`

* _Server-wide_ Tomcat configuration.
* You will modify `server.xml` and `tomcat-users.xml` to adjust logging, authentication and access control, enable TLS/SSL, etc.
* Web applications can override some server-wide settings in their own configuration file (e.g., the web deployment descriptor).

#### `webapps/`

* Contains web applications directories and WAR files.
* This is where we will be putting the TDS web application.
* You will also be using the `manager` application that comes with the Tomcat Servlet Container during this tutorial.

#### `logs/`

* Tomcat log files are here by default.
* This is one of the directories you will be looking for log files (the TDS logs elsewhere by default).
* The log files should be your first stop for troubleshooting Tomcat and TDS issues. (_Hint, hint_).
* Logs files may contain useful information for assessing the security of your system.
* Make a point of becoming familiar with the Tomcat-generated `catalina.out`, `catalina.yyyy-mm-dd.log`, and `localhost_access_log.yyyy-mm-dd.log` files.

## Next Step

Next, we'll look at starting/stopping the Tomcat Servlet Container as well as [configuring the JVM](running_tomcat.html) for the TDS.