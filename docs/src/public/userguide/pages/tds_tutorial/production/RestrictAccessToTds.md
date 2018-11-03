---
title: Restrict Access To The TDS
last_updated: 2018-11-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: restict_access_to_tds.html
---

This section demonstrates how to restrict access to the TDS and other web applications using Tomcat Valves.

{%include note.html content="
This section assumes you have successfully performed the tasks as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Getting Started With The TDS</a> section of this tutorial.
" %}

## Rationale

* Use the Tomcat `RemoteHostValve` or `RemoteAddrValve` to restrict access to the TDS and/or other web applications.
* Configured in the Tomcat `$TOMCAT_HOME/conf/server.xml` or web application `META-INF/context.xml` files.
* Valve declarations can be used to either allow or deny access to content.
* Utilize the valves for adding an extra layer of security to the Manager application to limit accessed to it from within a specific IP address range.
* Caveat: these valves rely on incoming IP addresses or hostnames which are vulnerable to spoofing. Also, not much help when dealing with DHCP.

## `RemoteAddrValve`

The `RemoteAddrValve` compares the client IP address against one or more regular expressions to either allow or refuse the request from this client.

#### Examples

1. Using the `RemoteAddrValve` to allow access only for the clients connecting from localhost:

   ~~~~xml
   <!-- This example only allows access from localhost -->
   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1"/>
   ~~~~
   
2. Using the `RemoteAddrValve` to allow unrestricted access for the clients connecting from localhost but for all other clients only to port 8443:
   ~~~~xml
   <!-- This example allows 8080 access from localhost but all other connections must use 8443  -->
   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          addConnectorPort="true"
          allow="127\.\d+\.\d+\.\d+;\d*|::1;\d*|0:0:0:0:0:0:0:1;\d*|.*;8443"/>
   ~~~~

## `RemoteHostValve`

The `RemoteHostValve` compares the client hostname against one or more regular expressions to either allow or refuse the request from this client.

#### Example

1. Using the `RemoteHostValve` to to restrict access based on resolved host names:

   ~~~~xml
   <!-- This example denies access based on host names -->
   <Valve className="org.apache.catalina.valves.RemoteHostValve"
              deny=".*\.bandwidthhogs\.com" />
   ~~~~  

{%include note.html content="
Consult the Tomcat <a href=\"https://tomcat.apache.org/tomcat-8.5-doc/config/valve.html#Remote_Host_Valve\" target=\"_blank\">Remote Host Valve</a> documentation for more information about valve syntax and options.
" %}


## Resources

* [The Valve Component](https://tomcat.apache.org/tomcat-8.5-doc/config/valve.html){:target="_blank"}
  Tomcat documentation about the various `valve` components available for use.
