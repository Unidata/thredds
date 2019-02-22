---
title: Remote Management Reference
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: remote_management_ref.html
---

## Introduction

It's very convenient to be able to remotely manage and administer the Tomcat web server, and to remotely configure and debug the THREDDS Data Server, for example from a web browser running on your desktop.
However, remote access to the server introduces potential security problems, so by default these capabilities are not turned on.
You can run Tomcat and TDS quite successfully by editing the configuration files on the server, and restarting when needed.

Managing a server is difficult and we recommend that you enable remote management.
By following the procedures here, you can do so without opening any big security holes.
However, you must decide this yourself, based on your organization's security policies, and a risk assessment for your server.
In what follows we try to explain what risks the various options have, as well as we understand them.
A good compromise may be to do all the work to enable remote management, then turn it on only while actively configuring the server, and turn it off when in production mode.

In any case, we strongly recommend that you also read and follow the [Tomcat/TDS Security](production_server_overview.html) guidelines.

Follow the [checklist](installation_checklist.html) for more concise and up-to-date configuration instruction.

## Configuring Tomcat Users

Special permissions (like remote management) are done in Tomcat by creating users with special roles.
As long as you also follow the [Tomcat/TDS Security](production_server_overview.html) guidelines, using the simplest Tomcat mechanism to do this should be safe.

Edit `${tomcat_home}/conf/tomcat-users.xml`, adding roles `tdsConfig`, `manager`, `admin`, and users who have those roles, e.g.:

~~~xml
<?xml version='1.0' encoding='utf-8'?>
<tomcat-users>
  <role rolename="manager"/>
  <role rolename="admin"/>
  <role rolename="tdsConfig"/>

  <user username="admin" password="adminpassword" roles="admin, manager-gui, manager"/>
  <user username="yername" password="yerpassword" roles="tdsConfig"/>
</tomcat-users>
~~~

The `manager`, `manager-gui`, and `admin` roles are used within Tomcat itself to allow the use of the manager and administrator web interface.
The `tdsConfig` role is used to configure the TDS.
These roles must be specified exactly as shown.
Note that all 3 of these roles are independent - you can add any, all or none of them.
The easiest way to enable or disable remote administration is to change this file and restart Tomcat.

The list of users, their names and passwords, are whatever you want them to be.
Note that after you get this set up, you can manage users remotely through the administrator interface.
Note also that before you go into production mode, you should change to using digest passwords by following the instructions [here](production_server_overview.html).

Note that any changes won't take effect until you restart Tomcat.

**Higher Security**:
You can also use an LDAP server or a Database to store users and roles, which may give you higher levels of security.
Use of this feature is beyond the scope of this documentation, however.

## Enable Secure Sockets Layer (SSL)

