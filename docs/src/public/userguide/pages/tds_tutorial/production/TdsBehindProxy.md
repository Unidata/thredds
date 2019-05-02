---
title: Running The TDS Behind a Proxy Server
last_updated: 2018-11-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tds_behind_proxy.html
---

This section demonstrates how to run the TDS and Tomcat Servlet Container behind an Apache HTTP proxy server using the [`mod_jk`](https://tomcat.apache.org/connectors-doc/){:target="_blank"} connector.

{%include note.html content="
This section assumes you have successfully performed the tasks as outlined in the <a href=\"install_java_tomcat.html\" target=\"_blank\">Getting Started With The TDS</a> section of this tutorial, enabled <a href=\"digested_passwords.html\" target=\"_blank\">Enabled digested passwords</a> in Tomcat, and are familiar with maintaining the <a href=\"https://httpd.apache.org/\" target=\"_blank\">Apache HTTP</a> server in a production environment.
" %}

## About Reverse Proxies

View the Wikipedia entry on [Reverse Proxies](https://en.wikipedia.org/wiki/Reverse_proxy){:target="_blank"} for more information on reverse proxies uses and types of proxies servers.

#### Uses of reverse proxies

* A reverse proxy is a proxy server that appears to clients to be an ordinary server.
   Requests are forwarded to one or more origin servers which handle the request.
   The response is returned as if it came directly from the proxy server.

   {% include image.html file="tds/tutorial/production_servers/tds_reverse_proxy.png" alt="reverse proxy" caption="" %}

* Reverse proxies can be used to hide the existence and characteristics of the origin server(s) and can be an additional layer of defense and can protect against some OS and WebServer specific attacks.
  However, it does not provide any protection to attacks against vulnerabilities in the web application or proxy service itself (e.g., Apache, Tomcat).
* A reverse proxy can also be used for load balancing, caching content, content compression, and SSL acceleration.

#### Using Tomcat and Apache HTTP server

{%include note.html content="
The TDS reverse proxy using Apache has been tested and vetted by Unidata.  Other HTTPD servers (e.g., NGINX) have not yet been explored.
" %}

* Using Apache as a front-end proxy server for the TDS running on Tomcat is perhaps the easiest method for setting up a reverse proxy for the TDS. 
  There are two methods to accomplish this:
  * Apache's [mod_proxy](#mod_proxy) in combination with Tomcat's HTTP connector; or
  * the [mod_jk](#mod_jk) Apache module with the Tomcat AJP connector.
  
  {%include warning.html content="
  It is important to carefully configure your proxy so that the existence of the proxy is transparent to the end-user/client.
     For instance, when a web application (e.g., the TDS) on the backend server includes a <a href=\"#chgContextPath\">self-referential URL</a> in some response content, it should use the proxy server’s name and port rather than those of the backend server.                                                                                                                                                           
  " %}

#### Tomcat-Apache proxy documentation

* [Tomcat Connectors](https://tomcat.apache.org/tomcat-8.5-doc/connectors.html){:target="_blank"}
  Documentation describing the difference between the Tomcat HTTP and AJP connectors.
<a id="mod_proxy" />
* `mod_proxy`
   * [Tomcat HTTP Connector](https://tomcat.apache.org/tomcat-8.5-doc/config/http.html){:target="_blank"}
     Configuration for the Tomcat HTTP connector (for use with Apache's mod_proxy).
   * [Tomcat Proxy Support - How To](https://tomcat.apache.org/tomcat-8.5-doc/proxy-howto.html){:target="_blank"}
     Tomcat documentation showing how to use the build-in Apache module mod_proxy for Apache versions 1.3X and 2.X.
<a id="mod_jk" />
* `mod_jk`
  * [Tomcat AJP Connector](https://tomcat.apache.org/tomcat-8.5-doc/config/ajp.html){:target="_blank"}
    Configuration for the Tomcat AJP connector (for use with Apache's mod_jk).
  * [Tomcat Reverse Proxy - How To](https://tomcat.apache.org/tomcat-8.5-doc/proxy-howto.html){:target="_blank"}
    Configurations and fine tuning of a reverse proxy set up using the mod_jk Apache module.
    
    {%include note.html content="
    The use of the <a href=\"https://tomcat.apache.org/connectors-doc/\" target=\"_blank\">`mod_jk`</a> has been tested and vetted by Unidata.  Other modules used for creating a reverse proxy have not yet been explored.
    " %}
    
## Implementing The Tomcat-Apache Proxy Using AJP

The following example shows how to implement a proxy using the Apache HTTPD server, Tomcat Servlet Container, and Tomcat's `mod_jk` on a linux system.

#### Install `mod_jk`

1. [Download](https://tomcat.apache.org/download-connectors.cgi){:target="_blank"} the latest version of Tomcat's `mod_jk` module.

2. Build and install the `mod_jk` module as per the installation instructions that come bundled with the download.  The build and installation will need to be done as either `root`, `sudo`, or as user with privileges to modify Apache.
   
    ~~~~bash
    # tar xvfz tomcat-connectors-1.2.xx-src.tar.gz
    # cd tomcat-connectors-1.2.xx-src/native
    # ./configure --with-apxs=/usr/bin/apxs  <--- path to your apache apxs
    # make
    # make install
    ~~~~ 

    Confirm the module was added to the directory in which the Apache HTTPD server stores its modules (`/usr/local/apache/modules` in this example):
    
    ~~~~bash
    # cd /usr/local/apache/modules
    # ls -l  mod_jk.so
    
    -rwxr-xr-x 1 root root 1147204 Oct  8 12:34 mod_jk.so
    ~~~~

#### Configure apache to use `mod_jk` to talk to Tomcat

1.  Update Apache configurations to use the `mod_jk` module.

    `mod_jk` was built as a [DSO module](https://httpd.apache.org/docs/current/dso.html){:target="_blank"}, therefore you will need to update your Apache configurations to enble this 3rd-party module:

    The following example shows adding `mod_jk` configurations to a Apache HTTPD 2.4 server built from source.  Modify the main Apache server configuration file (usually `httpd.conf`) in the following manner (in this example `/usr/local/apache/conf` is where the Apache configuration files are located):
   
    ~~~~bash
    # cd /usr/local/apache/conf
    # vi http.conf
    ~~~~
    
    Add the following configurations to enable the `mod_jk` module, restrict access to web application `WEB-INF` and `META-INF` directories, and tell Apache where to find the `workers.properties` file using the `JkWorkersFile` directive. (The `worker.properties` file is discussed in detail below.)
    
    ~~~~xml
    # Third party modules
    LoadModule jk_module    modules/mod_jk.so
 
    <IfModule jk_module>
        JkWorkersFile "conf/workers.properties"
        JkShmFile "logs/mod_jk.shm"
        JkLogFile "logs/mod_jk.log"
        JkLogLevel warn
        #Tomcat Security Section
        <LocationMatch "/WEB-INF/">
            Require all denied
        </LocationMatch>
    
        <LocationMatch "/META-INF/">
            Require all denied
        </LocationMatch>
    </IfModule>
    ~~~~

    {% include note.html content="
     Consult the Tomcat documentation for more information about the <a href=\"https://tomcat.apache.org/connectors-doc/reference/apache.html\" target=\"_blank\">`mod_jk`</a> directives.
    " %}


2. Create a [`workers.properties`](https://tomcat.apache.org/connectors-doc/reference/workers.html){:target="_blank"} file.
 
   The `mod_jk` modules in the Apache HTTPD server uses the `workers.properties` file to relevant map requests to the TDS using Tomcat's AJP connector.  
   
   Use your favorite text editor to create a `workers.properties` file in the Apache configuration directory that you specified in the previous step using the `JkWorkersFile` directive:

    ~~~~bash
    # cd /usr/local/apache/conf
    # vi http.conf
    ~~~~

    Add the following configurations to the `workers.properties` module to define a `worker` that will handle communication between the Apache HTTPD server and Tomcat.  
     
    ~~~~bash
    # workers.properties
    # To allow tomcat and apache to talk to each other.
    # needed by mod_jk
    
    # Define workers
    worker.list=worker1
    
    # Define workers using ajp13 protocol to forward requests to the Tomcat processes.
    worker.worker1.type=ajp13
    
    # TDS app
    # worker1 will talk to Tomcat listening on localhost at port 8009
    worker.worker1.host=localhost
    worker.worker1.port=8009
    ~~~~

    {% include note.html content="
     Consult the Tomcat documentation for more information about the <a href=\"https://tomcat.apache.org/connectors-doc/reference/workers.html\" target=\"_blank\">workers.properties</a> file syntax and options.
    " %}

3. Configure your Apache host to send the appropriate requests to TDS via `mod_jk` and Tomcat AJP.
    
   You will use the `JkMount` directives that come with the `mod_jk` to specifiy/match which URL requests should be proxied to the TDS in the Tomcat Servlet Container.
   
   The following shows an example of configuring an Apache `VirtualHost` with the `JkMount` directives.  Note that these directives reference the `worker` configured in the `workers.properties` file in the previous step. 
   
   ~~~~bash
   <VirtualHost IP_ADDRESS:PORT>
       ServerName thredds.unidata.ucar.edu 
       ...
       
       # Proxy requests to the TDS
       JkMount /thredds* worker1
       JkMount /thredds worker1
       # Proxy requests to the Manager App
       JkMount /manager* worker1
       JkMount /manager worker1
       
       ...
   </VirtualHost>
   ~~~~

    {% include note.html content="
     Consult the Tomcat documentation for more information about the <a href=\"https://tomcat.apache.org/connectors-doc/reference/apache.html\" target=\"_blank\">`JKMount`</a> directive.
    " %}


#### Configure Tomcat and the TDS for the proxy

1. Modify the Tomcat `AJP Connector`

   In the `$TOMCAT_HOME/conf/server.xml` file, locate  the `AJP Connector` (uncommented and enabled by default) and add the following additional configuration to it:

   ~~~~xml
   <!-- AJP 1.3 Connector on port 8009 -->
       <Connector port="8009" 
                  enableLookups="false"
                  useBodyEncodingForURI="true"
                  connectionTimeout="20000"
                  protocol="AJP/1.3" />
   ~~~~

   {% include note.html content="
   Consult the Tomcat documentation for more information about the <a href=\"https://tomcat.apache.org/tomcat-8.5-doc/config/ajp.html\" target=\"_blank\">AJP Connector</a> configuration options.
   " %}
  
2. Disable any active `Java HTTP/1.1 Connector` and the `SSL HTTP/1.1 Connector` Tomcat connectors.

   {%include important.html content="
   Only perform this step is you have enabled Apache to handle the SSL/TLS encryption for Tomcat and the TDS.
   " %}

   This will prevent direct communication to Tomcat via ports `8080` and `8443` ensuring the AJP proxy via Apache is the only HTTP method by which to access Tomcat and the TDS.

   Locate the `Java HTTP/1.1 Connector` listening on port `8080` and the active `SSL HTTP/1.1 Connector` listening on port `8443` and comment them out:
   ~~~~xml
     <!--
       <Connector port="8080" 
                  protocol="HTTP/1.1"
                  connectionTimeout="20000"
                  redirectPort="8443" />
     -->
     ...
     <!--
       <Connector port="8443" 
                  protocol="org.apache.coyote.http11.Http11NioProtocol" 
                  maxThreads="150" 
                  SSLEnabled="true">
          <SSLHostConfig>
              <Certificate certificateKeystoreFile="conf/localhost-rsa.jks" 
                           type="RSA" />
          </SSLHostConfig>
       </Connector>
      -->
   ~~~~ 

3. Configure the TDS to relinquish control of TLS/SSL to Apache

   {%include important.html content="
   Only perform this step is you have enabled Apache to handle the SSL/TLS encryption for Tomcat and the TDS.
   " %}

   The TDS deployment descriptor (`$TOMCAT_HOME/webapps/thredds/WEB-INF/web.xml`) is configured to only allow access parts of the TDS application via TLS/SSL.  Because we've disabled Tomcat's handling of the TLS/SSL, we need to update these configurations.

   Use your favorite editor to open the TDS `$TOMCAT_HOME/webapps/thredds/WEB-INF/web.xml` file.  Around line 106 you'll start seeing a configs that look like the following:
    
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
    ...
   ~~~~
   
   These configs restrict access to the url in the `<url-pattern>` tags to the role in the `<role-name>` tags (which correspond to the roles defined in the `$TOMCAT_HOME/conf/tomcat-users.xml` file).  The `<transport-guarantee>CONFIDENTIAL</transport-guarantee>` says access must take place via HTTPS.  
   
   For any restricted part of the TDS you want to access that is listed here, you'll need to comment out the configurations in the `<user-data-constraint>` tags:

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
        <!-- do not use tomcat https
          <user-data-constraint>
            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
          </user-data-constraint>
        -->
      </security-constraint>
    ...
   ~~~~
   
   {%include warning.html content="
   If you leave outside access to the TDS and Tomcat open via port `8080` this once protected portion of the TDS is now open in the clear. Hence, we recommend disabling these connectors if you are using Apache as a proxy).  
   
   Also, keep these changes to the configurations in mind if you ever decide to reverse or undo the Apache reverse proxy!
   " %}
   
## Changing the TDS Context Path (`/thredds`)
 
We **do not recommend** changing the TDS context path (the `/thredds` part of the URL path). However, if your network configuration requires that you use a different context path (e.g., `/my/thredds`) or you are proxying two TDS installations and need to differentiate them with different context paths (e.g., `/thredds1` and `/thredds2` ), you will need to make the following changes:

1. Rename the `thredds.war` file to match the desired context path before you deploy it to Tomcat.
   Tomcat and other servlet engines direct incoming requests to a particular web application when the beginning of the request URL path matches the context path of that particular webapp.
   The easiest way to let Tomcat (or any other servlet engine) know what context path to use for a given webapp is to rename that webapp\'s `.war` file before deploying it to Tomcat.

   For instance, if you want all URLs starting with `/thredds2` to be handled by your TDS install, rename the `thredds.war` file to `thredds2.war` before you deploy it to Tomcat.

   If the desired context path is a multi-level context path (e.g., `/my/thredds` ), you must use a pound sign ("#") in the `.war` filename to encode the slash (`/`).
   In this case, the `thredds.war` file would need to be renamed to `my#thredds.war`.

   {%include important.html content="
     The deployment descriptor (`web.xml` file) is overwritten during deployment which means this edit must be done every time the TDS is re-deployed.
   " %}

2. Edit the TDS `web.xml` file and change the value of the `ContextPath` parameter to match the desired context path.

   The TDS uses the value of the `ContextPath` context parameter (as defined in the TDS `web.xml` file) when generating TDS URLs in certain situations.
   To make sure all generated URLs are consistent, you must change the value of the `ContextPath` parameter to match the desired context path.

   (Changing the value of `ContextPath` will no longer be necessary in a future release once we require Tomcat 6.0 (Servlet 2.5).

   The TDS web.xml file is located in `${tomcat_home}/webapps/<contextPath>/WEB-INF/web.xml`, where `<contextPath>` is the value of the desired context path.
   The `ContextPath` context parameter is defined in the web.xml file (starting at line 12):

   ~~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>thredds</param-value>
   </context-param>
   ~~~~

   For the `/thredds2` example, it should be changed to:

   ~~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>thredds2</param-value>
   </context-param>
   ~~~~

   And for the `/my/thredds` example, it should be changed to:
   ~~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>my/thredds</param-value>
   </context-param>
   ~~~~

3. Edit your TDS configuration catalogs and change the service base URLs to start with the desired context path.
 
   So that users will receive the correct data access URLs for datasets served by your TDS, the base URLs given by the service elements in your TDS configuration catalogs must match the desired context path.

   An OPeNDAP service element on a TDS with the context path of `/thredds2` would need to look similar to this:

   ~~~~xml
   <service name="odap" serviceType="OPeNDAP" base="/thredds2/dodsC/"/>
   ~~~~

   And similarly, an OPeNDAP service element on a TDS with the context path of `/my/thredds` would need to look similar to this:

   ~~~~xml
   <service name="odap" serviceType="OPeNDAP" base="/my/thredds/dodsC/"/>
   ~~~~

## Troubleshooting

* Check that the catalog URL in the title of the HTML view of catalogs matches the requested URL.
* Check that the Data Access URL in the OPeNDAP Data Access Form matches the requested URL (minus the `.html` suffix).
* If you have [TDS Remote Management](remote_management_ref.html){:target="_blank"}
 configured, go to the TDS debug page (e.g., [http://localhost:8080/thredds/admin/debug](http://localhost:8080/thredds/admin/debug){:target="_blank"}) and follow the \"Show HTTP Request info\" link.
* Once there, check that the values listed for server name and port and the context path all match the appropriate values from the request URL, e.g., for the URL [http://localhost:8080/thredds/admin/debug?General/showRequest](http://localhost:8080/thredds/admin/debug?General/showRequest){:target="_blank"} , the values should be

   ~~~~bash
   req.getServerName(): localhost
   req.getServerPort(): 8080
   req.getContextPath(): /thredds
   ~~~~