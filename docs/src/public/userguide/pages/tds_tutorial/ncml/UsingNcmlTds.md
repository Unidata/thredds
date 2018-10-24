---
title: Using NcML in the TDS 
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: using_ncml_in_the_tds.html
---

## NCML and the TDS

An NcML document is an XML document that uses the NetCDF Markup Language to define a CDM dataset.
NcML can be embedded directly into the TDS catalogs to achieve a number of powerful features, shown below.
This embedded NcML is only useful in the TDS server catalogs, it is not meaningful to a THREDDS client, and so is not included in the client catalogs.
One can put an NcML element inside a dataset element, in which case it is a self-contained NcML dataset, or inside a datasetScan element, where it modifies a regular dataset.
In both cases, we call the result a virtual dataset, and you cannot serve a virtual dataset with a file-serving protocol like FTP or HTTP.
You can use subsetting services like OPeNDAP, WCS, WMS and NetcdfSubset.

## Using NcML in a dataset element

NcML embedded in a TDS dataset element creates a self-contained NcML dataset.
The TDS dataset does not refer to a data root, because the NcML contains its own location.
The TDS dataset must have a unique URL path (this is true for all TDS datasets), but unlike a regular dataset, does not have to match a data root.

### Modifying an existing dataset

You can use use NcML to modify an existing CDM dataset:

~~~xml
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="TDS workshop test 1" version="1.0.2">
  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>      <!-- 1 -->

  <dataset name="Example NcML Modified" ID="ExampleNcML-Modified" 
             urlPath="ExampleNcML/Modified.nc">                              <!-- 2 -->
    <serviceName>ncdods</serviceName>
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" 
            location="/machine/tds/workshop/ncml/example1.nc">               <!-- 3 -->
      <variable name="Temperature" orgName="T"/>                             <!-- 4 -->
      <variable name="ReletiveHumidity" orgName="rh">                        <!-- 5 -->
        <attribute name="long_name" value="relatively humid"/>               <!-- 6 -->
        <attribute name="units" value="percent (%)"/>
        <remove type="attribute" name="description"/>                        <!-- 7 -->
      </variable >
    </netcdf>
  </dataset>
</catalog>
~~~

1. A `service` is defined that allows the virtual dataset to be served through `OPENDAP`. 
   Make sure that the `base` attribute is exactly as shown.
2. The virtual dataset is created and given a `urlPath` of `ExampleNcML/Modified.nc`. 
   The `urlPath` is essentially arbitrary, but must be **unique** within the TDS, and you should maintain a consistent naming convention to ensure uniqueness, especially for large collections of data.
   Its important to also give the dataset a unique `ID`.
3. An NcML dataset is defined which references the netCDF file at the absolute location (example: `/machine/tds/workshop/ncml/example1.nc`).
   Note that you must declare the NcML namespace exactly as shown.
4. The variable named `T` in the original file is renamed `Temperature`.
5. The variable named `rh` in the original file is renamed `RelativeHumidity`.
6. Two attributes of `rh` are defined, `long_name` and `units`. 
   If these already exist, they are replaced.
7. The attribute of `rh` called _description_ is removed.

### Dataset vs virtual dataset

Lets look at serving a file directly vs serving it through NcML:

~~~xml
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="TDS workshop test 2" version="1.0.2">

  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
  <datasetRoot path="test/ExampleNcML" location="/machine/tds/workshop/ncml/" />       <!-- 1 -->
  <dataset name="Example Dataset" ID="Example" urlPath="test/ExampleNcML/example1.nc"> <!-- 2 -->
    <serviceName>ncdods</serviceName>
  </dataset>
  <dataset name="Example NcML Modified" ID="Modified" urlPath="ExampleNcML/Modified.nc">
    <serviceName>ncdods</serviceName>                      <!-- 3 -->
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" 
        location="/machine/tds/workshop/ncml/example1.nc"> <!-- 4 -->
      <variable name="Temperature" orgName="T"/>
    </netcdf>
  </dataset>
</catalog>
~~~

1. A `datasetRoot` is defined that associates URL path `test/ExampleNcML` with the disk location `/data/nc/`.
2. The dataset is created with a `urlPath` of `test/ExampleNcML/example.nc`.
   The first part of the path is matched to the `datasetRoot`, so that the full dataset location is `/data/nc/example1.nc`.
   This file is served directly by this dataset element. 
