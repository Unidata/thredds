---
title: Default TDS Configuration Catalog
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: /default_config_catalog.html
---

## Default TDS Root Catalog

The main TDS configuration catalog is at <tds.content.root.path>/thredds/catalog.xml.
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
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/thredds/dap4/" />
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/" />
    <!--service name="wcs" serviceType="WCS" base="/thredds/wcs/" /-->
    <!--service name="wms" serviceType="WMS" base="/thredds/wms/" /-->
    <!--service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/" /-->
  </service>
  
  <service name="dap" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/thredds/dap4/" />
  </service>
  
  <datasetRoot path="test" location="content/testdata/" />
  
  <dataset name="Test Single Dataset" ID="testDataset"
      serviceName="dap"  urlPath="test/testData.nc" dataType="Grid"/>
  
  <dataset name="Test Single Dataset 2" ID="testDataset2"
      serviceName="odap" urlPath="test/testData2.grib2" dataType="Grid"/>

  <datasetScan name="Test all files in a directory" ID="testDatasetScan"
      path="testAll" location="content/testdata">
    <metadata inherited="true">
      <serviceName>all</serviceName>
      <dataType>Grid</dataType>
    </metadata>
    
    <filter>
      <include wildcard="*eta_211.nc"/>
      <include wildcard="testgrid*.nc"/>
    </filter>
  </datasetScan>
  
  <catalogRef xlink:title="Test Enhanced Catalog" xlink:href="enhancedCatalog.xml" name=""/>
</catalog>
~~~

When the TDS starts, this root configuration catalog is read, as are all catalogs in the catalog tree defined by catalogRef elements.
The resulting tree of catalogs are used as the top-level catalogs served by the TDS.
In the case of our test catalog, the tree looks like:

~~~
catalog.xml
    |
    |-- enhancedCatalog.xml
~~~

The tree of configuration catalogs can be as deeply nested as desired.


## Additional Root Catalogs

Additional root configuration catalogs can be defined in the `<tds.content.root.path>/thredds/threddsConfig.xml` file. 
For instance, to add a test catalog add the following line to `threddsConfig.xml`:

<catalogRoot>myTestCatalog.xml</catalogRoot>

Each additional root configuration catalog can be the root of another tree of configuration catalogs.
