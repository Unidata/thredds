---
title: Basics of Configuration Catalogs
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: /basic_config_catalog.html
---

## Overview

TDS configuration catalogs are like THREDDS [Client catalogs](/basic_client_catalog.html) with extensions.
They contain information detailing the datasets the TDS will serve and what services will be available for each dataset.
For example,  The `datasetRoot` and `datasetScan` elements are extensions that:
 * provide mappings between incoming URL requests and directories on disk; and
 * are used in the detailing of the datasets the TDS will serve.

Available services are indicated in the normal THREDDS client catalog manner with service name references.

The TDS configuration catalogs represent the top-level catalogs served by the TDS.
The configuration information is only needed by the server,  and the client view of the catalogs do not contain any server specific configuration information.

Note that the two extensions, `datasetRoot` and `datasetScan`, are only two examples of the many configuration extensions.
In this section, we will explore how these two simple elements can be used to serve out simple datafile and datasets.

## `datasetRoot` Element

Each datasetRoot element defines a single mapping between a URL base path and a directory.
The URL base path so defined can then be used in accessible datasets for files under the mapped directory.
For instance, if you have a directory `/machine/tds/data/my/test` that contains:

~~~bash
afile.nc
testData.nc
junk.nc
model_output/
  data1.nc4
  data2.nc4
~~~

You can serve the `testData.nc file with the following:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />

<datasetRoot path="my/test" location="/machine/tds/data/my/test" />             <!-- 1 -->

<dataset name="A Test Dataset" ID="testDataset" urlPath="my/test/testData.nc" > <!-- 2 -->
  <serviceName>odap</serviceName>
</dataset>

<dataset name="A Test Dataset 2" ID="testDataset2" urlPath="my/test/model_output/data1.nc4" > <!-- 3 -->
  <serviceName>odap</serviceName>
</dataset>
~~~

The datasetRoot element above (1) maps the "my/test" path to the "/machine/tds/data/my/test/" directory.
The URLs to access the datasets (2 & 3) are

~~~
http://hostname:port/thredds/dodsC/my/test/testData.nc
http://hostname:port/thredds/dodsC/my/test/grib/data1.nc4
~~~

When the server receives a request for one of the above URLs, it uses the URL path to look for a matching dataset root.
In this case it finds the mapping provided by the `datasetRoot` element above and looks in the `/machine/tds/data/my/test` directory for the file.

The client catalog that results from this catalog is the same as the above without the `datasetRoot` element.

## `datasetScan` Element

Each `datasetScan` element also defines a single mapping between a URL base path and a directory.
Unlike the `datasetRoot` element which works with dataset elements to define the datasets served, the `datasetScan` element will automatically serve some or all of the datasets found in the mapped directory.
So, all the files in the above listing could be served with the following:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />

<datasetScan name="Test all files in a directory" ID="testDatasetScan"
    path="my/test/all" location="/machine/tds/data/my/test">
  <metadata inherited="true">
    <serviceName>odap</serviceName>
  </metadata>
</datasetScan>
~~~

In the client view of a configuration catalog, `datasetScan` elements are converted to `catalogRef` elements.
So, the resulting client view of this catalog looks like:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />

<catalogRef xlink:title="Test all files in a directory" ID="testDatasetScan"
    xlink:href="/thredds/catalog/my/test/all/catalog.xml" name="" />
~~~

The generation of the catalog referenced by the catalogRef element is deferred until a request is made for that catalog.
When the catalog is requested the location directory is scanned, directories are represented as `catalogRef` elements and files are represented as dataset elements.
The scanning of each subdirectory is deferred until a request is made for the corresponding catalog.
The catalog referenced above would look like:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />

<dataset name="Test all files in a directory" ID="testDatasetScan" >
  <metadata inherited="true">
          <serviceName>odap</serviceName>
  </metadata>

  <dataset name="afile.nc" ID="testDatasetScan/afile.nc" urlPath="my/test/all/afile.nc" />
  <dataset name="testData.nc" ID="testDatasetScan/testData.nc"
      urlPath="my/test/all/testData.nc" />
  <dataset name="junk.nc" ID="testDatasetScan/junk.nc" urlPath="my/test/all/junk.nc" />

  <catalogRef xlink:title="grib" ID="testDatasetScan/model_output" name=""
      xlink:href="/thredds/catalog/my/test/all/model_output/catalog.xml" />
</dataset>
~~~

Note: The `datasetScan` element provides ways for limiting the datasets that are included in the scan, changing the names of datasets, sorting datasets, etc.
We will go into more detail on this later.

## Exercise: Add NCEP NAM model data

Modify the main TDS configuration catalog to include some model data.

1. Take a look at the data (note: the filenames may be different):

   ~~~bash
   $ ls /machine/tds/data
   fc  gfs  grib  my  nam_12km  ncmlExamples  ncss  ocean  precip  sage
   $ ls /machine/tds/data/nam_12km/
   NAM_CONUS_12km_20141010_0000.nc4
   NAM_CONUS_12km_20141010_0600.nc4
   ~~~

2. Edit the main TDS configuration catalog, using the editor of your choice (here we use `vim`):

   ~~~bash
   $ cd <tds.content.root.path>/thredds
   $ vim catalog.xml
   ~~~

3. add a `datasetScan` element for the NAM data:
   ~~~xml
   <datasetScan name="NCEP NAM 12km" ID="NAM_12km"
                path="nam_12km" location="/machine/tds/data/nam_12km">
     <metadata inherited="true">
       <serviceName>odap</serviceName>
     </metadata>
   </datasetScan>
   ~~~
4. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

5. Test that the new datasetScan is working:

   1. Bring the catalog up in a browser: <http://localhost:8080/thredds/catalog.html>{:target="_blank"}
   2. Click down to one of the NAM dataset pages.
   3. Select the OPeNDAP link

Not working?

1. Take a look at the catalogInit.log:

   ~~~bash
   $ cd <tds.content.root.path>/thredds/logs
   $ more catalogInit.log
   ~~~

2. Take a look at the threddsServlet.log:

   ~~~bash
   $ cd <tds.content.root.path>/thredds/logs
   $ more threddsServlet.log
   ~~~
