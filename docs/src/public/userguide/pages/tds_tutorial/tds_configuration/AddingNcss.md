---
title: Adding the NetCDF Subset Service
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: /adding_ncss.html
---

The NetCDF Subset Service (`NCSS`) is one of the ways that the TDS can serve data.
It is a experimental REST protocol for returning subsets of CDM datasets.
We want to eventually serve all CDM-compatible datasets through NCSS, but right now there are some restrictions on the types of datasets that are supported.

This documentation is for TDS administrators.
If you are a client wanting to access data through the Netcdf Subset Service, look at NetCDF Subset Service Reference.

## Enabling the Netcdf Subset Service in the TDS

The netCDF Subset Service must be enabled in the `threddsConfig.xml` configuration file before it can be used.
This is done by adding an allow element to the `NetcdfSubsetService` element as follows:

~~~xml
<NetcdfSubsetService>
    <allow>true</allow>
</NetcdfSubsetService>
~~~

Note: Details on other configuration options for NCSS are available in the `threddsConfig.xml` docs.

## Serving Datasets with the Netcdf Subset Service

In your configuration catalogs, you must define the service like this:

~~~xml
<service name="subsetServer" serviceType="NetcdfSubset" base="/thredds/ncss/" />
~~~

Then as usual, add the service to any datasets that you want served, eg:

~~~xml
<dataset name="datasetName" ID="datasetID" urlPath="/my/urlPath"> 
   <serviceName>subsetServer</serviceName> 
</dataset> 
~~~

Note that the name of the service ( subsetServer in this example) is arbitrary, but the serviceType and base must be _exactly_ as shown.

## Restrictions on what files can be served

First, only datasets in the format that the CDM can read are supported. Second, the data must represent one of the following Feature Types: `GRID`, `POINT`, `STATION`.

## Verifying that a dataset is gridded

Open your file in the `ToolsUI` program, using the `FeatureTypes->Grids` tab.
Any fields identified as grids will show up in the top table.
To be sure, go into the viewer (click ![redraw](images/tds/tutorial/tds_configuration/redraw.gif) to bring up the Viewer, then click ![redraw](images/tds/tutorial/tds_configuration/redraw.gif) again to show the data) and make sure the data is displayed correctly.
If all that works, then the data should be served correctly by the TDS.

## Verifying that a dataset is "pointed"

Open your file in the `ToolsUI` program using the `FeatureTypes->PointFeature` tab.
In the top table, verify that "featureType" is either "POINT" or "STATION".