3. The same file is used in a virtual dataset defined by the embedded NcML.
   The virtual dataset is given an (arbitrary) `urlPath` of `ExampleNcML/Modified.nc`. 
4. The NcML element is defined which references the netCDF file at the absolute location `/data/nc/example1.nc`.
   The only modification is to rename the variable `T` to `Temperature`. 

### Using NcML aggregation

Here is an example that defines a dataset using NcML aggregation.

~~~xml
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="TDS workshop test 3" version="1.0.2">

  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/" />              <!-- 1 -->
  <dataset name="Example NcML Agg" ID="ExampleNcML-Agg" urlPath="ExampleNcML/Agg.nc"> <!-- 2 -->
    <serviceName>ncdods</serviceName>                                                 <!-- 3 -->
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 4 -->
      <aggregation dimName="time" type="joinExisting">                      <!-- 5 -->
        <scan location="/machine/tds/workshop/ncml/cg/" dateFormatMark="CG#yyyyDDD_HHmmss"
              suffix=".nc" subdirs="false"/>                                <!-- 6 -->
      </aggregation>
    </netcdf>
  </dataset>
</catalog>
~~~


1. An OPENDAP `service` is defined called ncdods.
2. A THREDDS `dataset` is defined, which must have a `urlPath` that is unique within the TDS, in this case ExampleNcML/Agg.nc.
3. The dataset uses the ncdods service.
4. An NcML `netcdf` element is embedded inside the THREDDS `dataset` element.
5. An NcML aggregation of type `joinExisting` is declared, using the existing time dimension as the aggregation dimension.
6. All the files in the directory `/machine/tds/workshop/ncml/cg/` that end with .nc will be scanned to create the aggregation.
   A `dateFormatMark` is used to define the time coordinates, indicating there is exactly one time coordinate in each file.

## Using NcML in a datasetScan element

If an NcML element is added to a `DatasetScan`, it will modify all of the datasets contained within the `DatasetScan`.
It is not self-contained, however, since it gets its location from the datasets that are dynamically scanned.

~~~xml
<datasetScan name="Ocean Satellite Data" ID="ocean/sat" path="ocean/sat" location="/machine/tds/workshop/ncml/ocean/"> <!-- 1 -->
  <filter>
    <include wildcard="*.nc" />
  </filter>
  <metadata inherited="true"> <!-- 2 -->
    <serviceName>ncdods</serviceName>
    <dataType>Grid</dataType>
  </metadata>
  <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 3 -->
    <attribute name="Conventions" value="CF-1.0"/>
  </netcdf>
</datasetScan>
~~~

1. A `datasetScan` element is created whose contained datasets start with URL path `ocean/sat`, and whose contents are all the files in the directory `/machine/tds/workshop/ncml/ocean/` which end in `.nc`.
2. All contained datasets inherit metadata indicating they use the ncdods service and are of type `Grid`.
3. All contained datasets are wrapped by this NcML element.
   In this case, each dataset has the global attribute `Conventions="CF-1.0"` added to it.
   Note that there is no location attribute, which is implicitly supplied by the datasets found by the`datasetScan`.

### DatasetScan versus Aggregation Scan

The `scan` element in the NcML aggregation is similar in purpose to the `datasetScan` element, but be careful not to confuse the two.
The `datasetScan` element is more powerful, and has more options for filtering, etc.
Its job is to create nested `dataset` elements inside the `datasetScan`, and so has various options to add information to those nested datasets.
It has a generalized framework (CrawlableDataset) for crawling other things besides file directories.
The `scan` element’s job is to easily specify what files go into an NcML aggregation, and those individual files are hidden inside the aggregation dataset.
It can only scan file directories.
In the future, some of the capabilities of `datasetScan` will migrate into NcML scan.

### Exercise: DatasetScan versus Aggregation Scan

