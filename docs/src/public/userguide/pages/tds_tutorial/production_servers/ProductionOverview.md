---
title: Production Overview
last_updated: 2018-10-13
sidebar: tdsTutorial_sidebar
toc: false
permalink: production_server_overview.html
---

## What This Section Covers

This section covers Best practices and recommendations on securing a production TDS/Tomcat server.
You should know:
 * how to do a basic installation of [Tomcat, Java](install_java_tomcat.html) , and the [TDS](deploying_the_tds.html)
 * be familiar with the [Tomcat directory structure](tomcat_dir_structure_qt.html)
 * how to [configure Tomcat ](tomcat_configuration_files.html)
 * how to use the [Tomcat manager application](tomcat_manager_app.html)

## Why Is Security Important?

Be afraid

* Misconfiguration of Tomcat can introduce vulnerabilities in security.
* The recommendations in this section should be considered \"layers\" of security: not completely effective by themselves, but more potent when combined.
* The topics covered in this section should not be considered a complete laundry list of security fixes! Please use it as a starting point when securing your server.
  The ordering of the topics in this section are not a representation of the section importance.

## Keeping Software Versions Up-To-Date



## Tomcat Process User/Group and ${tomcat_home} Permissions

## Removing Unused Web Applications



## Using Digested Passwords


## Enabling TSL/SSL Encryption



## Securing the Tomcat manager Application



## Blocking Non-Essential Port Access At The Firewall


## Restricting Access To The TDS By Remote IP Address Or Host


## Reverse Proxy

### Rationale

* A reverse proxy is a proxy server that appears to clients to be an ordinary server.
  Requests are forwarded to one or more origin servers which handle the request.
  The response is returned as if it came directly from the proxy server.

  {% include image.html file="tds/tutorial/production_servers/overview/tds_reverse_proxy.png" alt="reverse proxy" caption="" %}

* Reverse proxies can be used to hide the existence and characteristics of the origin server(s) and can be an additional layer of defense and can protect against some OS and web server specific attacks.
  This additional security layer forces an attacker to attack the proxy because the firewall allows only the proxy to communicate with the back-end content servers.
* However, it does not provide any protection to attacks against vulnerabilities in the web application or proxy service itself (e.g., Apache, Tomcat).
* If an attacker can use the front-end proxy server to launch an attack on the back-end servers if he/she manages to exploit the web application, proxy transaction or some other service running on the proxy server.
  
#### Resources

[Running The TDS Behind a Proxy Server](tds_behind_proxy.html){:target="_blank"}
How to set up a reverse proxy for the TDS using Tomcat and the Apache HTTP server.

## Running Tomcat with a Security Manager

### Rationale

* The JVM Security Manager that comes with Tomcat imposes a fine-grained security restrictions to all Java applications running the JVM.
* It confines the Java applications in a sandbox, and restricts them from utilizing certain features of the Java language Tomcat normally is able to access.
* If you are hosting untrusted servlets or JSP on your server, then implementing the Security Manager may be a good idea.
* Be aware the Security Manager may prevent trusted web applications (like the TDS) from performing certain functions if configured too restrictively.

### Resources

[Security Manager HOW-TO](https://docs.oracle.com/javase/tutorial/essential/environment/security.html){:target="_blank"}

Information on the default settings of the Java Security Manager and instructions on how to make changes to these settings.

## Protecting the Tomcat `SHUTDOWN` Port

### `SHUTDOWN` on port 8005

Tomcat uses a the default port of 8005 as the designated shutdown port.
Shutdown scripts make a call to this port and issue the `SHUTDOWN` command.
If need be, you can always change the shutdown command or even the port number in `${tomcat_home}/conf/server.xml`.