---
title: Enable TLS/SSL Encryption
last_updated: 2018-11-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: enable_tls_encryption.html
---

This section demonstrates how to enable tls/SSL Encryption for the TDS and Tomcat Servlet Container.

{%include note.html content="
This section assumes you have successfully performed the tasks as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Getting Started With The TDS</a> section of this tutorial.
" %}

## Rationale
* Communication between two servers can be intercepted (i.e., an http transaction between client and server).
* An attacker can eavesdrop on the conversation and control the relay of messages between the victims, making them believe that they are talking directly to each other over a private connection.
* The use of digital certificates adds a layer of security by allowing the receiver of the message to verify the sender is who he or she claims to be.
* Any intercepted information that is encrypted also adds a layer of security (the attacker must take the extra step of un-encrypting the data to view the message).
* Transport Layer Security (TLS), and formerly Secure Sockets Layer (SSL), is a cryptographic protocol that provides security and data integrity for communications over TCP/IP networks.
* TLS/SSL allows applications to communicate across a network in a way designed to prevent eavesdropping, tampering, and message forgery.
* TLS/SSL uses a cryptographic system that uses two keys to encrypt data: a public key known to everyone and a private or secret key known only to the recipient of the message.
* By convention, URLs that require an TLS/SSL connection start with `https` instead of `http`.

{%include note.html content="
For more information on how TLS/SSL works, Wikipedia details the <a href=\"https://en.wikipedia.org/wiki/Transport_Layer_Security\" target=\"_blank\">steps involved</a> during an TLS/SSL transaction.
" %}

## TLS/SSL Certificates
* A public key certificate is an electronic document which incorporates a digital signature to bind together a public key with identity information of the certificate user.
* The certificate can be used to verify that a public key belongs to an individual.
* The digital signature can be signed by a Certificate Authority (CA) or the certificate user (a self-signed certificate).

#### Do not use self-signed certificates
{%include important.html content="
Unidata _highly_ recommends the use of a certificate signed by a Certificate Authority (CA).
" %}

* Browser warnings for self-signed certificates can be very confusing to users and make them question the legitimacy of your web site.
* It's about trust: CA-signed certificates verify your identify to your users.
  If the traffic between your server and the client is intercepted, an attacker can inject their own self-signed cert in the place of yours and the visitor will likely not notice.
* Self-signed certificates cannot (by nature) be revoked, which may allow an attacker who has already gained access to monitor and inject data into a connection to spoof an identity if a private key has been compromised. 
  CAs on the other hand have the ability to revoke a compromised certificate, which prevents its further use.

#### Certificate keystore file
A keystore file stores the details of the TLS/SSL certificate necessary to make the protocol secured.
The Tomcat documentation includes a section on [importing your certificate](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html#Prepare_the_Certificate_Keystore){:target="_blank"} into a keystore file.
Tomcat uses the keystore file for TLS/SSL transactions. 

## Enabling TLS/SSL In Tomcat

The following example demonstrates enabling TLS/SSL in the Tomcat Servlet Container on a linux system as the `root` user. 

{%include note.html content="
This section assumes you have already imported your CA-signed certificate into the <a href=\"https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html#Prepare_the_Certificate_Keystore\" target=\"_blank\">keystore</a> file.
" %}

1. Modify the Tomcat configuration to enable TLS/SSL:

   Open `$TOMCAT_HOME/conf/server.xml` with your favorite text editor:

   ~~~~bash
   # vi server.xml
   ~~~~

   Locate the `Java HTTP/1.1 Connector` listening on port `8080` and verify it is redirecting TLS/SSL traffic to port `8443`:
   ~~~~xml
   <Connector port="8080" 
              protocol="HTTP/1.1"
              connectionTimeout="20000"
              redirectPort="8443" />
   ~~~~

   Find and uncomment the `NIO implementation SSL HTTP/1.1 Connector` listening on port `8443` to activate this connector:

   ~~~~xml
   <Connector port="8443" 
              protocol="org.apache.coyote.http11.Http11NioProtocol" 
              maxThreads="150" 
              SSLEnabled="true">
       <SSLHostConfig>
           <Certificate certificateKeystoreFile="conf/localhost-rsa.jks" 
                        type="RSA" />
       </SSLHostConfig>
   </Connector>
   ~~~~
   
   {%include note.html content="
     Tomcat also offers a `SSL/TLS HTTP/1.1 Connector` which utilizes `APR/native implementation`. Consult the <a href=\"http://tomcat.apache.org/tomcat-8.5-doc/config/http.html\" target=\"_blank\">documentation</a> to see if you should use this connector in lieu of the `NIO implementation SSL HTTP/1.1` connector.
   " %}
   
   Specify the keystore file in the `certificateKeystoreFile` attribute of the `Certificate` element to tell Tomcat where to find your keystore (the path will be relative to `$TOMCAT_HOME` directory).  
   
   In this example, the keystore file is `$TOMCAT_HOME/conf/tds-keystore`:

   ~~~~xml
   <Connector port="8443" 
              protocol="org.apache.coyote.http11.Http11NioProtocol" 
              maxThreads="150" 
              SSLEnabled="true">
       <SSLHostConfig>
           <Certificate certificateKeystoreFile="conf/tds_keystore" 
                        type="RSA"/>
       </SSLHostConfig>
   </Connector>
   ~~~~

   If you opted to not use the default keystore password (`changeit`), you'll need to specify the new password so Tomcat can open the file.  Add the `certificateKeystorePassword` attribute of the `Certificate` element for your keystore password.
   
   ~~~~xml
   <Connector port="8443" 
              protocol="org.apache.coyote.http11.Http11NioProtocol" 
              maxThreads="150" 
              SSLEnabled="true">
       <SSLHostConfig>
           <Certificate certificateKeystoreFile="conf/tds_keystore" 
                        certificateKeystorePassword="foobar"
                        type="RSA"/>
       </SSLHostConfig>
   </Connector>
   ~~~~
   
      
   {%include important.html content="
   Keep in mind: Changes to `$TOMCAT_HOME/conf/server.xml` do not take effect until Tomcat is restarted.
   " %}

2. Verify TLS/SSL has been enabled.

   Restart Tomcat:

   ~~~~bash
   # /usr/local/tomcat/bin/shutdown.sh
   # /usr/local/tomcat/bin/startup.sh
   ~~~~

   Verify Tomcat is listening on port 8443 by running the `netstat` command:

   ~~~~bash
   # netstat -an | grep tcp | grep 8443
   ~~~~

   `netstat` (short for network statistics) is a command-line tool that displays:

   * network connections (both incoming and outgoing)
   * routing tables
   * and a number of network interface statistics

   Look for something like the following in the output (output may vary between operating systems):

   ~~~~bash
   tcp        0      0 0.0.0.0:8443                0.0.0.0:*                   LISTEN 
   ~~~~

   {%include note.html content="
     Run `man netstat` in your terminal window to learn more about this command.
   " %}

#### Troubleshooting
* Check the XML syntax in `$TOMCAT_HOME/conf/server.xml` to make sure it is well-formed and without error.
* Did you restart Tomcat after you made your changes to `server.xml`?
* Did you specify the full path to the keystore file in `server.xml`?

{%include ahead.html content="
Other than the compelling security reasons, you will want to enable TLS/SSL to take advantage of a couple of monitoring and debugging tools: the <a href=\"http://localhost:8080/thredds/admin/debug\" target=\"_blank\">TDS Remote Management Tool</a>, and the <a href=\"/using_the_tdsmonitor_tool.html\" target=\"_blank\">TdsMonitor</a> Tool -- both of which (out-of-the-box) require TLS/SSL to access.
" %}

## Configuring Web Applications for TLS/SSL

* The web application deployment descriptor, a.k.a. `web.xml`, specifies if all or parts of it need to be accessed via TLS/SSL.
* Deployment descriptors are found in the `WEB-INF` directory of the web application: `$TOMCAT_HOME/webapps/application_name/WEB-INF/web.xml`.
* By convention, Tomcat and other servlet containers will read the web application deployment descriptors for initialization parameters and container-managed security constraints upon application deployment.
* The TDS has been pre-configured to require that TLS/SSL encryption is enabled in order to access the both the [TDS Remote Management Tool](http://localhost:8080/thredds/admin/debug){:target="_blank"}, and the [TdsMonitor Tool](using_the_tdsmonitor_tool.html){:target="_blank"}.

This is the entry in the TDS `web.xml` for the TDS Remote Management Tool:

~~~~xml
<!-- tdsConfig with HTTPS needed for /admin access  -->
<security-constraint>
  <web-resource-collection>
    <web-resource-name>sensitive read access</web-resource-name>
    <url-pattern>/admin/*</url-pattern>
  </web-resource-collection>
  <auth-constraint>
    <role-name>tdsConfig</role-name>
  </auth-constraint>
  <user-data-constraint>
    <transport-guarantee>CONFIDENTIAL</transport-guarantee>
  </user-data-constraint>
</security-constraint>
~~~~

* The `<user-data-constraint>` establishes a requirement that the constrained requests be received over a protected transport layer connection.
   This guarantees how the data will be transported between client and server.
* `<transport-guarantee>` choices for type of transport guarantee include `NONE`, `INTEGRAL`, and `CONFIDENTIAL`:
   1. Specify `CONFIDENTIAL` when the application requires that data be transmitted so as to prevent other entities from observing the contents of the transmission. (E.g., via TLS/SSL.)
   2. Specify `INTEGRAL` when the application requires that the data be sent between client and server in such a way that it cannot be changed in transit.
   3. Specify `NONE` to indicate that the container must accept the constrained requests on any connection, including an unprotected one.

{%include note.html content=" 
  For more information on how to configure security requirements for a web application in a deployment descriptor, see: <a href=\"https://javaee.github.io/tutorial/security-webtier.html#BNCAS\" target=\"_blank\">Defining Security Requirements for Web Applications</a>.
" %}

## Accessing TDS Monitoring and Debugging Tools
Other than the compelling security reasons, you will want to enable TLS/SSL to take advantage of the [TDS Remote Management Tool](http://localhost:8080/thredds/admin/debug){:target="_blank"} and the [TdsMonitor Tool](using_the_tdsmonitor_tool.html){:target="_blank"} monitoring and debugging tools.  

1. Enable TLS/SSL in Tomcat
   If Tomcat has not already been configured to run via TLS/SSL, follow the tutorial in the previous section to Enable TLS/SSL in Tomcat.
2. Modify `$TOMCAT_HOME/conf/tomcat-users.xml` to add the new tdsConfig and tdsMonitor roles.
   Add these roles to your list of roles:
   ~~~~xml
   <tomcat-users xmlns="http://tomcat.apache.org/xml"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
                 version="1.0">
   <!--
     NOTE:  By default, no user is included in the "manager-gui" role required
     to operate the "/manager/html" web application.  If you wish to use this app,
     you must define such a user - the username and password are arbitrary. It is
     strongly recommended that you do NOT use one of the users in the commented out
     section below since they are intended for use with the examples web
     application.
   -->
   <!--
     NOTE:  The sample user and role entries below are intended for use with the
     examples web application. They are wrapped in a comment and thus are ignored
     when reading this file. If you wish to configure these users for use with the
     examples web application, do not forget to remove the <!.. ..> that surrounds
     them. You will also need to set the passwords to something appropriate.
   -->
     <role rolename="manager-gui"/>
     <role rolename="tdsConfig"/>
     <role rolename="tdsMonitor"/>
     <user username="admin" 
           password="bb7a2b6cf8da7122125c663fc1585808170b2027677195e0ad121f87b27320ae$1$55003acb56e907b19d29d3b4211dc98c837354690bc90579742d6747efeec4ea" 
           roles="manager-gui, tdsConfig, tdsMonitor"/>
   </tomcat-users>
   ~~~~
      
   {%include important.html content="
   Keep in mind: Changes to `$TOMCAT_HOME/conf/tomcat-users.xml` do not take effect until Tomcat is restarted.
   " %}
   
3. Restart Tomcat and access the [TDS Remote Management Tool](http://localhost:8080/thredds/admin/debug){:target="_blank"} in your browser (authenticate with the login/password specified in `$TOMCAT_HOME/conf/tomcat-users.xml`).

   {% include image.html file="tds/tutorial/production_servers/remotemanagementtool.png" alt="TDS Remote Management Tool" caption="" %}


### Resources
* [Qualys SSL Server Test](https://www.ssllabs.com/ssltest/){:target="_blank"}
  is a free online service that analyzes the configuration of any public TLS/SSL web server. 
  Note: be sure to check the Do not show the results on the boards box if you do not want your results to be public.
* [TLS/SSL Configuration HOW-TO](https://tomcat.apache.org/tomcat-8.5-doc/ssl-howto.html){:target="_blank"}
  The Apache Tomcat document detailing how to enable TLS/SSL.
* [Tomcat Migration Guide](https://tomcat.apache.org/migration.html){:target="_blank"}
  A document detailing the various changes between Tomcat versions.
* [When are self-signed certificates acceptable?](https://www.sslshopper.com/article-when-are-self-signed-certificates-acceptable.html){:target="_blank"}
  A compelling argument as to why self-signed certificates should not be used in a production environment
