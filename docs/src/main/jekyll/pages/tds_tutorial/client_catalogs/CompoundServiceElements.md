---
title: Compound service elements
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: compound_service_elements.html
---

{%include note.html content="
Reference documentation - A complete listing of recognized service types can be found in the catalog specification.
" %}

Datasets can be made available through more than one access method by defining and then referencing a _compound_ `service` element. The following:

~~~xml
<service name="all" serviceType="Compound" base="" >
  <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
  <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
</service>
~~~

defines a compound service named `all` which contains two nested services.
Any dataset that reference the compound service will have two access methods. For instance:

~~~xml
<dataset name="SAGE III Ozone 2006-10-31" urlPath="sage/20061031.nc" ID="20061031.nc">
  <serviceName>all</serviceName>
</dataset>
~~~

would result in these two access URLs, one for OpenDAP access 

`/thredds/dodsC/sage/20061031.nc`

and one for WCS access:

`/thredds/wcs/sage/20061031.nc`

Note: the contained services can still be referenced independently.
For instance:

~~~xml
<dataset name="Global Averages" urlPath="sage/global.nc" ID="global.nc">
  <serviceName>odap</serviceName>
</dataset>
~~~

results in a single access URL:

`/thredds/dodsC/sage/global.nc`
