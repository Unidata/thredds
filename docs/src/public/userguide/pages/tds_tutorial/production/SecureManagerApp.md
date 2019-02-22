---
title: Secure the Tomcat Manager Application
last_updated: 2018-11-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: secure_manager_app.html
---

This section demonstrates how to secure the Tomcat Manager Application by enabling access via TLS/SSL Encryption.

{%include note.html content="
This section assumes you have successfully performed the tasks as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Getting Started With The TDS</a> and <a href=\"enable_tls_encryption.html\" target=\"_blank\">Enabling TLS/SSL</a> sections of this tutorial
" %}

**If you do not intend to use the Tomcat Manager Application, you can skip this section and proceed to [removing unused web applications](remove_unused_webapps.html){:target="_blank"}.**

## Rationale

The Tomcat The Manager application:
* \"Free\" web application that comes with Tomcat distribution that lives in the Tomcat Lives in the `$TOMCAT_HOME/webapps/manager` directory.
* Not enabled by default.
* Allows Tomcat administrators to deploy, un-deploy, or reload web applications such as the TDS without having to shut down and restart Tomcat.
* If exploited, an attacker can use the Manager application to install programs on your server willy-nilly.
* If you choose to enable the Manager application, we _highly recommend_ [enabling digested passwords](digested_passwords.html){:target="_blank"} and [TLS/SSL encryption](enable_tls_encryption.html){:target="_blank"} for the Manager application.
* Restricting access to the Manager application to a small [subset of IP addresses or host names using a Tomcat valve](restict_access_to_tds.html){:target="_blank"} is also a good idea.
* **Uninstall this application if you don't plan to use it.**

## Enabling TLS/SSL For The Tomcat Manager Application

The following example demonstrates enabling TLS/SSL for the Tomcat Manager Application on a linux system as the `root` user.

1. Modify the deployment descriptor (`$TOMCAT_HOME/webapps/manager/WEB-INF/web.xml` )of the Tomcat Manager application.

   Use your favorite editor to open the deployment descriptor for the Tomcat Manager application:
   
   ~~~~bash
   # vi $TOMCAT_HOME/webapps/manager/WEB-INF/web.xml
   ~~~~

   Locate the `<security-constraint>` elements (around line ~122):

   ~~~~xml
   <!-- Define a Security Constraint on this Application -->
   <!-- NOTE:  None of these roles are present in the default users file -->
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>HTML Manager interface (for humans)</web-resource-name>
       <url-pattern>/html/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-gui</role-name>
     </auth-constraint>
   </security-constraint>
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>Text Manager interface (for scripts)</web-resource-name>
       <url-pattern>/text/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-script</role-name>
     </auth-constraint>
   </security-constraint>
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>JMX Proxy interface</web-resource-name>
       <url-pattern>/jmxproxy/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-jmx</role-name>
     </auth-constraint>
   </security-constraint>
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>Status interface</web-resource-name>
       <url-pattern>/status/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-gui</role-name>
        <role-name>manager-script</role-name>
        <role-name>manager-jmx</role-name>
        <role-name>manager-status</role-name>
     </auth-constraint>
   </security-constraint>
   ~~~~

   The Tomcat 8.x version of the Manager application deployment descriptor contains a `<security-constraint>` section for each of the four possible `ContactPaths` (as per [Manager Application](https://tomcat.apache.org/migration.html){:target="_blank"} section of the Tomcat Migration Guide).

   Add a `<user-data-constraint>` with a `<transport-guarantee>` of `CONFIDENTIAL` for the desired `ContactPaths` to to enable port-forwarding to port `8443`:

   ~~~~xml
   <!-- Define a Security Constraint on this Application -->
   <!-- NOTE:  None of these roles are present in the default users file -->
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>HTML Manager interface (for humans)</web-resource-name>
       <url-pattern>/html/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-gui</role-name>
     </auth-constraint>
    <user-data-constraint>
      <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
   </security-constraint>

   <security-constraint>
     <web-resource-collection>
       <web-resource-name>Text Manager interface (for scripts)</web-resource-name>
       <url-pattern>/text/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-script</role-name>
     </auth-constraint>
    <user-data-constraint>
      <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
   </security-constraint>

   <security-constraint>
     <web-resource-collection>
       <web-resource-name>JMX Proxy interface</web-resource-name>
       <url-pattern>/jmxproxy/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-jmx</role-name>
     </auth-constraint>
    <user-data-constraint>
      <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
   </security-constraint>

   <security-constraint>
     <web-resource-collection>
       <web-resource-name>Status interface</web-resource-name>
       <url-pattern>/status/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-gui</role-name>
        <role-name>manager-script</role-name>
        <role-name>manager-jmx</role-name>
        <role-name>manager-status</role-name>
     </auth-constraint>
    <user-data-constraint>
      <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
   </security-constraint>
   ~~~~

2. Verify TLS/SSL has been enabled for the Tomcat Manager application.

   Restart Tomcat and verify TLS/SSL has been enabled for the Tomcat Manager application: [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}
   
   {% include image.html file="tds/tutorial/production_servers/managerssl.png" alt="manager ssl" caption="" %}

   {%include important.html content="
      You will have to perform this configuration change to the Manager application deployment descriptor every time you upgrade Tomcat.  :-|
   " %}

#### Troubleshooting

* Check the XML syntax in `$TOMCAT_HOME/webapps/manager/WEB-INF/web.xml` to make sure it is well-formed and without error.
* Did you specify a `<transport-guarantee>` of `CONFIDENTIAL`?
* Did you restart Tomcat after you made your changes to `web.xml`?

### Resources
* [Manager App HOW-TO](https://tomcat.apache.org/tomcat-8.5-doc/manager-howto.html){:target="_blank"}
  The Apache Tomcat document referencing how to use and configure the Manager application.
* [Tomcat Migration Guide](https://tomcat.apache.org/migration.html){:target="_blank"}
  A document detailing the various changes between Tomcat versions contains a section dedicated to the Manager application.