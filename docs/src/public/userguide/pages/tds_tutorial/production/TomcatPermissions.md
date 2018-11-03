---
title: Restrict Tomcat Permissions
last_updated: 2018-11-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_permissions.html
---
This section demonstrates how to restrict the permissions of Tomcat Servlet Container.

{%include note.html content="
This section assumes you have successfully performed the tasks as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Getting Started With The TDS</a> section of this tutorial.
" %}

## Rationale

The JVM doesn’t fork at all, nor does it support `setuid()` calls.
The JVM, and therefore Tomcat, is _one_ process.
The JVM is a virtual machine with many threads under the same process.

* Because of OS constraints, all threads in the same JVM process must run under the same user id.
  No thread may run as the `root` user unless they are **all** are run as the `root` user.
  Hence, any programs run in Tomcat (TDS, manager application, other JSPs and servlets) will run as the `root` user.
* If you _choose_ to run the Tomcat process as the `root` user and an attacker manages to exploit a weakness in Tomcat or something running in `$TOMCAT_HOME/webapps/` to run arbitrary commands, those commands will be run as the **superuser**!

{%include warning.html content="
We strongly discourage running Tomcat as the `root` user and recommend creating an unprivileged, dedicated user and group for running the Tomcat process.
" %}

## Create A Dedicated User/Group For Running Tomcat

The following example shows creation of a dedicated user/group on a linux system. (Windows and Mac OS users will need to consult their systems administrator regarding user/group creation for those operating systems.)

In this example, both the user and group names will be named `tomcat`, and the user\'s home directory, a.k.a. `$TOMCAT_HOME`, is `/usr/local/tomcat`.
Both the `groupadd` and `useradd` commands are run as the `root` user:

~~~~bash
# groupadd tomcat
# useradd -g tomcat -d /usr/local/tomcat tomcat
~~~~
    
You should see and entry for a `tomcat` user in your `/etc/group` file:
    
~~~~bash
tomcat:x:2001:
~~~~
    
And something like the following in your `/etc/passwd` file:
    
~~~~bash
tomcat:x:25945:2001::/usr/local/tomcat:/bin/bash
~~~~

## Restrict Permissions In `$TOMCAT_HOME`

We also recommend restricting the permissions of the Tomcat `user/group` within `$TOMCAT_HOME`.

1. Change the user/group ownership `$TOMCAT_HOME` to the `tomcat` user and `tomcat` group:

   ~~~~bash
   # cd /usr/local
   # chown -R tomcat:tomcat apache-tomcat-8.5.34
   # ls -l tomcat
   total 148
   drwxr-x--- 2 tomcat tomcat  4096 Oct 24 14:22 bin
   -rw-r----- 1 tomcat tomcat 19539 Sep  4 16:30 BUILDING.txt
   drwx------ 2 tomcat tomcat  4096 Oct 24 15:14 conf
   -rw-r----- 1 tomcat tomcat  6090 Sep  4 16:30 CONTRIBUTING.md
   drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:29 lib
   -rw-r----- 1 tomcat tomcat 57092 Sep  4 16:30 LICENSE
   drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:41 logs
   -rw-r----- 1 tomcat tomcat  1726 Sep  4 16:30 NOTICE
   -rw-r----- 1 tomcat tomcat  3255 Sep  4 16:30 README.md
   -rw-r----- 1 tomcat tomcat  7142 Sep  4 16:30 RELEASE-NOTES
   -rw-r----- 1 tomcat tomcat 16262 Sep  4 16:30 RUNNING.txt
   drwxr-x--- 3 tomcat tomcat  4096 Oct 24 14:43 temp
   drwxr-x--- 8 tomcat tomcat  4096 Oct 24 15:36 webapps
   drwxr-x--- 3 tomcat tomcat  4096 Oct 24 13:41 work
   ~~~~
   
2. Change the user/ownership of the `$TOMCAT_HOME/conf` directory to be owned by the `root` user, have a group of `tomcat` and have a permission of user/group read only:

    ~~~~bash
    # cd /usr/local/tomcat
    # chown -R root conf
    # ls -l 
    total 148
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 14:22 bin
    -rw-r----- 1 tomcat tomcat 19539 Sep  4 16:30 BUILDING.txt
    drwx------ 2 root   tomcat  4096 Sep  4 16:30 conf
    -rw-r----- 1 tomcat tomcat  6090 Sep  4 16:30 CONTRIBUTING.md
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:29 lib
    -rw-r----- 1 tomcat tomcat 57092 Sep  4 16:30 LICENSE
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:41 logs
    -rw-r----- 1 tomcat tomcat  1726 Sep  4 16:30 NOTICE
    -rw-r----- 1 tomcat tomcat  3255 Sep  4 16:30 README.md
    -rw-r----- 1 tomcat tomcat  7142 Sep  4 16:30 RELEASE-NOTES
    -rw-r----- 1 tomcat tomcat 16262 Sep  4 16:30 RUNNING.txt
    drwxr-x--- 3 tomcat tomcat  4096 Oct 24 14:43 temp
    drwxr-x--- 8 tomcat tomcat  4096 Oct 24 15:36 webapps
    drwxr-x--- 3 tomcat tomcat  4096 Oct 24 13:41 work
    ~~~~
   
3. Give the `tomcat` group write/execute permissions for the `$TOMCAT_HOME/conf` directory.

    ~~~~bash
    # chmod 750 conf
    # ls -l 
    total 148
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 14:22 bin
    -rw-r----- 1 tomcat tomcat 19539 Sep  4 16:30 BUILDING.txt
    drwxr-x--- 2 root   tomcat  4096 Sep  4 16:30 conf
    -rw-r----- 1 tomcat tomcat  6090 Sep  4 16:30 CONTRIBUTING.md
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:29 lib
    -rw-r----- 1 tomcat tomcat 57092 Sep  4 16:30 LICENSE
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 13:41 logs
    -rw-r----- 1 tomcat tomcat  1726 Sep  4 16:30 NOTICE
    -rw-r----- 1 tomcat tomcat  3255 Sep  4 16:30 README.md
    -rw-r----- 1 tomcat tomcat  7142 Sep  4 16:30 RELEASE-NOTES
    -rw-r----- 1 tomcat tomcat 16262 Sep  4 16:30 RUNNING.txt
    drwxr-x--- 3 tomcat tomcat  4096 Oct 24 14:43 temp
    drwxr-x--- 8 tomcat tomcat  4096 Oct 24 15:36 webapps
    drwxr-x--- 3 tomcat tomcat  4096 Oct 24 13:41 work
    ~~~~
   
   
4. Change the user/group permissions of the files and subdirectories in `$TOMCAT_HOME/conf` directory. 
   
   (Depending on the web applications you are running and/or your virtual host configurations, Tomcat may create a `$TOMCAT_HOME/conf/Catalina` directory with corresponding subdirectories and files for [context](https://tomcat.apache.org/tomcat-8.5-doc/virtual-hosting-howto.html#Configuring_Your_Contexts){:target="_blank"} information.)  
                                                                                                                                   
    ~~~~bash
    # cd /usr/local/tomcat/conf
    # find . -type f -print -exec chmod 440 {} \;
    # find . -type d -print -exec chmod 750 {} \;
    # ls -l 
    total 228
    drwxr-x--- 3 root tomcat   4096 Oct 24 13:41 Catalina
    -r--r----- 1 root tomcat  13548 Sep  4 16:30 catalina.policy
    -r--r----- 1 root tomcat   7746 Sep  4 16:30 catalina.properties
    -r--r----- 1 root tomcat   1338 Sep  4 16:30 context.xml
    -r--r----- 1 root tomcat   1149 Sep  4 16:30 jaspic-providers.xml
    -r--r----- 1 root tomcat   2313 Sep  4 16:30 jaspic-providers.xsd
    -r--r----- 1 root tomcat   3622 Sep  4 16:30 logging.properties
    -r--r----- 1 root tomcat   7511 Sep  4 16:30 server.xml
    -r--r----- 1 root tomcat   1993 Oct 24 15:14 tomcat-users.xml
    -r--r----- 1 root tomcat   2633 Sep  4 16:30 tomcat-users.xsd
    -r--r----- 1 root tomcat 169322 Sep  4 16:30 web.xml
    ~~~~
 
5. Change the user/ownership of the `$TOMCAT_HOME/bin` and `$TOMCAT_HOME/lib` directories to be owned by the `root` user and have a group of `tomcat`:
    ~~~~bash
    # cd /usr/local/tomcat
    # chown -R root lib
    # chown -R root bin
    # ls -l
    total 144
    drwxr-x--- 2 root   tomcat  4096 Oct 24 17:39 bin
    -rw-r----- 1 tomcat tomcat 19539 Sep  4 16:30 BUILDING.txt
    drwxr-x--- 2 root   tomcat  4096 Sep  4 16:30 conf
    -rw-r----- 1 tomcat tomcat  6090 Sep  4 16:30 CONTRIBUTING.md
    drwxr-x--- 2 root   tomcat  4096 Oct 24 17:38 lib
    -rw-r----- 1 tomcat tomcat 57092 Sep  4 16:30 LICENSE
    drwxr-x--- 2 tomcat tomcat  4096 Sep  4 16:28 logs
    -rw-r----- 1 tomcat tomcat  1726 Sep  4 16:30 NOTICE
    -rw-r----- 1 tomcat tomcat  3255 Sep  4 16:30 README.md
    -rw-r----- 1 tomcat tomcat  7142 Sep  4 16:30 RELEASE-NOTES
    -rw-r----- 1 tomcat tomcat 16262 Sep  4 16:30 RUNNING.txt
    drwxr-x--- 2 tomcat tomcat  4096 Oct 24 17:38 temp
    drwxr-x--- 7 tomcat tomcat  4096 Oct 24 17:39 webapps
    drwxr-x--- 2 tomcat tomcat  4096 Sep  4 16:28 work
    ~~~~

    {%include important.html content="
    If you are not planning to use the Tomcat Manager application, you may consider changing the ownership of the files in the `webapps` directory to belong to another under-privileged user.
    " %}

6. Change the user/group permissions of the TDS `$CONTENT_ROOT` directory to give the `tomcat` user and `tomcat` group permissions to write content (`$CONTENT_ROOT` is `/data/content` in this example):
    ~~~~bash
    # cd /data
    # chown -R tomcat:tomcat content
    # ls -l
    total 144
    drwxr-x--- 3 tomcat tomcat  4096 Oct 24 17:43 content
    ~~~~

 
## Resources

* [Tomcat as root and security issues](https://marc.info/?t=104516038700003&r=1&w=2){:target="_blank"}
  A lengthy thread in the tomcat-users mailing list archives dedicated to the perils of running Tomcat as the root user.