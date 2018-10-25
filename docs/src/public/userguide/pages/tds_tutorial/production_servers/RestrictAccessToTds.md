---
title: Restrict Access To The TDS
last_updated: 2018-10-13
sidebar: tdsTutorial_sidebar
toc: false
permalink: restict_access_to_tds.html
---

## Rationale

* Use the Tomcat `RemoteHostValve` or `RemoteAddrValve` to restrict access to the TDS and/or other web applications.
* Configured in the Tomcat `conf/server.xml` file.
* Valve declarations can be used to either allow or deny access to content.
* Utilize the valves for adding an extra layer of security to the `manager` application to limit accessed to it from within a specific IP address range.
* Caveat: these valves rely on incoming IP addresses or hostnames which are vulnerable to spoofing. Also, not much help when dealing with DHCP.

### Examples
1. Using the `RemoteAddrValve` to restrict access based on IP addresses.
   ~~~xml
   <!-- This example denies access based on IP addresses -->
   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          deny="128\.117\.47\.201,128\.107\.157\.210,96\.33\.56\.215" />
   ~~~
2. Using the `RemoteHostValve` to restrict access based on resolved host names.
   ~~~xml
   <!-- This example denies access based on host names -->
   <Valve className="org.apache.catalina.valves.RemoteHostValve"
          deny="www\.badguys\.com,www\.bandwidthhog\.net" />
   ~~~
3. Using wildcard characters.
   ~~~xml
   <!-- Wildcard characters can with the both valves -->
   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          deny="128\.117\.47\..*" />
   ~~~
4. Using the `RemoteAddrValve` to limit access to a specific range of IP addresses.
   ~~~xml
   <!-- This example only allows the specified IPs to access  -->
   <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          allow="128\.117\.140\..*" />
   ~~~

### Resources

* [The Valve Component](https://tomcat.apache.org/tomcat-8.5-doc/config/valve.html){:target="_blank"}
  Tomcat documentation about the various `valve` components available for use.