We ensure that no one can intercept and read sensitive information to and from the server (through doing what\'s called network sniffing) by encrypting the information using SSL.
To enable this, do the following:

1. Choose a keystore filename `${keystore_filename}`. 
   If this option is not specified, it defaults to `${user_home}/.keystore`.
2. Create a certificate by executing the following command:
   
   ~~~bash
   ${java_home}/bin/keytool -genkey -alias tomcat -keyalg RSA -validity 365 -keystore ${keystore_filename}
   ~~~

   You will then get a set of prompts:

   ~~~bash
   Enter keystore password: mypassword
   What is your first and last name? [Unknown]: www.mydomain.edu
   What is the name of your organizational unit? [Unknown]: 
   What is the name of your organization? [Unknown]: 
   What is the name of your City or Locality? [Unknown]: 
   What is the name of your State or Province? [Unknown]: 
   What is the two-letter country code for this unit? [Unknown]: 
   Is CN=*.ucar.edu, OU=UCAR Web Engineering Group, O=University Corporation for Atmospheric Research, L=Boulder, ST=Colorado, C=US correct?
   [no]: yes
   Enter key password for <tomcat>
   (RETURN if same as keystore password):
   ~~~

   It\'s not obvious, but the \"first and last name\" must be the name of your Tomcat server host machine (`www.mydomain.edu` in this example).
   Be sure to specify the same password for both the keystore and the `<tomcat>` key password (the default is `changeit`).
   Fill out the other values as appropriate.
   This creates a self-signed certificate and puts it into the `${keystore_filename}` keystore. 
   Note the validity option on the command line, which is the number of days the certificate is valid.

3. In `${tomcat_home}/conf/server.xml`, find the following section that configures the SSL port (not the regular 8080 port), uncomment it, and modify it so it looks like:
   ~~~xml
   <!-- Define a SSL Coyote HTTP/1.1 Connector on port 8443 -->
   <Connector port="8443"  protocol="org.apache.coyote.http11.http11NioProtocol" 
              SSLEnabled="true"    
              maxThreads="150" minSpareThreads="25"
              enableLookups="false" disableUploadTimeout="true"
              acceptCount="100" scheme="https" secure="true"
              clientAuth="false" sslProtocol="TLS" keystoreFile="${keystore_filename}"
              keystorePass="mypassword"/>
   ~~~

   This enables Tomcat to use port 8443 for the HTTPS protocol, which uses SSL.
   All sensitive accesses will get redirected to that port.

   See [keytool](https://docs.oracle.com/javase/1.5.0/docs/tooldocs/index.html#security){:target="_blank"} documentation for full description of managing keystores.
   The [Tomcat doc](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html){:target="_blank"} is quite good, also.

### Installing a Certificate from a Certificate Authority

The use of SSL in this way prevents a network sniffer from getting your password.
A more sophisticated (and more difficult to perform) attack is the so called man-in-the-middle attack where someone pretends to be your server, and induces your client to send the password to it.

In the above, we used self-signed certificates, and your browser will give you a warning each time you access a web page that uses self-signed certificates.
It will allow you to choose to continue however, and so a clever enough attacker might induce you to accept their self-signed certificate.
To prevent this, you can obtain a certificate signed by a Certificate Authority (CA).
You can then install it using [these instructions](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html#Installing_a_Certificate_from_a_Certificate_Authority){:target="_blank"}.
The browser will see that its a valid certificate, and so you will never accept self-signed certificates and you will preclude man-in-the-middle attacks.

Obtaining and installing a CA signed certificate is a fair amount of work, but not really all that difficult.
We recommend it if you plan on leaving remote management enabled in production mode.

### Using the JRE keystore

Third party authentication software such as CAS may require that a remote host's certificate be placed into the JRE keystore.

1. Obtain the remote host\'s certificate and place into a temporary file, say `server.crt`.
   For example, if running a CAS server in Tomcat, you want to use the tomcat certificate that we generated above. To extract it, use:

   ~~~bash
   keytool -export -alias tomcat -keypass changeit -keystore ${keystore_filename} -file server.crt
   ~~~

2. Import the certificate into the JRE's keystore. Be sure to use the same JRE that Tomcat will run under:

   ~~~bash
   keytool -import -alias tomcat -keypass changeit -file server.crt -keystore ${jre_home}/lib/security/cacerts
   ~~~

   If there are spaces, you need to quote the filename, eg:

   ~~~bash
   keystore "C:/Program Files (x86)/Java/jdk1.7.0_04/jre/lib/security/cacerts"
   ~~~

If the certificate is not correctly installed (e.g. you put it in the wrong JRE), you will get an Exception like:

~~~bash
sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
~~~

## Check that the correct ports are enabled

After restarting Tomcat, you can use `netstat -an` command to check that the ports are correctly configured.
You should see lines like:

~~~bash
TCP    127.0.0.1:8080         0.0.0.0:0              LISTENING
TCP    127.0.0.1:8443         0.0.0.0:0              LISTENING 
~~~

You also need to make sure that your firewall is allowing those ports through.

## TDS Remote Debugging

Once SSL is enabled, you can remotely debug and configure the TDS.
You need to login with a user who has the `tdsConfig` role.

Debugging information is available at [http://localhost:8080/thredds/admin/debug](http://localhost:8080/thredds/admin/debug){:target="_blank"}.

{% include image.html file="tds/reference/remote_management/TdsDebug.png" alt="TDS Remote Management" caption="" %}

Some capabilities of particular interest are:

* `Show Tomcat Logs`: allows you to look at the Tomcat logs in `${tomcat_home}/logs`
* `Show TDS Logs`: allows you to look at the TDS logs in `${tomcat_home}/content/thredds/logs`
* `Show static catalogs`: list all the static (non-dynamic) catalogs read in at startup
* `Show data roots`: list all the dataRoots with links to the directories they are mapped to
* `Show File Object Caches`: Show all files currently in the object caches
* `Clear File Object Caches`: Remove all unlocked files in the object caches

## TroubleShooting

* **Connection refused when trying to access a restricted page.** 
  The SSL socket is not enabled in the server.xml file, or you didn't install a certificate in the keystore.
* `sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target`: 
  A 3rd party security system like CAS is trying to establish an SSL connection.
  Install the remote host's certificate into the JRE certificate store.
* **What\'s in the keystore?**
  * Look at the Tomcat Server Status [page](https://localhost:8443/thredds/admin/debug?General/showServerInfo){:target="_blank"} and check the JVM Version.
  * From command line, make sure that this matches the results of `java -version`. 
    If not, then your Tomcat server is starting up with a different JVM.
  * From command line, run `keytool -list` to see what\'s in the default keystore.
    Standard is to have password _changeit_.
    You need an entry named _tomcat_.

## Resources

* [Tomcat SSL Configuration](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html){:target="_blank"}
* Java Secure Sockets Extension (JSSE) [Reference Guide](https://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html){:target="_blank"}
* JDK [keytool](https://docs.oracle.com/javase/1.5.0/docs/tooldocs/index.html#security){:target="_blank"} application
* JA-SIG Central Authentication Service. ([CAS]](https://www.apereo.org/projects/cas){:target="_blank"})