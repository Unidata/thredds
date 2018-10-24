---
title: Tomcat Manager Application
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_manager_app.html
---

## About the manager application

{%include note.html content="
This section assumes you have successfully installed the JDK and Tomcat Servlet Container as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Installation of Java and Tomcat</a> section.
" %}

* "Free" web application that comes with Tomcat distribution.
* Lives in the `manager` directory in the Tomcat `webapps/` directory.
* Allows Tomcat administrators to deploy, un-deploy, or reload web applications such as the TDS without having to shut down and restart Tomcat.
* Provides server status statistics for the JVM and each connector you have configured in `server.xml`.

{%include note.html content=
"For more information about the Tomcat manager application, see the <a href=\"https://tomcat.apache.org/tomcat-8.5-doc/manager-howto.html\" target=\"_blank\">Tomcat Manager App HOW-TO</a> documentation.
" %}

## Accessing the Tomcat manager application

Attempt to access the Tomcat `manager` application in your browser by visiting [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}.
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
   
   You should see something like this:
   
   ~~~bash
   <?xml version="1.0" encoding="UTF-8"?>
   <!--
     Licensed to the Apache Software Foundation (ASF) under one or more
     contributor license agreements.  See the NOTICE file distributed with
     this work for additional information regarding copyright ownership.
     The ASF licenses this file to You under the Apache License, Version 2.0
     (the "License"); you may not use this file except in compliance with
     the License.  You may obtain a copy of the License at
   
         http://www.apache.org/licenses/LICENSE-2.0
   
     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
   -->
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
   <!--
     <role rolename="tomcat"/>
     <role rolename="role1"/>
     <user username="tomcat" password="<must-be-changed>" roles="tomcat"/>
     <user username="both" password="<must-be-changed>" roles="tomcat,role1"/>
     <user username="role1" password="<must-be-changed>" roles="role1"/>
   -->
   </tomcat-users>
   ~~~

   Between the `<tomcat-users>` tags, un-comment the `role` and `user` tags.  
   Add a `role` element and specify the `rolename` attribute as `manager` and delete any un-used roles: 

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
     <user username="tomcat" password="<must-be-changed>" roles="tomcat"/>
     <user username="both" password="<must-be-changed>" roles="tomcat,role1"/>
     <user username="role1" password="<must-be-changed>" roles="role1"/>
   </tomcat-users>

   ~~~

   Now add a new user by adding a user element.
   Create a username and password for the new user and specify `manager-gui` as one of the roles and delete any un-used users.
   
   In this example we are creating a user called *admin* with a corresponding password of *supersecretpassword*:

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
     <user username="admin" password="supersecretpassword" roles="manager-gui"/>
   </tomcat-users>
   ~~~

Restart Tomcat and log into the manager application.

{%include ahead.html content="
To gain access to restricted parts of the TDS, you will perform the same steps you used to grant yourself access to the manager application.
" %}

2. Attempt to access the manager application again [http://localhost:8080/manager/html/](http://localhost:8080/manager/html/){:target="_blank"}, this time logging in using the user name and password specified in `tomcat-users.xml`:


   {% include image.html file="tds/tutorial/getting_started/manager.png" alt="Tomcat manager application" caption="" %}


   Voil&aacute;! You should have access to the manager application.


   {%include troubleshooting.html content="
   Check the XML syntax in `tomcat-users.xml` to make sure it is well-formed and without error.
   " %}
   {%include troubleshooting.html content="
   Did you restart Tomcat after you made your changes to `tomcat-users.xml`?
   " %}
   {%include troubleshooting.html content="
   Any errors will be reported in the Tomcat `logs/catalina.out` file.
   " %}

## Deploying the TDS using the manager application

1. Use the `manager` application to un-deploy the TDS.

   Find the TDS in the list of web application on the _Applications_ page.
   Stop and then Undeploy the TDS:

   {% include image.html file="tds/tutorial/getting_started/undeploy.png" alt="Undeploy the TDS" caption="" %}

   List the contents of the Tomcat `webapps/` directory to verify that both `thredds.war` and the unpacked `thredds/` directory have been removed:

   ~~~bash
   $ pwd
   /usr/local/tds/tomcat
   $ cd webapps

   $ ls -l
   total 20
   drwxr-x--- 14 oxelson ustaff 4096 Oct 24 13:29 docs
   drwxr-x---  6 oxelson ustaff 4096 Oct 24 13:29 examples
   drwxr-x---  5 oxelson ustaff 4096 Oct 24 13:29 host-manager
   drwxr-x---  5 oxelson ustaff 4096 Oct 24 13:29 manager
   drwxr-x---  3 oxelson ustaff 4096 Oct 24 13:29 ROOT
   ~~~

2. Deploy the TDS using the `manager` application.

   Upload the TDS WAR file using the _Deploy_ section of the manager application:

   {% include image.html file="tds/tutorial/getting_started/deploy.png" alt="Deploy the TDS" caption="" %}

   Confirm the deployment went as planned by accessing the TDS using your browser:[http://localhost:8080/thredds/](http://localhost:8080/thredds/){:target="_blank"} 

{%include note.html content="
Running an older version of Tomcat?  The manager application URLs and roles have been re-structured.
See the <a href=\"http://tomcat.apache.org/migration.html\" target=\"_blank\">Tomcat Migration guide</a> for more information.
" %}