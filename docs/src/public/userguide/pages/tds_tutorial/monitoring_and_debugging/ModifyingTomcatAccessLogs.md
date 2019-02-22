---
title: Modifying Tomcat Access Logs
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_access_log.html
---

{% include image.html file="tds/tutorial/monitoring_and_debugging/log.png" alt="Log!" caption="" %}

## Tomcat Access Logs

{%include note.html content="
This section assumes you are familiar with the <a href=\"install_java_tomcat.html\" target=\"_blank\">JDK and Tomcat Servlet Container</a>, have <a href=\"deploying_the_tds.html\" target=\"_blank\">deployed the TDS</a>, and are comfortable with basic and advanced TDS configuration.
" %}

* The access log records all requests processed by the server.
* As of Tomcat 7, enabled in Tomcat by default in `${tomcat_home}/conf/server.xml`.
* Information it contains is different from other logs in `${tomcat_home}/logs`.
* Used for monitoring who is using your server and as a way of obtaining "feedback" about the activity and performance of the server.
* In order to use the [`TdsMonitor`](using_the_tdsmonitor_tool.html){:target="_blank"}  tool, you will need to change the default configuration of the `AccessLogValve`.

### Modifying Tomcat Access Logging For The `TdsMonitor`

Modify the `prefix`, `suffix`, and `pattern` attributes of the `AccessLogValve` element.

1. Using your favorite editor open `${tomcat_home}/conf/server.xml`:

   ~~~bash
   $ vi server.xml
   ~~~

   Locate the `AccessLogValve` contained in the `Host` element (should be near the bottom of the file):

   ~~~xml
   <!-- Define the default virtual host
           Note: XML Schema validation will not work with Xerces 2.2.
  -->
   <Host name="localhost"  appBase="webapps"
         unpackWARs="true" autoDeploy="true">

     <!-- SingleSignOn valve, share authentication between web applications
          Documentation at: /docs/config/valve.html -->
     <!--
     <Valve className="org.apache.catalina.authenticator.SingleSignOn" />
     -->

     <!-- Access log processes all example.
       Documentation at: /docs/config/valve.html
       Note: The pattern used is equivalent to using pattern="common" -->
       <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
       prefix="localhost_access_log." suffix=".txt"
       pattern="%h %l %u %t "%r" %s %b" />

   </Host>
   ~~~

   Change the `prefix` and `suffix` attributes to customize the access log name. (The `TdsMonitor` tool looks for log files to begin with "access".)

   ~~~xml
   prefix="access"
   suffix=".log"
   ~~~

   To provide more useful information about who is accessing the TDS, change the pattern element to customize the format of each log entry:

   ~~~xml
   pattern="%h %l %u %t &quot;%r&quot; %s %b &quot;%{Referer}i&quot; &quot;%{User-Agent}i&quot; %D"
   ~~~

   When you are finished with your edits, the `AccessLogValve` should look something like the following:

   ~~~xml
   <Valve className="org.apache.catalina.valves.AccessLogValve"
      directory="logs"  
      prefix="access"
      suffix=".log"
      pattern="%h %l %u %t &quot;%r&quot; %s %b &quot;%{Referer}i&quot; &quot;%{User-Agent}i&quot; %D" />
   ~~~

2. Verify the changes to the access log have taken affect.
   Restart Tomcat and verify an access log has been generated in the `${tomcat_home}/logs/` directory:

   ~~~bash
   $ ls -l /usr/local/tds/tomcat/logs
   total 164
   -rw-r----- 1 tomcat tomcat     0 Oct 25 07:06 access.2018-10-25.log
   -rw-r----- 1 tomcat tomcat 58233 Oct 24 19:58 catalina.2018-10-24.log
   -rw-r----- 1 root   root    2453 Oct 25 07:06 catalina.2018-10-25.log
   -rw-r----- 1 tomcat tomcat 72826 Oct 25 07:06 catalina.out
   -rw-r----- 1 tomcat tomcat     0 Oct 24 17:43 host-manager.2018-10-24.log
   -rw-r----- 1 tomcat tomcat     0 Oct 25 07:06 host-manager.2018-10-25.log
   -rw-r----- 1 tomcat tomcat  6874 Oct 24 19:58 localhost.2018-10-24.log
   -rw-r----- 1 root   root     566 Oct 25 07:06 localhost.2018-10-25.log
   -rw-r----- 1 tomcat tomcat  3660 Oct 24 19:58 localhost_access_log.2018-10-24.txt
   -rw-r----- 1 tomcat tomcat  1355 Oct 24 19:58 manager.2018-10-24.log
   -rw-r----- 1 tomcat tomcat     0 Oct 25 07:06 manager.2018-10-25.log
   ~~~

### Access log format

The access log entry format we are using is almost identical to the standard combined logging format with an addition: the %D which is used for documenting the Time taken to process the request, in milliseconds will appear at the end of each log entry:

~~~xml
pattern="%h %l %u %t &quot;%r&quot; %s %b &quot;%{Referer}i&quot; &quot;%{User-Agent}i&quot; %D"
~~~

### Access log format

{%include note.html content="
For more information on access log format configuration, see the Tomcat <a href=\"http://tomcat.apache.org/tomcat-8.5-doc/config/valve.html\" target=\"_blank\">Valve Component</a> Documentation.
" %}


The above pattern makes use of the following codes:

 * `%h` - Remote host name (or IP address if resolveHosts is false)
 * `%l` - Remote logical username from identd (always returns '-')
 * `%u` - Remote user that was authenticated (if any), else '-'
 * `%t` - Date and time, in Common Log Format
 * `%r` - First line of the request (method and request URI)
 * `%s` - HTTP status code of the response
 * `%b` - Bytes sent, excluding HTTP headers, or '-' if zero
 * `%D` - Time taken to process the request, in millis

The above pattern translates into:

~~~bash
127.0.0.1 - admin [25/Oct/2018:07:12:49 -0600] "GET /manager/html HTTP/1.1" 200 19930 "-" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" 16
~~~

Another way of looking at it:

|---------|---------|-------|
| pattern | meaning | value |
|:--------|:--------|:------|
| `%h` | Remote host  | 127.0.0.1 |
| `%l` | Remote logical username from identd | - |
| `%u` | Authenticated user       | admin |
| `%t` | Time and date of request | [25/Oct/2018:07:12:49 -0600] |
| `%r` | HTTP request method      | GET |
| `%r` | Request URI              | /manager/html |
| `%r` | Protocol used            | HTTP/1.1 |
| `%s`  | HTTP server response    | 200 |
| `%b` | Bytes transferred        | 17578 |
| `%{Referer}` | Referer          | - |
| `%{User-Agent}`| User Agent | Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" |
| `%D` | Response time (in milliseconds) | 16 |