Let\'s look at using a `datasetScan` and an Aggregation `scan` on the same collection of files.
Download  {% include link_file.html file="tds_tutorial/ncml/ncmlTds/catalogScan.xml" text="catalogScan.xml" %}, place it in `${tomcat_home}/content/thredds` directory and add a `catalogRef` to it from your main catalog.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
     xmlns:xlink="http://www.w3.org/1999/xlink"
     name="TDS workshop test 4" version="1.0.2">

  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
  <dataset name="Example NcML Agg" ID="ExampleNcML-Agg" urlPath="ExampleNcML/Agg.nc"> <!-- 1 -->
    <serviceName>ncdods</serviceName>
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">           <!-- 2 -->
      <aggregation dimName="time" type="joinExisting" recheckEvery="4 sec">
        <scan location="/machine/tds/workshop/ncml/cg/" dateFormatMark="CG#yyyyDDD_HHmmss" suffix=".nc" subdirs="false"/>
      </aggregation>
    </netcdf>
  </dataset>

  <datasetScan name="CG Data" ID="cg/files" path="cg/files" location="/machine/tds/workshop/ncml/cg/"> <!-- 3 -->
    <metadata inherited="true">
      <serviceName>ncdods</serviceName>
      <dataType>Grid</dataType>
    </metadata>
    <filter>
      <include wildcard="*.nc"/>     <!-- 4 -->
    </filter>
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 5 -->
      <attribute name="Yoyo" value="Ma"/>
    </netcdf>
  </datasetScan>
</catalog>
~~~

1. A virtual dataset is defined with URL `ExampleNcML/Agg.nc`
2. The NcML aggregation for this dataset. Remember that the recheckEvery attribute only applies when using a scan element.
3. A datasetScan element is created whose contained datasets start with URL path `cg/files`, and which scans the directory `/workshop/test/cg/`
4. Only files which end in .nc.
5. Add a global attribute to each file in the collection.

Start or restart your TDS and look at those datasets through the HTML interface and through ToolsUI.

## Using NcML in a featureCollection element

Here we show a brief example of modifying files with NcML in a `featureCollection` element.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="Unidata THREDDS Data Server" version="1.0.3">

  <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>

  <featureCollection featureType="FMRC" name="GOMOOS" harvest="true" path="fmrc/USGS/GOMOOS">
    <metadata inherited="true">
      <serviceName>ncdods</serviceName>
      <dataFormat>netCDF</dataFormat>
      <documentation type="summary">Munge this with NcML</documentation>
    </metadata>

    <collection spec="/machine/tds/workshop/ncml/gomoos/gomoos.#yyyyMMdd#.cdf$"/>
    <protoDataset>  <!-- 1 -->
      <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
        <attribute name="History" value="Processed by Kraft"/>
      </netcdf>
    </protoDataset>

    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 2 -->
      <variable name="time">
        <attribute name="units" value="days since 2006-11-01 00:00 UTC"/>
      </variable>
      <attribute name="Conventions" value="CF-1.0"/>
    </netcdf>
  </featureCollection>
</catalog>
~~~

1. The `protoDataset` is modified by adding a global attribute `History="Processed by Kraft"`.
2. Each component file is changed by modifying the time variable\'s `units` attribute and adding a global attribute `Conventions="CF-1.0"`

You might wonder why not put the global attribute `Conventions="CF-1.0"` on the protoDataset instead of on each individual dataset?
The reason is because in an FMRC, each dataset is converted into a `GridDataset`, and then combined into the FMRC.
So the modifications in 2) are whats needed to make the individual datasets be correctly interpreted as a `Grid` dataset.
The modifications to the `protoDataset` are then applied to the resulting FMRC 2D dataset.

## Debugging NcML

When things go wrong, it\'s best to first debug the aggregation outside of the TDS.

Go to the TDS catalog and find the problem dataset. 
Inside the `<dataset>` element will be a `<netcdf>` element, that is the NcML aggregation.
Extract it out and put it in a file called `test.ncml`.

1. Add the XML header to the top of it: `<?xml version="1.0"encoding="UTF-8"?>`
2. Remove the recheckEvery attribute if present on the `<scan>` element.
3. Make sure that the `<scan>` location is available on the machine you are running ToolsUI

Now start up ToolsUI, and in the viewer tab, navigate to `test.ncml` and try to open it.

If the dataset is dynamic (files can be added or deleted), add the `recheckEvery` attribute on the `scan` element and open the dataset, then reopen after a new file has arrived (and `recheckEvery` time has passed).
Generally you make `recheckEvery` very short while testing.

Now add the NcML dataset back to the TDS, without a `recheckEvery` attribute on the scan element.
See if OPeNDAP access works.

Add the `recheckEvery` attribute (if needed) and test again.

{%include troubleshooting.html content="
Remember that you **can’t** use `HTTPServer` for NcML datasets, as they are virtual datasets.
Use only the subsetting services, such as `OpenDAP`, `WCS`, `WMS`, and `NetcdfSubset`.
" %}