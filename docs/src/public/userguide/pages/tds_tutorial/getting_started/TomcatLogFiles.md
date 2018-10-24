---
title: Tomcat Log Files
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_log_files.html
---

## Tomcat `logs/`

{%include note.html content="
This section assumes you have successfully installed the JDK and Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}

1. Look at the different types of log files being generated in the Tomcat `logs/` directory.
   Move into the `logs/` directory to see the type of information being logged:

   ~~~bash
   $ pwd
   /usr/local/tds/tomcat/bin
   $ cd ../logs

   $ ls -l
   -rw-r----- 1 oxelson ustaff 23451 Oct 24 14:17 catalina.2018-10-24.log
   -rw-r----- 1 oxelson ustaff 23451 Oct 24 14:17 catalina.out
   -rw-r----- 1 oxelson ustaff     0 Oct 24 13:41 host-manager.2018-10-24.log
   -rw-r----- 1 oxelson ustaff  1929 Oct 24 14:17 localhost.2018-10-24.log
   -rw-r----- 1 oxelson ustaff   859 Oct 24 13:45 localhost_access_log.2018-10-24.txt
   -rw-r----- 1 oxelson ustaff     0 Oct 24 13:41 manager.2018-10-24.log
   ~~~

   {% include question.html content="
   Do you see a correspondence between some of the web applications in the Tomcat `webapps/` directory and the naming of certain log files?
   " %}

   {% include question.html content="
   Is there a difference in the information being logged to `catalina.out` versus `catalina.yyyy-mm-dd.log`?
   " %}

   {% include question.html content="
   Are some log files more verbose than others?
   " %}

2. Examining `catalina.out`.
   Open another terminal window (here after referred to as terminal #2) and run the following command in the new terminal:

   ~~~bash
   $ tail -f /usr/local/tds/tomcat/logs/catalina.out
   ~~~

   In your original terminal window, start/stop and start Tomcat and watch what is being logged to `catalina.out` in the terminal #2 window.

   {% include question.html content="
   Is it only errors messages being reported to `catalina.out`?
   " %}

## Things to know about `catalina.out`

{% include note.html content="
The <a href=\"http://marc.info/?l=tomcat-user&w=2&r=1&s=catalina.out+rotate&q=b\" target=\"_blank\">Tomcat Users mailing list</a> has seen a lot of traffic dedicated to `catalina.out` logging and rotation.
" %}

* Tomcat `System.out` and `System.err` gets appended to `catalina.out`. `catalina.out` can quickly grow large if the hosted web applications are not specifically catching and logging `System.out` and `System.err` to designated files.
* `catalina.out` is not automatically rotated in Tomcat.
* You should employ an outside log rotation program (e.g., `logadm` or `logrotate`) to rotate `catalina.out`.
* It is good practice to archive and remove old `catalina.out` files and other log files out of the Tomcat `logs/` on a regular basis.
* On Windows, the `catalina.out` file is not automatically created. Instead only the `catalina.yyyy-mm-dd.log` files are used.
  These have equivalent content.
