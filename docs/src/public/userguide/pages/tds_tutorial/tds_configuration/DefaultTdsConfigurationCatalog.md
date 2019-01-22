---
title: Default TDS Configuration Catalog
last_updated: 2018-10-22
sidebar: tdsTutorial_sidebar
toc: false
permalink: default_config_catalog.html
---

## Default TDS Root Catalog

The main TDS configuration catalog is at `${tds.content.root.path}/thredds/catalog.xml`.
We ship a simple test catalog:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog name="THREDDS Server Default Catalog : You must change this to fit your server!"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
           http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">
  
  <service name="all" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>
    <service name="dap4" serviceType="DAP4" base="/thredds/dap4/"/>
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/"/>
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/"/>
    <service name="wms" serviceType="WMS" base="/thredds/wms/"/>
    <service name="ncssGrid" serviceType="NetcdfSubset" base="/thredds/ncss/grid/"/>
    <service name="ncssPoint" serviceType="NetcdfSubset" base="/thredds/ncss/point/"/>
    <service name="cdmremote" serviceType="CdmRemote" base="/thredds/cdmremote/"/>
    <service name="cdmrFeature" serviceType="CdmrFeature" base="/thredds/cdmrfeature/grid/"/>
    <service name="iso" serviceType="ISO" base="/thredds/iso/"/>
    <service name="ncml" serviceType="NCML" base="/thredds/ncml/"/>
    <service name="uddc" serviceType="UDDC" base="/thredds/uddc/"/>
  </service>

  <datasetRoot path="test" location="content/testdata/" />

  <dataset name="Test Grid Dataset" ID="testGrid"
           serviceName="all"  urlPath="test/crossSeamProjection.nc" dataType="Grid"/>

  <dataset name="Test Point Dataset" ID="testPoint"
           serviceName="all" urlPath="test/H.1.1.nc" dataType="Point"/>
  
  <dataset name="Test Station Dataset" ID="testStation"
           serviceName="all" urlPath="test/H.2.1.1.nc" dataType="Point"/>

  <datasetScan name="Test all files in a directory" ID="testDatasetScan"
               path="testAll" location="content/testdata">
    <metadata inherited="true">
      <serviceName>all</serviceName>
    </metadata>
  </datasetScan>

  <catalogRef xlink:title="Test Enhanced Catalog" xlink:href="enhancedCatalog.xml" name=""/>
</catalog>
~~~

When the TDS starts, this root configuration catalog is read, as are all catalogs in the catalog tree defined by catalogRef elements.
The resulting tree of catalogs are used as the top-level catalogs served by the TDS.
The main root catalog, `${tds.content.root.path}/thredds/catalog.xml`,  can be accessed at

`www.server.com/thredds/catalog/catalog.xml`

In the case of our test catalog, the tree looks like:

~~~
catalog.xml
    |
    |-- enhancedCatalog.xml
~~~

The nested catalog, `enhancedCatalog.xml`, is exposed to end users in a `<catalogRef>` element in the client catalog:

~~~xml
<catalogRef xlink:title="Test Enhanced Catalog" name="Test Enhanced Catalog" xlink:href="enhancedCatalog.xml"/>
~~~

and can be accessed at the provided by the `xlink:href`, which in this case is:

`www.server.com/thredds/catalog/enhancedCatalog.xml`

The tree of configuration catalogs can be as deeply nested as desired.

## Additional Root Catalogs

Additional root configuration catalogs can be defined in the `<tds.content.root.path>/thredds/threddsConfig.xml` file. 
For instance, to add a test catalog add the following line to `threddsConfig.xml`:

`<catalogRoot>myTestCatalog.xml</catalogRoot>`

Each additional root configuration catalog can be the root of another tree of configuration catalogs.
To access the new root as an end user, you would visit: 

`www.server.com/thredds/catalog/myTestCatalog.xml`