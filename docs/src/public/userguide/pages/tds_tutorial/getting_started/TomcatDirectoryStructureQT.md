---
title: Tomcat Directory Structure - Quick Tour
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_dir_structure_qt.html
---

## Exploring the Tomcat Directory Structure

1. Examine the Tomcat directory structure, a.k.a. `${tomcat_home}`.
   Move into `${tomcat_home}` and do a long listing:
    
   ~~~bash
   $ cd apache-tomcat-8.0.24
   $ ls -l
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:37 bin
   drwxr-xr-x 2 tds workshop  4096 Jul  2 01:59 conf
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:37 lib
   -rw-r--r-- 1 tds workshop 56812 Jul  2 01:59 LICENSE
   drwxr-xr-x 2 tds workshop  4096 Jul  2 01:57 logs
   -rw-r--r-- 1 tds workshop  1192 Jul  2 01:59 NOTICE
   -rw-r--r-- 1 tds workshop  8826 Jul  2 01:59 RELEASE-NOTES
   -rw-r--r-- 1 tds workshop 16262 Jul  2 01:59 RUNNING.txt
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:37 temp
   drwxr-xr-x 7 tds workshop  4096 Jul  2 01:59 webapps
   drwxr-xr-x 2 tds workshop  4096 Jul  2 01:57 work
   ~~~

2. Familiarize yourself with the following important directories.

   `bin/`

   * Contains startup.sh, shutdown.sh and other scripts/programs.
   * The `*.sh` files (for Unix systems) are functional duplicates of the `*.bat` files (for Windows systems).

   `conf/`

   * _Server-wide_ Tomcat configuration.
   * You will modify `server.xml` and `tomcat-users.xml` to adjust logging, authentication and access control, enable SSL, etc.
   * Web applications can override some server-wide settings in their own configuration files (more about that later).

   `webapps/`

   Contains web applications directories and WAR files.
   This is where we will be putting the TDS web application.
   You will also be using the `manager` application that comes with Tomcat during this workshop.

   `logs/`

   * Tomcat log files are here by default.
   * This is one of the directories you will be looking for log files (the TDS logs elsewhere by default).
   * The log files should be your first stop for troubleshooting Tomcat and TDS issues. (Hint, hint.)
   * Logs files may contain useful information for assessing the security of your system.
   * You will become very familiar with the Tomcat-generated `catalina.out`, `catalina.yyyy-mm-dd.log`, and `localhost_access_log.yyyy-mm-dd.log` files by the end of this workshop.
