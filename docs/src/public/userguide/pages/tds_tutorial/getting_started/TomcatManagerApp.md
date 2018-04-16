---
title: Tomcat Manager Application
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_manager_app.html
---

## About the manager application

* "Free" web application that comes with Tomcat distribution.
* Lives in the `manager` directory in the Tomcat `webapps/` directory.
* Allows Tomcat administrators to deploy, undeploy, or reload web applications such as the TDS without having to shut down and restart Tomcat.
* Provides server status statistics for the JVM and each connector you have configured in `server.xml`.

{%include note.html content=
"More about manager: For more information about the Tomcat manager application, see the Tomcat Manager App HOW-TO documentation.
" %}

## Accessing the Tomcat manager application

{%include note.html content="
Changes to the manager application: The manager application URLs and roles have been re-structured.
See the [Tomcat Migration guide](http://tomcat.apache.org/migration.html) for more information.
" %}

Attempt to access the Tomcat `manager` application in your browser by visiting <http://localhost:8080/manager/html/>{:target="_blank"}.
You will be prompted to login via BASIC authentication, which will end in failure since we do not yet have permission to access the `manager` application.

{%include image.html file="tds/tutorial/getting_started/manager401.png" alt="Manager app with 401 response code" caption="" %}

{%include question.html content="
Based on what we know about Tomcat configuration, which file in the Tomcat `conf/` directory should we edit to grant ourselves access to the `manager` application?
" %}

{%include important.html content="
Keep in mind: Changes to `tomcat-users.xml` do not take effect until Tomcat is restarted.
" %}

## Granting access to the manager application

1. Modify `tomcat-users.xml` to add role and user elements.

   Using your favorite editor, open `${tomcat_home}/conf/tomcat-users.xml`:

   ~~~bash
   $ vi tomcat-users.xml
   ~~~

   Between the `<tomcat-users>` tags, add a `role` element and specify the `rolename` attribute as `manager`:

   ~~~xml
   <tomcat-users>
       <role rolename="manager-gui"/>
   </tomcat-users>
   ~~~

   Now add a new user by adding a user element.
   Create a username and password for the new user and specify manager-gui as one of the roles (in this example we are creating a user called *admin* with a corresponding password of *secret*):

   ~~~xml
   <tomcat-users>
       <role rolename="manager-gui"/>
       <user username="admin" password="secret" roles="manager-gui"/>   
   </tomcat-users>
   ~~~

Restart Tomcat and log into the manager application.

{%include ahead.html content="
To gain access to restricted parts of the TDS, you will perform the same steps you used to grant yourself access to the manager application.
" %}

2. Attempt to access the manager application again <http://localhost:8080/manager/html/>{:target="_blank"}, this time logging in using the user name and password specified in `tomcat-users.xml`:


   {% include image.html file="tds/tutorial/getting_started/manager.png" alt="Tomcat manager application" caption="" %}


   Voil&aacute;! You should have access to the manager application.


   {%include troubleshooting.html content="
   Check the XML syntax in tomcat-users.xml to make sure it is well-formed and without error.
   " %}
   {%include troubleshooting.html content="
   Did you restart Tomcat after you made your changes to `tomcat-users.xml`?
   " %}
   {%include troubleshooting.html content="
   Any errors will be reported in the Tomcat `logs/catalina.out` file.
   " %}

## Deploying the TDS using the manager application

1. Use the `manager` application to undeploy the TDS.

   Find the TDS in the list of web application on the _Applications_ page.
   Stop and then Undeploy the TDS:

   {% include image.html file="tds/tutorial/getting_started/undeploy.png" alt="Undeploy the TDS" caption="" %}

   List the contents of the Tomcat `webapps/` directory to verify that both `thredds.war` and the unpacked `thredds/` directory have been removed:

   ~~~bash
   $ pwd
   /usr/local/tds/apache-tomcat-8.0.24/conf
   $ cd ../webapps

   $ ls -l
   drwxr-xr-x 13 tds workshop     4096 Jul 15 09:37 docs
   drwxr-xr-x  6 tds workshop     4096 Jul 15 09:37 examples
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 host-manager
   drwxr-xr-x  5 tds workshop     4096 Jul 15 09:37 manager
   drwxr-xr-x  3 tds workshop     4096 Jul 15 09:37 ROOT
   ~~~

2. Deploy the TDS using the `manager` application.

   Upload the TDS WAR file using the _Deploy_ section of the manager application:

   {% include image.html file="tds/tutorial/getting_started/deploy.png" alt="Deploy the TDS" caption="" %}

   Confirm the deployment went as planned by accessing the TDS using your browser: <http://localthost:8080/thredds/>{:target="_blank"}
