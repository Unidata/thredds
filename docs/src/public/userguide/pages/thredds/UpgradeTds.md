---
title: Recommended Process for Upgrading a TDS
last_updated: 2018-10-24
sidebar: tdsTutorial_sidebar
toc: false
permalink: upgrade_tds_ref.html
---

When upgrading a THREDDS Data Server (TDS), it is important to fully undeploy the running TDS before deploying the new TDS.
Not doing so can cause conflicts between old and new Java classes and/or JSP pages (among other things).
The reason for this is that Tomcat and other webapp containers have working directories, such as `${tomcat.home}/work/Catalina/localhost/thredds`, in which generated files are stored for use (like compiled JSP pages).

So, here is the process we follow when we upgrade the TDS on our systems (we use Tomcat, but the process should be similar for other webapp containers):

* Use the Tomcat manager app to undeploy the TDS
  * https://server:port/manager/html/
* Shutdown Tomcat
* Clean up and archive any log files
  * ${tomcat.home}/logs/*
  * ${tds.content.root.path}/thredds/logs/*
* Startup Tomcat
* Use the Tomcat manager to deploy the new TDS