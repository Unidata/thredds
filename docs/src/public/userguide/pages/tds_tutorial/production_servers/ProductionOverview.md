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
 * how to do a basic installation of [Tomcat, Java](/install_java_tomcat.html) , and the [TDS](/deploying_the_tds.html)
 * be familiar with the [Tomcat directory structure](/tomcat_dir_structure_qt.html)
 * how to [configure Tomcat ](/tomcat_configuration_files.html)
 * how to use the [Tomcat manager application](/tomcat_manager_app.html)

## Why Is Security Important?

Be afraid

* Misconfiguration of Tomcat can introduce vulnerabilities in security.
* The recommendations in this section should be considered \"layers\" of security: not completely effective by themselves, but more potent when combined.
* The topics covered in this section should not be considered a complete laundry list of security fixes! Please use it as a starting point when securing your server.
  The ordering of the topics in this section are not a representation of the section importance.

## Keeping Software Versions Up-To-Date

### Rationale

Running the most current versions of software keeps your environment protected against _known_ [security vulnerabilities](https://www.securityfocus.com/vulnerabilities){:target="_blank"}.
This includes the JDK, Tomcat, the TDS and any other third-party libraries or software you run.

Stay Informed!
Subscribe to announcement lists for Tomcat, the TDS and any other software you deploy, to stay abreast of new versions released due to security issues.

As soon as a security issue is disclosed, potential attackers will begin trying to exploit that vulnerability.
It is important that you upgrade your software before an attacker uses the vulnerability against you.

### Resources

* [Tomcat security reports](https://tomcat.apache.org/security.html){:target="_blank"}
  A complete list of known and documented security vulnerabilities associated with each Tomcat release.

* [Tomcat mailing lists](https://tomcat.apache.org/lists.html){:target="_blank"}
  Various tomcat-related mailing lists, including tomcat-announce which is a low volume list for release announcements and security vulnerabilities.

* [Java SE Security](https://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html){:target="_blank"}
  Oracle’s Java security page which includes a chronology of Java security issues and user forums.

* [thredds mailing list](www.unidata.ucar.edu/mailing_lists/archives/thredds/){:target="_blank"}
  The THREDDS mailing list where announcements of new releases will be made.

* [Buqtraq vunerability database](https://www.securityfocus.com/vulnerabilities){:target="_blank"}
  SecurityFocus’ database of all known vulnerabilities for all different types of software from different vendors.

## Tomcat Process User/Group and ${tomcat_home} Permissions

### Rationale

#### Background info

The JVM doesn’t fork at all, nor does it support `setuid()` calls.
The JVM, and therefore Tomcat, is one process.
The JVM is a virtual machine with many threads under the same process.

* Because of OS constraints, all threads in the same JVM process must run under the same user id.
  No thread may run as the root user unless they are all are run as the root user.
  Hence, any programs run in Tomcat (TDS, manager application, other JSPs and servlets) will run as the root user.
* If you _choose_ to run the Tomcat process as the root user, and an attacker manages to exploit a weakness in Tomcat or something running in `webapps/` to run arbitrary commands, those commands will be run as the **superuser**!
* We strongly discourage running Tomcat as the root user and recommend creating an unprivileged, dedicated user and group for running the Tomcat process.

#### Create a dedicated user/group for running Tomcat

In this example, both the user and group names will be names tomcat, and the user\'s home directory, a.k.a. `${tomcat_home}`, is `/opt/tomcat` (notice the symlink below).
The `groupadd` and `useradd` commands were run as the root:

~~~bash
# groupadd tomcat
# useradd -g tomcat -d /opt/tomcat tomcat
# passwd tomcat
~~~


#### Restrict the permissions in `${tomcat_home}`

We also recommend restricting the permissions of the Tomcat `user/group` within `${tomcat_home}`.
1. Change the user/group ownership `${tomcat_home}` to the `tomcat` user and `tomcat` group:
   ~~~bash
   # cd /opt
   # chown -R tomcat:tomcat apache-tomcat-8.0.24
   # ls -ld  *tomcat*
   drwxr-xr-x  9 tomcat tomcat  4096 Jul 15 16:03 apache-tomcat-8.0.24
   ~~~
2. Change the user/ownership of the `${tomcat_home}/conf` directory to be owned by the `root` user, have a group of `tomcat` and have a permission of user/group read only:
   ~~~bash
   # cd /opt/tomcat
   # ls -l
   total 92
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 15 16:05 bin
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 18 12:18 conf
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 15 16:03 lib
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 logs
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 15 16:03 temp
   drwxr-xr-x 7 tomcat  tomcat  4096 Jul 15 16:04 webapps
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 work

   # chown -R root:tomcat conf
   # chmod -R 440 conf/*
   # ls -l conf
   total 92
   -r--r----- 1 root    tomcat  9978 Feb  2 12:06 catalina.policy
   -r--r----- 1 root    tomcat  3713 Feb  2 12:06 catalina.properties
   -r--r----- 1 root    tomcat  1395 Feb  2 12:06 context.xml
   -r--r----- 1 root    tomcat  1353 Jul 18 12:14 keystore
   -r--r----- 1 root    tomcat  3257 Feb  2 12:06 logging.properties
   -r--r----- 1 root    tomcat  6814 Jul 18 12:18 server.xml
   -r--r----- 1 root    tomcat   210 Jul 18 12:10 tomcat-users.xml
   -r--r----- 1 root    tomcat 51835 Feb  2 12:06 web.xml
   ~~~
3. Change the user/ownership of the `${tomcat_home}/bin` and `${tomcat_home}/lib` directories to be owned by the `root` user and have a group of `tomcat`:
   ~~~bash
   # chown -R root:tomcat lib
   # chown -R root:tomcat bin
   # ls -l
   total 92
   drwxr-xr-x 2 root    tomcat  4096 Jul 15 16:05 bin
   drwxr-xr-x 2 root    tomcat  4096 Jul 18 12:18 conf
   drwxr-xr-x 2 root    tomcat  4096 Jul 15 16:03 lib
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 logs
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 15 16:03 temp
   drwxr-xr-x 7 tomcat  tomcat  4096 Jul 15 16:04 webapps
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 work
   ~~~
4. Change the user/group permissions of the `${tomcat_home}/conf` directory and its subdirectories to give the `root` user and `tomcat group` read/write permissions.
   Depending on the web applications you are running and/or your virtual host configurations, Tomcat will create a `${tomcat_home}/conf/Catalina` directory with corresponding subdirectories and files for [context](https://tomcat.apache.org/tomcat-8.0-doc/virtual-hosting-howto.html#Configuring_Your_Contexts){:target="_blank"} information.
   Allowing the tomcat group to write to `${tomcat_home}/conf` allow this and prevent errors from appearing in your Tomcat logs.
   ~~~bash
   # pwd
   # /opt/apache-tomcat-8.0.24
   # find conf -type d -exec chmod 775 {} \; -print
   # ls -l
   total 220
   total 92
   drwxr-xr-x 2 root    tomcat  4096 Jul 15 16:05 bin
   drwxrwxr-x 2 root    tomcat  4096 Jul 18 12:18 conf
   drwxr-xr-x 2 root    tomcat  4096 Jul 15 16:03 lib
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 logs
   drwxr-xr-x 2 tomcat  tomcat  4096 Jul 15 16:03 temp
   drwxr-xr-x 7 tomcat  tomcat  4096 Jul 15 16:04 webapps
   drwxr-xr-x 2 tomcat  tomcat  4096 Feb  2 12:04 work
   ~~~
5. Change the user/group permissions of the files in `${tomcat_home}/conf` directory to give the root user and tomcat group has read-only permissions:
   ~~~bash
   # cd conf
   # find . -type f -exec chmod 440 {} \; -print
   # ls -l
   total 220
   drwxr-xr-x 3 root tomcat   4096 2013-12-26 15:24 Catalina
   -r--r----- 1 root tomcat  12374 2015-07-01 14:23 catalina.policy
   -r--r----- 1 root tomcat   7086 2015-07-01 14:23 catalina.properties
   -r--r----- 1 root tomcat   1577 2015-07-01 14:23 context.xml
   -r--r----- 1 root tomcat   2899 2015-04-17 15:42 logging.properties
   -r--r----- 1 root tomcat   2330 2015-04-22 10:33 server.xml
   -r--r----- 1 root tomcat   1460 2015-04-24 14:33 tomcat-users.xml
   -r--r----- 1 root tomcat   1846 2015-07-01 14:23 tomcat-users.xsd
   -r--r----- 1 root tomcat 166582 2015-07-01 14:23 web.xml
   ~~~

### Resources

* [Tomcat as root and security issues](https://marc.info/?t=104516038700003&r=1&w=2){:target="_blank"}
  A lengthy thread in the tomcat-users mailing list archives dedicated to the perils of running Tomcat as the root user.

## Removing Unused Web Applications

### Rationale
It is generally good practice to remove any un-used web applications out of `${tomcat_home}/webapps`.

Tomcat \"ships\" with several default web applications you may want to consider removing if they are not being utilized:

* The `ROOT` application is Tomcat\'s `DocumentRoot` and contains the server\'s main web page.
   Give thought to the content that is placed in `ROOT/`, as it will be readily available. 
   Note: if you want to utilize a `robots.txt` file to restrict crawler activity, `ROOT/` is the place it will go.
* The `manager` application is used for remote management of web applications. 
  To use this application, you must add a user with role of `manager-gui` in `tomcat-users.xml`.
  Obviously, if you are not planning to use the manager application, it should be removed.
* The `host-manager` application is used for management of virtual hosts. 
  To use this application, you must add a user with role of `admin-gui` in `tomcat-users.xml`. 
  If you are not planning to do a lot of virtual hosting in Tomcat this application should be removed.
* The `examples` application should probably be removed from a production server to minimize security exposure.
* The docs are a copy of the [Tomcat documentation found online](https://tomcat.apache.org/tomcat-8.0-doc/){:target="_blank"}. 
  Unless you have need for a local copy, removing docs would help to tidy-up `${tomcat_home}/webapps`.

## Using Digested Passwords

### Rationale

* Passwords stored in clear text are a vulnerability if the host is compromised.
* Better to have the passwords encrypted using a cryptographic hash functions (SHA, MD2, or MD5) and then stored in `tomcat-users.xml` file in the Tomcat `conf/` directory.
* Tomcat can be configured to support digested passwords (this is not the default setting).

How it works: When a client makes a request Tomcat will tell the client that a digested password is required.
Based on this dialog, the client will automatically digest the password entered by the user.

{%include ahead.html content="
Tomcat Realms:
A <a href=\"https://tomcat.apache.org/tomcat-8.0-doc/config/realm.html\" target=\"_blank\">realm</a> element represents a \"database\" of usernames, passwords, and roles (similar to Unix groups) assigned to those users.
" %}

### Configure Tomcat to use digested passwords

1. First we need to enable digest passwords support in Tomcat by modifying a couple of Tomcat Realms in the `server.xml` file in the Tomcat `conf/` directory.
   A Tomcat Realm represents a \"database\" of usernames, passwords, and roles assigned to tomcat users.

   | Realm Name | Purpose |
   | UserDatabaseRealm | The UserDatabaseRealm is enabled by default and reads clear text user password information stored in tomcat-users.xml. |
   MemoryRealm | The MemoryRealm reads the user password information stored in the  tomcat-users.xml in a specified encrypted format.| 

   Open the server.xml with your favorite editor:
   
   ~~~bash
   $ vi server.xml
   ~~~

   Locate the `UserDatabaseRealm` (in the `LockOutRealm`, right above the `Host` element) and comment it out:

   ~~~xml
   <!-- Use the LockOutRealm to prevent attempts to guess user passwords
     via a brute-force attack -->
   <Realm className="org.apache.catalina.realm.LockOutRealm">
   <!-- This Realm uses the UserDatabase configured in the global JNDI
        resources under the key "UserDatabase".  Any edits
        that are performed against this UserDatabase are immediately
        available for use by the Realm.  -->
   <!--
   <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
     resourceName="UserDatabase"/>
   -->
   </Realm>

   <Host name="localhost"  appBase="webapps"
      unpackWARs="true" autoDeploy="true">
   ~~~

   Now add the following MemoryRealm information below the commented out UserDatabaseRealm:

   ~~~xml
   <!-- Use the LockOutRealm to prevent attempts to guess user passwords
     via a brute-force attack -->
   <Realm className="org.apache.catalina.realm.LockOutRealm">
     <!-- This Realm uses the UserDatabase configured in the global JNDI
          resources under the key "UserDatabase".  Any edits
          that are performed against this UserDatabase are immediately
          available for use by the Realm.  -->
     <!--
     <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
       resourceName="UserDatabase"/>
     -->
     <Realm className="org.apache.catalina.realm.MemoryRealm"
       digest="SHA" />
   </Realm>

   <Host name="localhost"  appBase="webapps"
         unpackWARs="true" autoDeploy="true">
   ~~~
2. Create a SHA encrypted version of your password.

   Tomcat provides a script (`${tomcat_home}/bin/digest.sh`) that will encrypt a password string according to the algorithm specified.
   Use this script as follows with the password you made for yourself previously:

   ~~~bash
   $ /home/tds/apache-tomcat-8.0.24/bin/digest.sh -a SHA secret
   secret:60a5cd07880b5595c11f9ee37ed122cd9bc213a70b9a19a03ee44e4f7806c20c$1$65ffdbd93d3b3d16339d653f5078c8a7ecb7dcd9
   ~~~

   To use a different algorithm, a salt, or to limit the length of the resulting password hash, consult the [syntax options](https://tomcat.apache.org/tomcat-8.0-doc/realm-howto.html#Digested_Passwords){:target="_blank"} of `tomcat_home/bin/digest.[bat|sh]`.
3. Update `tomcat-users.xml`.
   Replace your clear-text password in tomcat-users.xml with the encrypted version:
   ~~~xml
   <tomcat-users>
     <role rolename="manager-gui"/>
     <user username="admin" 
           password="60a5cd07880b5595c11f9ee37ed122cd9bc213a70b9a19a03ee44e4f7806c20c$1$65ffdbd93d3b3d16339d653f5078c8a7ecb7dcd9"
          roles="manager-gui"/>
   </tomcat-users>
   ~~~
4. Verify digest passwords have been successfully enabled in Tomcat.
   Restart Tomcat and verify digest passwords have been successfully enabled by logging into the Tomcat manager application using your password in clear text: [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}

Note: Since we are using BASIC authentication, you will need to clear any authenticated sessions in your browser to test whether digested passwords have been enabled.

#### Troubleshooting

* Check the XML syntax in `tomcat-users.xml` and `server.xml` to make sure it is well-formed and without error.
* Did you restart Tomcat after you made your changes to `tomcat-users.xml` and `server.xml`?
* Any errors will be reported in the catalina.out file in the Tomcat `logs/` directory.
* You do not need to type the encrypted version of your password into the browser (the browser auto-magically encrypts your password for you before it transmits it to the server).

## Enabling TSL/SSL Encryption

### Rationale
* Communication between two servers can be intercepted (i.e., an http transaction between client and server).
* An attacker can eavesdrop on the conversation and control the relay of messages between the victims, making them believe that they are talking directly to each other over a private connection.
* The use of digital certificates adds a layer of security by allowing the receiver of the message to verify the sender is who he or she claims to be.
* Any intercepted information that is encrypted also adds a layer of security (the attacker must take the extra step of unencrypting the data to view the message).
* Transport Layer Security (TLS), and formerly Secure Sockets Layer (SSL), is a cryptographic protocol that provides security and data integrity for communications over TCP/IP networks.
* TSL/SSL allows applications to communicate across a network in a way designed to prevent eavesdropping, tampering, and message forgery.
* TSL/SSL uses a cryptographic system that uses two keys to encrypt data: a public key known to everyone and a private or secret key known only to the recipient of the message.
* By convention, URLs that require an TSL/SSL connection start with `https` instead of `http`.

How TSL/SSL works: For more information on how SSL works, Wikipedia details the [steps involved](https://en.wikipedia.org/wiki/Transport_Layer_Security){:target="_blank"} during an TSL/SSL transaction.

### TSL/SSL certificates
* A public key certificate is an electronic document which incorporates a digital signature to bind together a public key with identity information of the certificate user.
* The certificate can be used to verify that a public key belongs to an individual.
* The digital signature can be signed by a Certificate Authority (CA) or the certificate user (a self-signed certificate).
* Unidata _highly_ recommends the use of a certificate signed by a Certificate Authority (CA).
* Browser warnings for self-signed certificates can be very confusing to users and make them question the legitimacy of your web site.
* It's about trust: CA-signed certificates verify your identify to your users.
  If the traffic between your server and the client is intercepted, an attacker can inject their own self-signed cert in the place of yours and the visitor will likely not notice.
* Self-signed certificates cannot (by nature) be revoked, which may allow an attacker who has already gained access to monitor and inject data into a connection to spoof an identity if a private key has been compromised. 
  CAs on the other hand have the ability to revoke a compromised certificate, which prevents its further use.

### Certificate keystore file
A keystore file stores the details of the TSL/SSL certificate necessary to make the protocol secured.
The Tomcat documentation includes a section on [importing your certificate](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html#Prepare_the_Certificate_Keystore){:target="_blank"} into a keystore file.
Tomcat uses the keystore file for TSL/SSL transactions. Example:
   ~~~xml
  <Connector protocol="HTTP/1.1" port="8443" maxThreads="200" 
     scheme="https" secure="true" SSLEnabled="true"
     keystoreFile="${user.home}/.keystore" 
     keystorePass="changeit"
     clientAuth="false" sslProtocol="TLS"/>
   ~~~

#### Enabling TSL/SSL in Tomcat
1. Modify the Tomcat configuration to enable TSL/SSL:
   {%include question.html content="
   Based on what we know about Tomcat configuration, which file in ${tomcat_home}/conf should we edit to to enable TSL/SSL?
   " %}

   Open `${tomcat_home}/conf/server.xml` with your favorite editor:

   ~~~bash
   $ vi server.xml
   ~~~

   Locate the `Java HTTP/1.1 Connector` listening on port `8080` and verify it is redirecting TSL/SSL traffic to port `8443`:
   ~~~xml
   <Connector port="8080" 
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
   ~~~

   Find and uncomment the `SSL HTTP/1.1 Connector` listening on port `8443` to activate this connector:

   ~~~xml
   <Connector port="8443" 
              protocol="HTTP/1.1" 
              SSLEnabled="true"
              maxThreads="150" 
              scheme="https" 
              secure="true"
              clientAuth="false" 
              sslProtocol="TLS" />
   ~~~
   
   Add a `keystoreFile` attribute to the `SSL HTTP/1.1 Connector` to tell Tomcat where to find your keystore:

   ~~~xml
   <Connector port="8443" 
              protocol="HTTP/1.1" 
              SSLEnabled="true"
              maxThreads="150" 
              scheme="https" 
              secure="true"
              clientAuth="false" 
              sslProtocol="TLS" 
              keystoreFile="/home/tds/apache-tomcat-8.0.24/conf/keystore" />
   ~~~

   Since we opted to not use the default keystore password, we need to specify the new password so Tomcat can open the file:
   ~~~xml
   <Connector port="8443" 
              protocol="HTTP/1.1" 
              SSLEnabled="true"
              maxThreads="150" 
              scheme="https" 
              secure="true"
              clientAuth="false" 
              sslProtocol="TLS" 
              keystoreFile="/home/tds/apache-tomcat-8.0.24/conf/keystore"
              keystorePass="foobar" />
   ~~~

2. Verify TSL/SSL has been enabled.

   Restart Tomcat:

   ~~~bash
   $ ${tomcat_home}/bin/shutdown.sh
   $ ${tomcat_home}/bin/startup.sh
   ~~~

   Verify Tomcat is listening on port 8443 by running the netstat command:

   ~~~bash
   $ netstat -an | grep tcp | grep 8443
   ~~~

   `netstat` (short for network statistics) is available on Unix, Unix-like, and Windows NT-based operating systems.
   It is a command-line tool that displays:

   * network connections (both incoming and outgoing)
   * routing tables
   * and a number of network interface statistics

   Look for the following in the output:

   ~~~bash
   tcp        0      0 :::8443              :::*                  LISTEN
   ~~~

   {%include note.html content="
     Run <b>man netstat</b> in your terminal window to learn more about this command.
   " %}
   

#### Troubleshooting
* Check the XML syntax in `server.xml` to make sure it is well-formed and without error.
* When generating the self-signed certificate, the last password (the key password) and keystore password should be the same (changeit). 
  If they differ, Tomcat cannot open the keystore and you will get this error: `java.io.IOException: Cannot recover key`.
* Did you restart Tomcat after you made your changes to `server.xml`?
* Did you specify the full path to the keystore file in `server.xml`?

{%include ahead.html content="
Other than the compelling security reasons, you will want to enable TSL/SSL to take advantage of a couple of monitoring and debugging tools: the <a href=\"http://localhost:8080/thredds/admin/debug\" target=\"_blank\">TDS Remote Management Tool</a>, and the <a href=\"/using_the_tdsmonitor_tool.html\" target=\"_blank\">TdsMonitor</a> Tool -- both of which (out-of-the-box) require TSL/SSL to access.
" %}

### Configuring web applications for TSL/SSL

* The web application deployment descriptor, a.k.a. `web.xml`, specifies if all or parts of it need to be accessed via TSL/SSL.
* Deployment descriptors are found in the WEB-INF directory of the web application: `${tomcat_home}/webapps/application_name/WEB-INF/web.xml`.
* By convention, Tomcat and other servlet containers will read the web application deployment descriptors for initialization parameters and container-managed security constraints upon application deployment.
* The TDS has been pre-configured to require that TSL/SSL encryption is enabled in order to access the both the [TDS Remote Management Tool](http://localhost:8080/thredds/admin/debug){:target="_blank"}, and the [TdsMonitor Tool](/using_the_tdsmonitor_tool.html){:target="_blank"}.

`web.xml` fom the TDS Remote Management Tool:

~~~xml
<!-- This allows "remote configuration":
    /thredds/admin/debug gives access to various debug and status info.
    /thredds/admin/content/ -> "{tomcat_home}/content/thredds/"
    /thredds/admin/root/ -> "{tomcat_home}/webapps/thredds/" DISABLED
    /thredds/admin/dataDir/path -> "{dataRoot(path)}/webapps/thredds/"  DISABLED
   -->
<security-constraint>
  <web-resource-collection>
    <web-resource-name>sensitive read access</web-resource-name>
    <url-pattern>/admin/*</url-pattern>
    <http-method>GET</http-method>
  </web-resource-collection>
  <auth-constraint>
    <role-name>tdsConfig</role-name>
  </auth-constraint>
  <user-data-constraint>
    <transport-guarantee>CONFIDENTIAL</transport-guarantee>
  </user-data-constraint>
</security-constraint>
~~~

* The `<user-data-constraint>` establishes a requirement that the constrained requests be received over a protected transport layer connection.
   This guarantees how the data will be transported between client and server.
* `<transport-guarantee>` choices for type of transport guarantee include `NONE`, `INTEGRAL`, and `CONFIDENTIAL`:
   1. Specify `CONFIDENTIAL` when the application requires that data be transmitted so as to prevent other entities from observing the contents of the transmission. (E.g., via TSL/SSL.)
   2. Specify `INTEGRAL` when the application requires that the data be sent between client and server in such a way that it cannot be changed in transit.
   3. Specify `NONE` to indicate that the container must accept the constrained requests on any connection, including an unprotected one.

{%include note.html content=" 
  For more information on how to configure security requirements for a web application in a deployment descriptor, see: <a href=\"https://javaee.github.io/tutorial/security-webtier.html#BNCAS\" target=\"_blank\">Defining Security Requirements for Web Applications</a>.
" %}

#### Accessing TDS Monitoring and Debugging Tools
Other than the compelling security reasons, you will want to enable TSL/SSL to take advantage of a couple of monitoring and debugging tools: the TDS Remote Management Tool, and the TdsMonitor Tool -- both of which (out-of-the-box) require TSL/SSL to access.

1. Enable TSL/SSL in Tomcat
   If Tomcat has not already been configured to run via TSL/SSL, follow the tutorial in the previous section to Enable TSL/SSL in Tomcat.
2. Modify `${tomcat_home}/conf/tomcat-users.xml` to add the new tdsConfig and tdsMonitor roles.
   Add these roles to your list of roles:
   ~~~xml
   <tomcat-users>
       <role rolename="manager-gui"/>
       <role rolename="tdsConfig"/>
         <role rolename="tdsMonitor"/>
       <user username="admin" 
             password="e5e9fa1ba31ecd1ae84f75caaa474f3a663f05f4" 
             roles="manager-gui,tdsConfig, tdsMonitor"/>
   </tomcat-users>
   ~~~
3. Restart Tomcat

### Resources
* [Qualys SSL Server Test](https://www.ssllabs.com/ssltest/){:target="_blank"}
  is a free online service that analyzes the configuration of any public TSL/SSL web server. 
  Note: be sure to check the Do not show the results on the boards box if you do not want your results to be public.
* [TSL/SSL Configuration HOW-TO](https://tomcat.apache.org/tomcat-8.0-doc/ssl-howto.html){:target="_blank"}
  The Apache Tomcat document detailing how to enable TSL/SSL.
* [Tomcat Migration Guide](https://tomcat.apache.org/migration.html){:target="_blank"}
  A document detailing the various changes between Tomcat versions.
* [When are self-signed certificates acceptable?](https://www.sslshopper.com/article-when-are-self-signed-certificates-acceptable.html){:target="_blank"}
  A compelling argument as to why self-signed certificates should not be used in a production environment

## Securing the Tomcat manager Application

### Rationale

* \"Free\" web application that comes with Tomcat distribution that lives in the Tomcat Lives in the `${tomcat_home}/webapps/manager` directory.
* Not enabled by default.
* Allows Tomcat administrators to deploy, undeploy, or reload web applications such as the TDS without having to shut down and restart Tomcat.
* If exploited, an attacker can use the manager application to install programs on your server willy-nilly.
* If you choose to enable the manager application, we _highly recommend_ enabling digested passwords and TSL/SSL encryption for the manager.
* Restricting access to the manager application to a small subset of IP addressess or host names using a Tomcat valve, etc., is also a good idea.
* Uninstall this application if you don't plan to use it.

#### Enabling TSL/SSL for the Tomcat manager application

1. Modify the deployment descriptor of the Tomcat manager application.
   Using your favorite editor, open the deployment descriptor for the Tomcat manager application:
   ~~~bash
   $ vi ${tomcat_home}/webapps/manager/WEB-INF/web.xml
   ~~~

   Locate the `<security-constraint>` elements (near the bottom of the file):

   ~~~xml
   <!-- Define a Security Constraint on this Application -->
   <!-- NOTE:  None of these roles are present in the default users file -->
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>
         HTML Manager interface (for humans)
       </web-resource-name>
       <url-pattern>/html/*</url-pattern>
     </web-resource-collection>
     <auth-constraint>
        <role-name>manager-gui</role-name>
     </auth-constraint>
   </security-constraint>
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>
         Text Manager interface (for scripts)
       </web-resource-name>
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
   ~~~

   The Tomcat 8 version of the manager application deployment descriptor contains a `<security-constraint>` section for each of the four possible `ContactPaths` (as per [Manager Application](https://tomcat.apache.org/migration.html){:target="_blank"} section of the Tomcat Migration Guide).

   Add a `<user-data-constraint>` with a `<transport-guarantee>` of `CONFIDENTIAL` for the desired `ContactPaths` to to enable port-forwarding to port `8443`:

   ~~~xml
   <!-- Define a Security Constraint on this Application -->
   <!-- NOTE:  None of these roles are present in the default users file -->
   <security-constraint>
     <web-resource-collection>
       <web-resource-name>
        HTML Manager interface (for humans)
       </web-resource-name>
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
       <web-resource-name>
        Text Manager interface (for scripts)
       </web-resource-name>
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
   ~~~

2. Verify TSL/SSL has been enabled for the Tomcat manager application.

   Restart Tomcat and verify TSL/SSL has been enabled for the Tomcat manager application: [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}
   
   {% include image.html file="tds/tutorial/production_servers/overview/managerssl.png" alt="manager ssl" caption="" %}

3. NOTE: You will have to redo this every time you upgrade Tomcat.

#### Troubleshooting

* Check the XML syntax in web.xml to make sure it is well-formed and without error.
* Did you specify a `<transport-guarantee>` of `CONFIDENTIAL`?
* Did you restart Tomcat after you made your changes to `web.xml`?

### Resources
* [Manager App HOW-TO](https://tomcat.apache.org/tomcat-8.0-doc/manager-howto.html){:target="_blank"}
  The Apache Tomcat document referencing how to use and configure the manager application.
* [Tomcat Migration Guide](https://tomcat.apache.org/migration.html){:target="_blank"}
  A document detailing the various changes between Tomcat versions contains a section dedicated to the manager application.

## Blocking Non-Essential Port Access At The Firewall

### Rationale
* It is easy to issue commands to Tomcat if you know:
  1. the correct port number; and
  2. the command expected on that port.
* Unless you are on a private network, you need a firewall to restrict who is allowed to access network ports.
* We recommend working with your systems/network administrator to block access to all non-essential ports at the firewall.

### For running the TDS, keep in mind the following:
* Port `8080` should have unrestricted access unless you plan to proxy requests to Tomcat from and HTTP server.
* If you are using any of the TDS monitoring and debugging tools, or the Tomcat `manager` application, you must also open up port `8443`.

### Resources
* Your local systems/network administrator:

  {% include image.html file="tds/tutorial/production_servers/overview/super.png" alt="super sys admin" caption="" %}

## Restricting Access To The TDS By Remote IP Address Or Host

### Rationale

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

* [The Valve Component](https://tomcat.apache.org/tomcat-8.0-doc/config/valve.html){:target="_blank"}
  Tomcat documentation about the various `valve` components available for use.

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

[Running The TDS Behind a Proxy Server](/tds_behind_proxy.html){:target="_blank"}
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