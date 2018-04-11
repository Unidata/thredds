---
title: Adding OGC/ISO Services
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: /adding_ogc_iso_services.html
---

## Configure TDS to Allow WCS, WMS, and `ncISO` Access

Out of the box, the TDS distribution will have WCS, WMS, and `ncISO` enabled.
If you do not wish to use these services, they must be explicitly allowed in the `threddsConfig.xml` file.
Please see the  [threddsConfig.xml file](/tds_config_ref.html#wcs-service) documentation for information on how to disable these services.
The default `threddsConfig.xml` file (which should now be in your `${tds.content.root.path}/content/thredds` directory) contains commented out sections for each of these services.

### `WCS` Configuration

The following section in the `threddsConfig.xml` file controls the WCS service:

~~~xml
<WCS>
  <allow>true</allow>
  ...
</WCS>
~~~

Additional `WCS` configuration options can be set in the `threddsConfig.xml` file.
More details are available in the `WCS` section of the [threddsConfig.xml file](/tds_config_ref.html#wcs-service) documentation.

### `WMS` Configuration

The following section in the `threddsConfig.xml` file controls the WMS service:

~~~xml
<WMS>
  <allow>true</allow>
  ...
</WMS>
~~~

Additional `WMS` configuration options can be set in the `threddsConfig.xml` file,
More details are available in the `WMS` section of the [threddsConfig.xml file](/tds_config_ref.html#wms-service) documentation.

### `ncISO` Configuration

The following section in the `threddsConfig.xml` file controls the `ncIso` services:

~~~xml
<NCISO>
  <ncmlAllow>true</ncmlAllow>
  <uddcAllow>true</uddcAllow>
  <isoAllow>true</isoAllow>
</NCISO>
~~~

Each `*Allow` element allows one of the three `ncISO` services.

### Adding `WCS` and `WMS` Services

As long as the `WCS` and `WMS` services are enabled, all that is required for the TDS to provide `WCS` and `WMS` access to datasets is for those datasets to reference `WCS` and `WMS` service elements.
Adding them to an existing compound service would look something like this:

~~~xml
<service name="grid" serviceType="Compound" base="" >
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
    <service name="wms" serviceType="WMS" base="/thredds/wms/" />
    <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/" />
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/" />
</service>
~~~

### Adding `ncISO` Services

Similar to above, as long as the `ncISO` services are enabled, all that is required for the TDS to provide `ncISO` services on datasets is for those datasets to reference the `ncISO` service elements.
For instance, adding to the same compound service as above:

~~~xml
<service name="grid" serviceType="Compound" base="" >
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
    <service name="wms" serviceType="WMS" base="/thredds/wms/" />
    <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/" />
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/" />
    <service name="ncml" serviceType="NCML" base="/thredds/ncml/" />
    <service name="uddc" serviceType="UDDC" base="/thredds/uddc/" />
    <service name="iso" serviceType="ISO" base="/thredds/iso/" />
</service>
~~~

### Exercise: Setup `WCS` and `WMS` Access for NAM Data

1. Edit the TDS configuration file and allow `WCS` and `WMS` services as discussed above

   ~~~bash
   $ cd ${tds.content.root.path}/thredds
   $ vim threddsConfig.xml
   ~~~

   and add/replace the `WCS` and `WMS` elements (as described above)

   ~~~xml
   <WCS>
     <allow>true </allow>
   </WCS>
   <WMS>
     <allow>true</allow>
   </WMS>
   ~~~

2. Edit the `catalog.xml` file and add `WCS` and `WMS` services to the NAM dataset

   ~~~xml
   <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
   <service name="wms" serviceType="WMS" base="/thredds/wms/" />
   ~~~

3. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

4. Test that `WCS` and `WMS` are working:
   1. Bring the catalog up in a browser: <http://localhost:8080/thredds/catalog.html>{:target="_blank"}
   2. Click down to one of the NAM dataset pages.
   3. Select the `WCS` Access link
   4. Go back, select the `WMS` Access link

5. Check Dataset Viewer Links for Godiva2 (ToolsUI and Godiva2 are there but IDV is not).

### Adding `Grid` DataType to Datasets

Once datasets are accessible over the `WMS` and `WCS` services, a quick look at the dataset pages shows several `Viewer` links available for each dataset (`ToolsUI` and `Godiva2`).
The IDV `Viewer` link is only added for datasets with a `Grid` data type.
This is not whether the dataset is recognized by the `CDM` as gridded but rather if the metadata in the catalog indicates that the dataset is a `Grid`.
This is accomplished with the `dataType` metadata element:

~~~xml
<dataType>Grid</dataType>
~~~

### Exercise: Add `Grid` DataType to the NAM Data

1. Edit the `catalog.xml` file and add a `Grid` `dataType` element (as above) to the NAM dataset.
2. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

3. Check the dataset pages for the IDV Viewer link.

### More `WMS` Configuration

Besides the basic `WMS` configuration available in the `threddsConfig.xml` file, there are additional configuration settings in the `wmsConfig.xml` file.
These settings can be applied globally, by dataset, by variable in a dataset, or to variables in any dataset by CF standard name.

### Default Image Styling

There are additional configuration settings for default image styling including settings for the default values of color scale range, palette name, and number of color bands as well as whether to use a linear or logarithmic scale.

Detailed information is available from the [ncWMS/MyOcean]{updatme} `WMS` Detailed Configuration web page.

### Interval Time vs Full Time List in `GetCapabilities` Documents

By default, the `WMS` will list all time values in a `GetCapabilities` document.
For long time-series, this list can cause the `GetCapabilities` document to be quite large.
Datasets/variables can be configured to use time intervals with the addition of an `intervalTime` element in the `wmsConfig.xml` file.
For instance:

~~~xml
<intervalTime>true</intervalTime>
~~~
     
Unfortunately, though time intervals are part of the `WMS` specification, not all `WMS` clients know how to interpret time intervals in the `GetCapabilities` document.

### Exercise: Try Modifying the `wmsConfig.xml` File

1. Open a dataset in Godiva2 and plot a parameter.
2. Notice the default color scale range is [-50,50].
   Decide on a better color scale range.
3. Open the " `WMS` Detailed Configuration" page in your browser.
4. Edit the wmsConfig.xml file

   ~~~bash
   $ cd ${tds.content.root.path}/thredds
   $ vi, wmsConfig.xml
   ~~~

   and change the color scale range for the target parameter in the chosen dataset.
5. Reopen Godiva2 on the dataset and plot the target parameter.
   Check the new default color scale range.

### Styling features and non-standard requests

ncWMS provides several styling and displaying capabilities that are also available in TDS:

* The `WMS` tries to identify vector components that it can combine and display as a single vector layer.
   It looks for CF standard_name attributes with values of the form `eastward_*` and `northward_*` and combines those that match into a vector layer.
* ncWMS provides several vector styles: barb, stumpvec, trivec, linevec, fancyvec.Demo
* Some styling properties can be specified through the non-standard optional parameters supported by ncWMS

* Some non-standard requests are supported by ncWMS:

  1. GetTransect
  2. GetVerticalProfile
  3. GetVerticalSection

### Exercise: `WMS` request with styling parameters

1. Open the non-standard optional parameters supported by ncWMS page
2. Make several `WMS` request changing the color scale range and the displaying properties for the values out of range.
3. Use this as base request.


### Using `WCS` and WMS

#### Various `WCS` and `WMS` Clients

* [GoogleEarth](https://www.google.com/earth/){:target="_blank"} (WMS) [free]
* Godiva3 (WMS) [free - distributed with TDS]
* [NASA WorldWind](https://worldwind.arc.nasa.gov){:target="_blank"} (WMS) [free]
* [IDV](https://www.unidata.ucar.edu/software/idv/){:target="_blank"} (WMS) [free]
* [ToolsUI](http://www.unidata.ucar.edu/downloads/netcdf/netcdf-java-4/index.jsp){:target="_blank"} (WMS) [free]
* [OWSlib](http://geopython.github.io/OWSLib/){:target="_blank"} (WMS and WCS) [free]
* [Map Express](https://www.cadcorp.com/products/free-mapping-software/){:target="_blank"} (`WMS` and `WCS`) [commercial / free]
* [IDL](http://www.harrisgeospatial.com/ProductsandSolutions/GeospatialProducts/IDL.aspx){:target="_blank"} (WMS) [commercial]
* [gvSIG](http://www.gvsig.org/web/){:target="_blank"} (WMS and WCS) [free]

#### Godiva2 `WMS` Client

The Godiva2 `WMS` client is part of the ncWMS code base and as such is included in the TDS distribution.
It is a web application written in JavaScript using the OpenLayers library.

In the TDS, you can access the Godiva2 client from the `Viewers` section of all `WMS` accessible datasets.
The Godiva2 User Guide is available from the ncWMS site.

{% include image.html file="tds/tutorial/tds_configuration/Godiva2_screenshot.png" alt="Godiva2" caption="" %}

### OWSLib (python client) example:

[tds-python-workshop `WMS` notebook](http://nbviewer.jupyter.org/github/Unidata/unidata-python-workshop/blob/master/notebooks/wms_sample.ipynb){:target="_blank"}
