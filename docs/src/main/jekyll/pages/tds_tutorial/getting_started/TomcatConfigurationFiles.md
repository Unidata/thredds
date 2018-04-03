---
title: Tomcat Log Files
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tomcat_configuration_files.html
---
## About server.xml

{%include note.html content="
Tomcat's configuration files, including `server.xml` can be found in in the Tomcat `conf/` directory.
" %}

* XML file (well-formed syntax is important).
* Tomcat's main configuration file.
* Changes to server.xml do not take effect until Tomcat is restarted.
* Where we make changes to enhance TDS security.

## Important elements in server.xml

1. Examine the Elements in server.xml.
   Move into the Tomcat conf/ directory and examine the server.xml file:

   ~~~bash
   $ pwd
   /usr/local/tds/apache-tomcat-8.0.24/logs
   $ cd ../conf

   $ less server.xml
   ~~~

   |----------|-----------|---------------------------|
   | Tag Name | Instances | How it relates to the TDS |
   |:---------|:---------:|:--------------------------|
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/server.html" title="The Server element represents the entire Catalina servlet container as a whole. It is the single outermost element in server.xml" target="_blank"><code>&lt;Server&gt;</code></a> | 1...1 | Not modified unless you want to change the port number Tomcat listens for a <code>SHUTDOWN</code> command. (Enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/globalresources.html" title="The GlobalNamingResources element defines the global Java Naming and Directory Interface (JNDI) resources for the Server." target="_blank"><code>  &lt;GlobalNamingResources&gt;</code></a> | 0...\* | Needed to contain the <code>UserDatabase</code> that corresponds to the <code>UserDatabaseRealm</code> used to authenticate users. (Enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/resources.html" title="The Resource element represents a static resource from which classes will be loaded and static files will be served." target="_blank"><code>    &lt;Resource&gt;</code></a> | 0...\* | Editable user database (<code>tomcat-users.xml</code>) used by <code>UserDatabaseRealm</code> to authenticate users. (<code>UserDatabaseRealm</code> Resource enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/service.html" title="The Service element represents the combination of one or more Connector components that share a single Engine component for processing incoming requests. The top Tomcat service is named Catalina (hence the log file name of catalina.out)." target="_blank"><code>  &lt;Service&gt;</code></a> | 1...\* | Not modified unless <code>you</code> wish to establish more than one service. (Catalina Service enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/connectors.html" title="The Connector element forward requests to the Engine using a specific protocol and returns the results to the requesting client." target="_blank"><code>  &lt;Connector&gt;</code></a> | 1...\* | Used to establish HTTP and SSL connections. Also will communicate with an web server for proxying requests. (HTTP connector enabled by default on port 8080.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/engine.html" title="The Engine element represents the entire request processing machinery associated with a particular Catlina Service." target="_blank"><code>  &lt;Engine&gt;</code></a> | 1...1 | Not modified unless you specify a Host other than <code>localhost</code>. (Enabled by default.) |
   | <a  href="http://tomcat.apache.org/tomcat-8.0-doc/config/realm.html" title="The Realm element represents a database of usernames, passwords, and roles (groups) assigned to those users." target="_blank"><code>    &lt;Realm&gt;</code></a> | 0...\* | The <code>UserDatabaseRealm</code> uses the </code>UserDatabase</code> configured in the global JNDI Resource. (<code>UserDatabaseRealm</code> enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/valve.html" title="The Valve element represents a component that will be inserted into the request processing pipeline for the associated containing element." target="_blank"><code>    &lt;Valve&gt;</code></a> | 0...\* | The <code>RemoteAddrValve</code> is used to filter access to the TDS based on IP address. (NOT enabled by default. You will need to add this if you want to use IP Filtering.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/host.html" title="The Host element represents a virtual host." target="_blank"><code>    &lt;Host&gt;</code></a> | 1...\* | Not modified unless you specify a <code>Host</code> other than <code>localhost</code>. (<code>localhost</code> enabled by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/realm.html" title="The Realm element represents a database of usernames, passwords, and roles (groups) assigned to those users." target="_blank"><code>     &lt;Realm&gt;</code></a> | 0...\* | We use the <code>MemoryRealm</code> to configuring Tomcat to use digested passwords. (NOT enabled by default. You will need to add this if you want to use digested passwords.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/valve.html" title="The Valve element represents a component that will be inserted into the request processing pipeline for the associated containing element." target="_blank"><code>      &lt;Valve&gt;</code></a> | 0...\* | We modify the <code>AccessLogValve</code> to customize the access logs generated by Tomcat. (NOT enabled by default. You will need to add this if you want to enable access logging. ) |

## About tomcat-users.xml

* XML file (well-formed syntax is important).
* Stores user names, passwords and roles.
* Changes to `tomcat-users.xml` do not take effect until Tomcat is restarted.
* What the TDS uses for user authentication and access control.


## Important elements in `tomcat-users.xml`

1. Examine the Elements in `tomcat-users.xml`.
   Open the `tomcat-users.xml` file:

   ~~~bash
   $ pwd
   /usr/local/tds/apache-tomcat-8.0.24/conf

   $ less tomcat-users.xml
   ~~~

   Reference the table below to see how the tomcat-users.xml elements relate to configuring TDS (mouse-over the element for a description):

   |----------|-----------|---------------------------|
   | Tag Name | Instances | How it relates to the TDS |
   |:---------|:---------:|:--------------------------|
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/realm-howto.html#UserDatabaseRealm" title="The tomcat-users element represents the single outermost element in tomcat-users.xml" target="_blank"><code>&lt;tomcat-users&gt;</code></a> | 1...1 | Not modified. (The only tag you get by default.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/realm-howto.html#UserDatabaseRealm" title="The role element defines one role or group a user can belong to." target="_blank"><code>&lt;role&gt;</code></a> | 1...\* | You will have at least two of these: one for the Tomcat manager application and one for the TDS. (You will need to add if you want to enable role-based authentication.) |
   | <a href="http://tomcat.apache.org/tomcat-8.0-doc/realm-howto.html#UserDatabaseRealm" title="The user element represents one valid user." target="_blank"><code>&lt;user&gt;</code></a> | 1...\* | You will need to create an entry for each user who needs access to the Tomcat manager application and/or the restricted areas of the TDS. (You will need to add if you want to enable user authentication.) |
