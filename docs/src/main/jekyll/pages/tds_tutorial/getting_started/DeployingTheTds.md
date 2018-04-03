---
title: Deploying the TDS 
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: deploying_the_tds.html
---

## About WAR files

* WAR is short for Web ARchive.
* By default, Tomcat will automatically unpack the WAR distribution into directory of the same name upon deployment.
* Note: the unpacked directory is overwritten each time a new WAR file is deployed.

## Downloading & deploying thredds.war

1. [Download](http://www.unidata.ucar.edu/downloads/thredds/index.jsp){:target="_blank"} the TDS WAR file (`thredds.war`) from Unidata's web site.


2. Deploy the TDS in Tomcat.
   Put `thredds.war` in the Tomcat webapps/ directory:

   ~~~bash
   $ pwd
   /usr/local/tds/apache-tomcat-8.0.24/bin
   $ cd ../webapps

   $ mv ~/thredds.war .
   $ ls -l
   drwxr-xr-x 13 tds workshop     4096 Jul 15 09:37 docs
   drwxr-xr-x  6 tds workshop     4096 Jul 15 09:37 examples
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 host-manager
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 manager
   drwxr-xr-x  3 tds workshop     4096 Jul 15 09:37 ROOT
   -rw-r--r--  1 tds workshop 33218655 Jul 15 09:50 thredds.war
   ~~~

3. Confirm the TDS has been deployed.

   If Tomcat is already running, wait a couple of seconds after placing the WAR file in the Tomcat webapps/ and then verify the thredds.war file was unpacked:

   ~~~bash
   $ ls -l
   drwxr-xr-x 13 tds workshop     4096 Jul 15 09:37 docs
   drwxr-xr-x  6 tds workshop     4096 Jul 15 09:37 examples
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 host-manager
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 manager
   drwxr-xr-x  3 tds workshop     4096 Jul 15 09:37 ROOT
   drwxr-xr-x  8 tds workshop 4096 Jul 15 09:51 thredds
   -rw-r--r--  1 tds workshop 33218655 Jul 15 09:50 thredds.war
   ~~~

   Go to [http://localhost:8080/thredds/](http://localhost:8080/thredds/){:target="_blank"}  in your browser to verify the TDS has been deployed:

   {% include image.html file="tds/tutorial/getting_started/default_cat.png" alt="THREDDS Distribution Catalog" caption="THREDDS Distribution Catalog" %}


4. Confirm the creation of the TDS `content/` directory.

   Move into `${tomcat_home}` and do a long listing:

   ~~~bash
   $ pwd
   /usr/local/tds/apache-tomcat-8.0.24/webapps
   $ cd ..

   $ ls -l
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:51 bin
   drwxr-xr-x 3 tds workshop  4096 Jul 15 09:55 conf
   drwxr-xr-x 3 tds workshop 4096 Jul 15 09:52 content
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:37 lib
   -rw-r--r-- 1 tds workshop 56812 Jul  2 01:59 LICENSE
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:39 logs
   -rw-r--r-- 1 tds workshop  1192 Jul  2 01:59 NOTICE
   -rw-r--r-- 1 tds workshop  8826 Jul  2 01:59 RELEASE-NOTES
   -rw-r--r-- 1 tds workshop 16262 Jul  2 01:59 RUNNING.txt
   drwxr-xr-x 2 tds workshop  4096 Jul 15 09:55 temp
   drwxr-xr-x 8 tds workshop  4096 Jul 15 09:51 webapps
   drwxr-xr-x 3 tds workshop  4096 Jul 15 09:39 work
   ~~~

   {%include troubleshooting.html content="
   Any error in the TDS deployment will be reported in the catalina.out file of the Tomcat `logs/` directory.
   " %}

   {%include troubleshooting.html content="
   Be sure you have downloaded and deployed the correct version of the TDS.
   The TDS version number will appear in the blue bar at the bottom of TDS catalog pages.
   " %}

{%include note.html content="
Upgrading the TDS: A [maintenance checklist](updateme.html) contains helpful information about upgrading the TDS.
[New features](updateme.html) and [configuration changes](updateme.html) made between TDS versions are listed for each release.
" %}
