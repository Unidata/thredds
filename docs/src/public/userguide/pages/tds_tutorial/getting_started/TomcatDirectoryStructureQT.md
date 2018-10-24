---
title: Tomcat Directory Structure - Quick Tour
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_dir_structure_qt.html
---

## Exploring the Tomcat Directory Structure

{%include note.html content="
This section assumes you have successfully installed the Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}


1. Examine the Tomcat directory structure, a.k.a. `${tomcat_home}`.
   Move into `${tomcat_home}` and do a long listing:
    
   ~~~bash
   $ cd /usr/local/tds/tomcat
   $ ls -l
   total 144
   drwxr-x--- 2 oxelson ustaff  4096 Oct 24 13:29 bin
   -rw-r----- 1 oxelson ustaff 19539 Sep  4 16:30 BUILDING.txt
   drwx------ 2 oxelson ustaff  4096 Sep  4 16:30 conf
   -rw-r----- 1 oxelson ustaff  6090 Sep  4 16:30 CONTRIBUTING.md
   drwxr-x--- 2 oxelson ustaff  4096 Oct 24 13:29 lib
   -rw-r----- 1 oxelson ustaff 57092 Sep  4 16:30 LICENSE
   drwxr-x--- 2 oxelson ustaff  4096 Sep  4 16:28 logs
   -rw-r----- 1 oxelson ustaff  1726 Sep  4 16:30 NOTICE
   -rw-r----- 1 oxelson ustaff  3255 Sep  4 16:30 README.md
   -rw-r----- 1 oxelson ustaff  7142 Sep  4 16:30 RELEASE-NOTES
   -rw-r----- 1 oxelson ustaff 16262 Sep  4 16:30 RUNNING.txt
   drwxr-x--- 2 oxelson ustaff  4096 Oct 24 13:29 temp
   drwxr-x--- 7 oxelson ustaff  4096 Sep  4 16:29 webapps
   drwxr-x--- 2 oxelson ustaff  4096 Sep  4 16:28 work
   ~~~

2. Familiarize yourself with the following important directories.

   `bin/`

   * Contains startup.sh, shutdown.sh and other scripts/programs.
   * The `*.sh` files (for Unix systems) are functional duplicates of the `*.bat` files (for Windows systems).

   `conf/`

   * _Server-wide_ Tomcat configuration.
   * You will modify `server.xml` and `tomcat-users.xml` to adjust logging, authentication and access control, enable TSL/SSL, etc.
   * Web applications can override some server-wide settings in their own configuration files (more about that later).

   `webapps/`

   * Contains web applications directories and WAR files.
   * This is where we will be putting the TDS web application.
   * You will also be using the `manager` application that comes with Tomcat during this workshop.

   `logs/`

   * Tomcat log files are here by default.
   * This is one of the directories you will be looking for log files (the TDS logs elsewhere by default).
   * The log files should be your first stop for troubleshooting Tomcat and TDS issues. (Hint, hint.)
   * Logs files may contain useful information for assessing the security of your system.
   * You will become very familiar with the Tomcat-generated `catalina.out`, `catalina.yyyy-mm-dd.log`, and `localhost_access_log.yyyy-mm-dd.log` files by the end of this workshop.
