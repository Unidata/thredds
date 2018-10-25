---
title: Use Digested Passwords
last_updated: 2018-10-13
sidebar: tdsTutorial_sidebar
toc: false
permalink: digested_passwords.html
---

## Rationale

* Passwords stored in clear text are a vulnerability if the host is compromised.
* Better to have the passwords encrypted using a cryptographic hash functions (SHA, MD2, or MD5) and then stored in `tomcat-users.xml` file in the Tomcat `conf/` directory.
* Tomcat can be configured to support digested passwords (this is not the default setting).

How it works: When a client makes a request Tomcat will tell the client that a digested password is required.
Based on this dialog, the client will automatically digest the password entered by the user.

## Configure Tomcat to use digested passwords

1. First we need to enable digest passwords support in Tomcat by modifying the `UserDatabaseRealm` in the `server.xml` file in the Tomcat `conf/` directory.
   A Tomcat Realm represents a \"database\" of usernames, passwords, and roles assigned to tomcat users.

   | Realm Name | Purpose |
   | UserDatabaseRealm | The UserDatabaseRealm is enabled by default and reads clear text user password information stored in tomcat-users.xml. |
  
   Open the `server.xml` with your favorite editor:
   
   ~~~bash
   $ cd /usr/local/tds/tomcat/conf
   $ vi server.xml
   ~~~

   Locate the `UserDatabaseRealm` (in the `LockOutRealm`, right above the `Host` element):

   ~~~xml
   <!-- Use the LockOutRealm to prevent attempts to guess user passwords
     via a brute-force attack -->
   <Realm className="org.apache.catalina.realm.LockOutRealm">
   <!-- This Realm uses the UserDatabase configured in the global JNDI
        resources under the key "UserDatabase".  Any edits
        that are performed against this UserDatabase are immediately
        available for use by the Realm.  -->
   <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
     resourceName="UserDatabase"/>
   </Realm>

   <Host name="localhost"  appBase="webapps"
      unpackWARs="true" autoDeploy="true">
   ~~~

   Add the `CredentialHandler` element as an inner element of the `UserDatabaseRealm` element:

   ~~~xml
   <!-- Use the LockOutRealm to prevent attempts to guess user passwords
     via a brute-force attack -->
   <Realm className="org.apache.catalina.realm.LockOutRealm">
     <!-- This Realm uses the UserDatabase configured in the global JNDI
          resources under the key "UserDatabase".  Any edits
          that are performed against this UserDatabase are immediately
          available for use by the Realm.  -->
     <Realm className="org.apache.catalina.realm.UserDatabaseRealm" resourceName="UserDatabase">
       <CredentialHandler className="org.apache.catalina.realm.MessageDigestCredentialHandler" algorithm="SHA-256" />
     </Realm>
   </Realm>

   <Host name="localhost"  appBase="webapps"
         unpackWARs="true" autoDeploy="true">
   ~~~
   <a name="digest.sh"></a>
2. Create a SHA encrypted version of your password.

   Tomcat provides a script (`${tomcat_home}/bin/digest.sh`) that will encrypt a password string according to the algorithm specified.
   Use this script as follows with the password you made for yourself previously:

   ~~~bash
   $ /usr/local/tds/tomcat/bin/digest.sh -a SHA-256 supersecretpassword
   supersecretpassword:bb7a2b6cf8da7122125c663fc1585808170b2027677195e0ad121f87b27320ae$1$55003acb56e907b19d29d3b4211dc98c837354690bc90579742d6747efeec4ea
   ~~~

    {% include note.html content="
    To use a different algorithm, a salt, or to limit the length of the resulting password hash, consult the <a href=\"https://tomcat.apache.org/tomcat-8.5-doc/realm-howto.html#Digested_Passwords\" target=\"_blank\">syntax options</a> for the `tomcat_home/bin/digest.[bat|sh]` script.
    " %}
  
3. Update `tomcat-users.xml`.
   Replace your clear-text password in `${tomcat_home}/conf/tomcat-users.xml` with the encrypted version:
   
   ~~~bash
   $ pwd
   /usr/local/tds/tomcat/conf
   
   $ vi tomcat-users.xml
   ~~~
   
   ~~~xml
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
        <user username="admin" 
              password="bb7a2b6cf8da7122125c663fc1585808170b2027677195e0ad121f87b27320ae$1$55003acb56e907b19d29d3b4211dc98c837354690bc90579742d6747efeec4ea" 
              roles="manager-gui"/>
   </tomcat-users>
   ~~~
4. Restart Tomcat and verify digested passwords have been successfully enabled by logging into the Tomcat manager application using your password in clear text: [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}

    {% include note.html content="
    Note: Since we are using BASIC authentication, you will need to clear any authenticated sessions in your browser to test whether digested passwords have been enabled.
    " %}

## Troubleshooting

* Check the XML syntax in `tomcat-users.xml` and `server.xml` to make sure it is well-formed and without error.
* Did you restart Tomcat after you made your changes to `tomcat-users.xml` and `server.xml`?
* Any errors will be reported in the `catalina.out` file in the Tomcat `logs/` directory.
* You do not need to type the encrypted version of your password into the browser (the browser auto-magically encrypts your password for you before it transmits it to the server).
