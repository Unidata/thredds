---
title: Running The TDS Behind a Proxy Server
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: tds_behind_proxy.html
---

## About Reverse Proxies

View the Wikipedia entry on [Reverse Proxies](https://en.wikipedia.org/wiki/Reverse_proxy){:target="_blank"} for more information on reverse proxies uses and types of proxies servers.

## Uses of reverse proxies

* A reverse proxy is a proxy server that appears to clients to be an ordinary server.
   Requests are forwarded to one or more origin servers which handle the request.
   The response is returned as if it came directly from the proxy server.

   {% include image.html file="tds/tutorial/production_servers/overview/tds_reverse_proxy.png" alt="reverse proxy" caption="" %}

* Reverse proxies can be used to hide the existence and characteristics of the origin server(s) and can be an additional layer of defense and can protect against some OS and WebServer specific attacks.
  However, it does not provide any protection to attacks against vulnerabilities in the web application or proxy service itself (e.g., Apache, Tomcat).
* A reverse proxy can also be used for load balancing, caching content, content compression, and SSL acceleration.

## Setting Up A Reverse Proxy For The TDS

### Using Tomcat and Apache HTTP Server

* Using Apache as a front-end proxy server for the TDS running on Tomcat is perhaps the easiest method for setting up a reverse proxy for the TDS. 
  There are two methods to accomplish this:
  * Apache's [mod_proxy](#mod_proxy) in combination with Tomcat's HTTP connector; or
  * the [mod_jk](#mod_jk) Apache module with the Tomcat AJP connector.
* **Warning!**
   It is important to carefully configure your proxy so that the existence of the proxy is transparent to the end-user/client.
   For instance, when a web application (e.g., the TDS) on the backend server includes a [self-referential URL](#chgContextPath) in some response content, it should use the proxy server's name and port rather than those of the backend server.

<a id="chgContextPath" />
## Tomcat-Apache Proxy Documentation

* [Tomcat Connectors](https://tomcat.apache.org/tomcat-8.0-doc/connectors.html){:target="_blank"}
  Documentation describing the difference between the Tomcat HTTP and AJP connectors.
<a id="mod_proxy" />
* `mod_proxy`
   * [Tomcat HTTP Connector](https://tomcat.apache.org/tomcat-8.0-doc/config/http.html)
     Configuration for the Tomcat HTTP connector (for use with Apache's mod_proxy).
   * [Tomcat Proxy Support - How To](https://tomcat.apache.org/tomcat-8.0-doc/proxy-howto.html)
     Tomcat documentation showing how to use the build-in Apache module mod_proxy for Apache versions 1.3X and 2.X.
<a id="mod_jk" />
* `mod_jk`
  * [Tomcat AJP Connector](https://tomcat.apache.org/tomcat-8.0-doc/config/ajp.html)
    Configuration for the Tomcat AJP connector (for use with Apache's mod_jk).
  * [Tomcat Reverse Proxy - How To](https://tomcat.apache.org/tomcat-8.0-doc/proxy-howto.html)
    Configurations and fine tuning of a reverse proxy set up using the mod_jk Apache module.

### Changing the TDS Context Path (`/thredds`)
 
We **do not recommend** changing the TDS context path (the `/thredds` part of the URL path). However, if your network configuration requires that you use a different context path (e.g., `/my/thredds`) or you are proxying two TDS installations and need to differentiate them with different context paths (e.g., `/thredds1` and `/thredds2` ), you will need to make the following changes:

1. Rename the `thredds.war` file to match the desired context path before you deploy it to Tomcat.
   Tomcat and other servlet engines direct incoming requests to a particular web application when the beginning of the request URL path matches the context path of that particular webapp.
   The easiest way to let Tomcat (or any other servlet engine) know what context path to use for a given webapp is to rename that webapp\'s `.war` file before deploying it to Tomcat.

   For instance, if you want all URLs starting with `/thredds2` to be handled by your TDS install, rename the `thredds.war` file to `thredds2.war` before you deploy it to Tomcat.

   If the desired context path is a multi-level context path (e.g., `/my/thredds` ), you must use a pound sign ("#") in the `.war` filename to encode the slash (`/`).
   In this case, the `thredds.war` file would need to be renamed to `my#thredds.war`.

   {%include important.html content="
     The deployment descriptor ( web.xml file) is overwritten during deployment which means this edit must be done every time the TDS is re-deployed.
   " %}

2. Edit the TDS web.xml file and change the value of the `ContextPath` parameter to match the desired context path.

   The TDS uses the value of the `ContextPath` context parameter (as defined in the TDS `web.xml` file) when generating TDS URLs in certain situations.
   To make sure all generated URLs are consistent, you must change the value of the `ContextPath` parameter to match the desired context path.

   (Changing the value of `ContextPath` will no longer be necessary in a future release once we require Tomcat 6.0 (Servlet 2.5).

   The TDS web.xml file is located in `${tomcat_home}/webapps/<contextPath>/WEB-INF/web.xml`, where `<contextPath>` is the value of the desired context path.
   The `ContextPath` context parameter is defined in the web.xml file (starting at line 12):

   ~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>thredds</param-value>
   </context-param>
   ~~~

   For the `/thredds2` example, it should be changed to:

   ~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>thredds2</param-value>
   </context-param>
   ~~~

   And for the `/my/thredds` example, it should be changed to:
   ~~~xml
   <context-param>
     <param-name>ContextPath</param-name>
     <param-value>my/thredds</param-value>
   </context-param>
   ~~~

3. Edit your TDS configuration catalogs and change the service base URLs to start with the desired context path.
 
   So that users will receive the correct data access URLs for datasets served by your TDS, the base URLs given by the service elements in your TDS configuration catalogs must match the desired context path.

   An OPeNDAP service element on a TDS with the context path of `/thredds2` would need to look similar to this:

   ~~~xml
   <service name="odap" serviceType="OPeNDAP" base="/thredds2/dodsC/"/>
   ~~~

   And similarly, an OPeNDAP service element on a TDS with the context path of `/my/thredds` would need to look similar to this:

   ~~~xml
   <service name="odap" serviceType="OPeNDAP" base="/my/thredds/dodsC/"/>
   ~~~

### Troubleshooting tips

Check that the catalog URL in the title of the HTML view of catalogs matches the requested URL.
Check that the Data Access URL in the OPeNDAP Data Access Form matches the requested URL (minus the `.html` suffix).
If you have [TDS Remote Management](/remote_management_ref.html) configured, go to the TDS debug page (e.g., [http://localhost:8080/thredds/admin/debug](http://localhost:8080/thredds/admin/debug){:target="_blank"}) and follow the \"Show HTTP Request info\" link.
Once there, check that the values listed for server name and port and the context path all match the appropriate values from the request URL, e.g., for the URL [http://localhost:8080/thredds/admin/debug?General/showRequest](http://localhost:8080/thredds/admin/debug?General/showRequest){:target="_blank"} , the values should be
`req.getServerName():` `localhost`
`req.getServerPort():` `8080`
`req.getContextPath():` `/thredds`