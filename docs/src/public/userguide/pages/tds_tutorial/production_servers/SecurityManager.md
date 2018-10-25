---
title: Running Tomcat With A Security Manager
last_updated: 2018-10-13
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_with_security_manager.html
---

### Rationale

* The JVM Security Manager that comes with Tomcat imposes a fine-grained security restrictions to all Java applications running the JVM.
* It confines the Java applications in a sandbox, and restricts them from utilizing certain features of the Java language Tomcat normally is able to access.
* If you are hosting untrusted servlets or JSP on your server, then implementing the Security Manager may be a good idea.
* Be aware the Security Manager may prevent trusted web applications (like the TDS) from performing certain functions if configured too restrictively.

### Resources

[Security Manager HOW-TO](https://docs.oracle.com/javase/tutorial/essential/environment/security.html){:target="_blank"}

Information on the default settings of the Java Security Manager and instructions on how to make changes to these settings.
