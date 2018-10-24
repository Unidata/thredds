---
title: Deploying the TDS 
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: deploying_the_tds.html
---

## About WAR files

{%include note.html content="
This section assumes you have successfully installed the JDK and Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}

* WAR is short for Web ARchive.
* By default, Tomcat will automatically unpack the WAR distribution into directory of the same name upon deployment.
* <i>The unpacked directory is overwritten each time a new WAR file is deployed.</i>

## Downloading & deploying thredds.war

1. [Download](http://www.unidata.ucar.edu/downloads/thredds/index.jsp){:target="_blank"} the TDS WAR file (`tds-5.0.0-beta5.war`) from Unidata's web site.

2. Rename the WAR file to `thredds.war`.

    ~~~bash
   $ cd /home/oxelson/downloads
      
   $ ls -l
   total 274828
   -rw-r--r-- 1 oxelson ustaff   9625824 Sep 20 12:18 apache-tomcat-8.5.34.tar.gz
   -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:18 jdk-8u192-linux-x64.tar.gz
   -rw-r--r-- 1 oxelson ustaff  80027070 Oct 24 14:42 tds-5.0.0-beta5.war
   
   $ mv tds-5.0.0-beta5.war thredds.war
   
   $ ls -l  
   total 274828
   -rw-r--r-- 1 oxelson ustaff   9625824 Sep 20 12:18 apache-tomcat-8.5.34.tar.gz
   -rw-r--r-- 1 oxelson ustaff 191757099 Oct 24 13:18 jdk-8u192-linux-x64.tar.gz
   -rw-r--r-- 1 oxelson ustaff  80027070 Oct 24 14:42 thredds.war
   ~~~


3. Deploy the TDS in Tomcat.
   Put `thredds.war` in the Tomcat `webapps/` directory:

   ~~~bash
   $ cd /usr/local/tds/tomcat/webapps/
   
   $ ls -l
   total 20
   drwxr-x--- 14 oxelson ustaff 4096 Oct 24 13:29 docs
   drwxr-x---  6 oxelson ustaff 4096 Oct 24 13:29 examples
   drwxr-x---  5 oxelson ustaff 4096 Oct 24 13:29 host-manager
   drwxr-x---  5 oxelson ustaff 4096 Oct 24 13:29 manager
   drwxr-x---  3 oxelson ustaff 4096 Oct 24 13:29 ROOT
    
   $ cp ~/downloads/thredds.war .
   $ ls -l
   total 78172
   drwxr-x--- 14 oxelson ustaff     4096 Oct 24 13:29 docs
   drwxr-x---  6 oxelson ustaff     4096 Oct 24 13:29 examples
   drwxr-x---  5 oxelson ustaff     4096 Oct 24 13:29 host-manager
   drwxr-x---  5 oxelson ustaff     4096 Oct 24 13:29 manager
   drwxr-x---  3 oxelson ustaff     4096 Oct 24 13:29 ROOT
   -rw-r--r--  1 oxelson ustaff 80027070 Oct 24 14:51 thredds.war
   ~~~

4. Confirm the TDS has been deployed.

   If Tomcat is already running, wait a couple of seconds after placing the WAR file in the Tomcat `webapps/` and then verify the `thredds.war` file was unpacked:

   ~~~bash
   $ ls -l
   total 78176
   drwxr-x--- 14 oxelson ustaff     4096 Oct 24 13:29 docs
   drwxr-x---  6 oxelson ustaff     4096 Oct 24 13:29 examples
   drwxr-x---  5 oxelson ustaff     4096 Oct 24 13:29 host-manager
   drwxr-x---  5 oxelson ustaff     4096 Oct 24 13:29 manager
   drwxr-x---  3 oxelson ustaff     4096 Oct 24 13:29 ROOT
   drwxr-x---  8 oxelson ustaff     4096 Oct 24 14:51 thredds
   -rw-r--r--  1 oxelson ustaff 80027070 Oct 24 14:51 thredds.war
   ~~~

   Go to [http://localhost:8080/thredds/](http://localhost:8080/thredds/){:target="_blank"} in your browser to verify the TDS has been deployed:

   {% include image.html file="tds/tutorial/getting_started/default_cat.png" alt="THREDDS Distribution Catalog" caption="THREDDS Distribution Catalog" %}


5. Confirm the creation of the TDS `content/` directory.

   Move into `${tomcat_home}` and do a long listing:

   ~~~bash
   $ pwd
   /usr/local/tds/tomcat/webapps
   $ cd ..

   $ ls -l
    total 148
    drwxr-x--- 2 oxelson ustaff  4096 Oct 24 14:22 bin
    -rw-r----- 1 oxelson ustaff 19539 Sep  4 16:30 BUILDING.txt
    drwx------ 3 oxelson ustaff  4096 Oct 24 13:41 conf
    drwxr-x--- 3 oxelson ustaff  4096 Oct 24 14:43 content
    -rw-r----- 1 oxelson ustaff  6090 Sep  4 16:30 CONTRIBUTING.md
    drwxr-x--- 2 oxelson ustaff  4096 Oct 24 13:29 lib
    -rw-r----- 1 oxelson ustaff 57092 Sep  4 16:30 LICENSE
    drwxr-x--- 2 oxelson ustaff  4096 Oct 24 13:41 logs
    -rw-r----- 1 oxelson ustaff  1726 Sep  4 16:30 NOTICE
    -rw-r----- 1 oxelson ustaff  3255 Sep  4 16:30 README.md
    -rw-r----- 1 oxelson ustaff  7142 Sep  4 16:30 RELEASE-NOTES
    -rw-r----- 1 oxelson ustaff 16262 Sep  4 16:30 RUNNING.txt
    drwxr-x--- 3 oxelson ustaff  4096 Oct 24 14:43 temp
    drwxr-x--- 8 oxelson ustaff  4096 Oct 24 14:51 webapps
    drwxr-x--- 3 oxelson ustaff  4096 Oct 24 13:41 work
   ~~~

   {%include troubleshooting.html content="
   Any error in the TDS deployment will be reported in the `catalina.out` file of the Tomcat `logs/` directory.
   " %}

   {%include troubleshooting.html content="
   Be sure you have downloaded and deployed the correct version of the TDS.
   The TDS version number will appear in the blue bar at the bottom of TDS catalog pages.
   " %}