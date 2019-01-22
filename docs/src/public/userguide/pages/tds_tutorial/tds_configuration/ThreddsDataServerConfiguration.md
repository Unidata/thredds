---
title: Basic TDS Configuration
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: basic_tds_configuration.html
---

## TDS Configuration File threddsConfig.xml

The TDS configuration file (`{tds.content.root.path}/thredds/threddsConfig.xml`) allows the TDS administrator to set a number of parameters that control the behavior of the TDS.
Most of these parameters will be set with reasonable default values.
However, a number of them allow you to describe your server, provide contact information, and change the "theme" of server-generated HTML pages.
So unless you want your server to be called "Initial TDS Installation", maintained by Support at "My Group" with contact email `support@my.group`, then you will need to change some of these settings.

## Server Information

Information describing your TDS installation and providing contact information is configured in the serverInformation element of the TDS configuration file. This includes:

 * basic information about the server (e.g., name, abstract, keywords, host institution)
 * contact information so that users will know where to go with problems or questions.

Here is what the default version of the serverInformation element looks like:

~~~xml
<serverInformation>
  <name>Initial TDS Installation (please change threddsConfig.xml)</name>
  <logoUrl>/thredds/threddsIcon.png</logoUrl>
  <logoAltText>Initial TDS Installation</logoAltText>

  <abstract>Scientific Data</abstract>
  <keywords>meteorology, atmosphere, climate, ocean, earth science</keywords>

  <contact>
    <name>Support</name>
    <organization>My Group</organization>
    <email>support@my.group</email>
    <!--phone></phone-->
  </contact>
  <hostInstitution>
    <name>My Group</name>
    <webSite>http://www.my.site/</webSite>
    <logoUrl>/thredds/myGroup.png</logoUrl>
    <logoAltText>My Group</logoAltText>
  </hostInstitution>
</serverInformation>
~~~

## Where is Server Information Used

The headers and footers of all TDS generated catalog HTML pages. Examples:

* <http://localhost:8080/thredds/catalog.html>{:target="_blank"}
* <http://thredds.ucar.edu/thredds/catalog.html>{:target="_blank"}

Server element of the WMS GetCapabilities document:
 * <http://thredds.ucar.edu/thredds/wms/grib/NCEP/GFS/Pacific_40km/best?service=WMS&version=1.3.0&request=GetCapabilities>{:target="_blank"}

The three supported server information documents:
 * An HTML document: <http://thredds.ucar.edu/thredds/serverInfo.html>{:target="_blank"}
 * An XML document: <http://thredds.ucar.edu/thredds/serverInfo.xml>{:target="_blank"}
 * A version only text document: <http://thredds.ucar.edu/thredds/serverVersion.txt>{:target="_blank"}

Other Places the Server Information Will be Included

 * The Server element of the WCS GetCapabilities document.
 * All generated THREDDS catalogs that don't override this information.

## Exercise: Add Server Information

Change the default server information in the TDS configuration file

1. Look at the current server information by viewing the Server Info page in a browser: http://localhost:8080/thredds/serverInfo.html

2. Edit the main TDS configuration catalog:

   ~~~bash
   $ cd ${tds.content.root.path}/content/thredds
   $ vi threddsConfig.xml
   ~~~

3. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

4. Check that the server information has changed in the Server Info page: <

   http://localhost:8080/thredds/serverInfo.html>{:target="_blank"}

## Appearance of Generated HTML Pages

Server and institution names, logos, and links given in the above section are used in all TDS generated HTML pages.

You can also specify the CSS files and several icons used in the HTML pages.
These are configured in the htmlSetup element of the TDS configuration file threddsConfix.xml.
Default CSS files are provided with the thredds.war distribution, and should not be modified.
Instead, these can be overridden by placing the appropriate CSS files in the `${tds.content.root.path}/thredds/public/` directory.

The following shows the default configuration of the htmlSetup element:

~~~xml
<htmlSetup>
  <!--
   * CSS documents used in generated HTML pages.
   * The CSS document given in the "catalogCssUrl" element is used for all pages
   * that are HTML catalog views. The CSS document given in the "standardCssUrl"
   * element is used in all other generated HTML pages.
   * -->
   <standardCssUrl>standard.css</standardCssUrl>
   <catalogCssUrl>catalog.css</catalogCssUrl>
   <datasetCssUrl>dataset.css</datasetCssUrl>
   <openDapCssUrl>tdsDap.css</openDapCssUrl>
   ...
   ...
   ...
</htmlSetup>
~~~

More details are given in the [threddsConfig.xml Reference document](/tds_config_ref.html#generated-html-pages).
The Catalog and Dataset HTML pages are generated using the Thymeleaf templating library.
This means you have full control over how these pages are generated.
Please see the [customizing the tds look and feel](/customizing_tds_look_and_feel.html) for more information.


## Include Generated JSON-LD in Dataset HTML Pages

This experimental feature has the overall goal of exposing TDS datasets to [Google's Dataset search](https://developers.google.com/search/docs/data-types/dataset).

~~~xml
<htmlSetup>
    ...
    ...
    ...
    <!--
     * Allow the TDS to generate JSON-LD in Dataset HTML pages following
     * schema.org Dataset (https://schema.org/Dataset)
     * The default is false, as this is experimental

    <generateDatasetJsonLD>false</generateDatasetJsonLD>
    -->
</htmlSetup>
~~~

If set to true, schema.org [`Dataset`](https://schema.org/Dataset){:target="_blank"} objects will be encoded using json-ld and embeded into the `<head>` element of the generated dataset HTML pages.
You can add individual datasets to a `sitemap.xml`, located in `tds.content.root.path/public` (you will need to create it the first time), and it will be avaliable at `<server>/thredds/sitemap.xml`.
The sitemap can be submitted to Google following [these instructions](https://support.google.com/webmasters/answer/183668){:target="_blank"}. 

Note that this will very likely change in the future to be less manual, and is 100% experimental!