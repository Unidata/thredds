---
title: Configuration Catalogs
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: config_catalog.html
---

## The service Element

### Compound service Elements - Serving Datasets with Multiple Methods

Datasets can be made available through more than one access method by defining and then referencing a `compound` service element.
The following:

~~~xml
<service name="all" base="" serviceType="Compound" >
    <service name="odap" serviceType="OPeNDAP" base="/thredds/dodsC/" />
    <service name="http" serviceType="FileServer" base="/thredds/fileServer/" />
</service>
~~~

defines a compound service named \"all\" which contains two nested services.
Any dataset that reference the compound service will have two access methods.
For instance:

~~~xml
<datasetRoot path="precip" location="/machine/tds/data/precip" />

<dataset name="Precip Data" ID="precipdata" urlPath="precip/nws_precip_conus_20130909.nc"
    dataType="Grid" serviceName="all" />
~~~

would result in these two access URLs:

~~~bash
/thredds/dodsC/precip/nws_precip_conus_20130909.nc
/thredds/fileServer/precip/nws_precip_conus_20130909.nc
~~~

Note: The contained services can still be referenced independently.
For instance:

~~~bash
<dataset name="More Precip Data" ID="precipdata2" urlPath="precip/nws_precip_conus_20130910.nc"
    dataType="Grid" serviceName="odap" />
~~~
results in a single access URL:

`/thredds/dodsC/precip/nws_precip_conus_20130910.nc`

Using compound services allow maintaining groups of services across datasets, rather than needing to maintain individual lists with each dataset.
For instance, we can add the DAP4 service to the above all service:

~~~xml
<service name="all" base="" serviceType="Compound" >
    <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/thredds/dap4/" />
    <service name="http" serviceType="FileServer" base="/thredds/fileServer/" />
</service>
~~~

This would then add an additional access URL for DAP4:

`/thredds/dap4/precip/nws_precip_conus_20130909.nc`

### Unique Service Names


* Service names do not have to be unique globally within a TDS, only on a catalog by catalog basis.
* Duplicate service names do not adversely affect the TDS.
  However, clients reading a catalog with duplicate service names may get confused if a dataset references that service name.
* `<service>` elements may defined in the top level catalog and referenced globally. 

#### Why duplicate names are a bad idea

Service names are used by datasets to reference the service element that represents the available service(s) for that dataset.
So that the service reference resolves to a unique service element, all service names within a given catalog must be unique.

Here's an example:

~~~xml
<service name="any" serviceType="Compound" base="" >
    <service name="service1" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="service2" serviceType="HTTPServer" base="/thredds/fileServer/" />
</service>
<service name="grid" serviceType="Compound" base="" >
    <service name="service1" serviceType="OpenDAP" base="/thredds/dodsC/" />
    <service name="service2" serviceType="WCS" base="/thredds/wcs/" />
    <service name="service3" serviceType="WMS" base="/thredds/wms/" />
    <service name="service4" serviceType="HTTPServer" base="/thredds/fileServer/" />
</service>
~~~

Notes:
* A dataset that references `service1` will be fine. 
  But only because both `service1` instances have the same serviceType and base URL.
* A dataset that references `service2` may find either the HTTP file service or the WCS service.

## TDS Requirements for the service Elements

The TDS provides data access services at predefined URL base paths.
Therefore, it is required that service base URLs must [exactly match the values given here](services_ref.html#tdsServiceElemRequirements) according to service type, for example:

* OPeNDAP

  `<service name="odap" serviceType="OPeNDAP" base="/thredds/dodsC/" />`

* NetCDF Subset Service (grid)

  `<service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/grid/" />`

* NetCDF Subset Service (point)

  `<service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/point/" />`

* WCS

  `<service name="wcs" serviceType="WCS" base="/thredds/wcs/" />`

* WMS

  `<service name="wms" serviceType="WMS" base="/thredds/wms/" />`

* HTTP Bulk File Service

  `<service name="fileServer" serviceType="HTTPServer" base="/thredds/fileServer/" />`

Notes: These base URLs are relative to the server so your catalogs are independent of your servers hostname or port.

### Data Type Requirement for Each Service

* The `HTTPServer` service can serve any file.
* The `OPeNDAP` service can serve any data file that the netCDF-Java library can open.
* The `WCS` service can only serve data files that the netCDF-Java library can recognize as \"gridded\" data.
* The `WMS` service also only serves \"gridded\" data files.
* The `NetcdfSubset` service serves \"gridded\" and \"point\" (station and timeseries) data files.

You can check that a data file is recognized as \"gridded\" with netCDF-Java ToolsUI (available for download from the netCDF-Java home page or you can use webstart).

### Exercise: Check that the NAM Dataset is Gridded Data

* Run the netCDF-Java ToolsUI application.
* In the `"FeatureTypes" - "Grids"` tab, browse to the `<path-to-workshop-data>/data/basic_catalog/nam_12km` directory and open a dataset file.
* If variables are listed in the top section of the window, the netCDF-Java library has recognized the dataset as gridded data.

## Standard Services

If the `dataType` of the dataset is know (that is, set in the catalog metadata), then there is no need to specify a service element for the dataset.
The TDS defines a set of Standard Services, based on `dataType`.

### Exercise: use Standard Services for the NAM dataset

1. Edit the main TDS configuration catalog, using the editor of your choice (here we use `vim`):

   ~~~bash
   $ cd <tds.content.root.path>/thredds
   $ vim catalog.xml
   ~~~

2. edit the NAM 12km  `datasetScan` element and replace the `serviceName` element with a `dataType` element:
   ~~~xml
   <datasetScan name="NCEP NAM 12km" ID="NAM_12km"
                path="nam_12km" location="<path-to-workshop-data>/data/basic_catalog/nam_12km">
    <metadata inherited="true">
       <dataType>GRID</dataType>
     </metadata>
   </datasetScan>
   ~~~
3. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

4. Examine the new set of services:

   1. Bring the catalog up in a browser: <http://localhost:8080/thredds/catalog.html>{:target="_blank"}
   2. Click down to one of the NAM dataset pages.
   3. How many new access methods are there as compared to the single OPeNDAP access method from before?

{%include ahead.html content="
The `dataType` of a `featureCollection` is always known, so there is no need to explicitly define the services for these more advanced catalog configurations.
" %}

## THREDDS Metadata

### Linking to Metadata

~~~xml
<metadata xlink:title="some good metadata" xlink:href="http://my.server/md/data1.xml" />
~~~

### Linking to Human Readable Metadata

~~~xml
<documentation xlink:title="My Data" xlink:href="http://my.server/md/data1.html" />
~~~

### Inherited Metadata

~~~xml
...
  <dataset name="Precip Set">

    <metadata inherited="true">
      <serviceName>all</serviceName>
      <description>Multi-sensor precipitation estimates</description>
      <keyword>Precipitation</keyword>
      <creator>
        <name>National Weather Service</name>
	    <contact url="http://water.weather.gov/precip/" email="AHPS.Precip@noaa.gov" />
      </creator>
      <dataType>Grid</dataType>
    </metadata>

    <dataset name="Precip Data" ID="precip1" urlPath="precip/nws_precip_conus_20130909.nc">
      <date type="created">2013-09-09</date>
    </dataset>

    <dataset name="More Precip Data" ID="precip2"   
             urlPath="precip/nws_precip_conus_20130910.nc">

      <metadata>
        <serviceName>odap</serviceName>
        <date type="created">2013-09-10</date>
      </metadata>

    </dataset>

  </dataset>
~~~

Notes:
1. Children datasets `Precip Data` and `More Precip Data` inherit the `dataType` element
2. `More Precip Data` datasets `serviceName` overwrites the inherited element, so only `odap` is used.

The `datasetScan` element is an extension of the `dataset` element and so can contain metadata.

~~~xml
      <datasetScan name="Precip Set" ID="precipset"
                   path="precip" location="/machine/tds/data/precip/">
          <metadata inherited="true">
              <serviceName>all</serviceName>
	          <documentation>Multi-sensor precipitation estimates</documentation>
              <keyword>Precipitation</keyword>
              <creator>
                <name>National Weather Service</name>
	            <contact url="http://water.weather.gov/precip/" 
                         email="AHPS.Precip@noaa.gov" />
              </creator>
              <dataType>Grid</dataType>
              <date type="created">2013</date>
          </metadata>
      </datasetScan>
~~~

The client view of the above `datasetScan` element will be a `catalogRef` element which will also contain any metadata contained in the `datasetScan` element.
It will look something like:

~~~xml
<catalogRef xlink:href="/thredds/catalog/precipscan/catalog.xml"
               xlink:title="Precip Set" ID="precipset" name="">
    <metadata inherited="true">
        ...
    </metadata>
</catalogRef >
~~~

All generated catalogs that are descendants of this `datasetScan` will contain all inherit-able (inherited="true") metadata contained in the `datasetScan` element.
For instance, given that the precip directory contained five files, the resulting child catalog will look like:

~~~xml
  <service name="all" serviceType="Compound" base="">
    <service name="odap" serviceType="OPENDAP" base="/thredds/dodsC/"/>
    <service name="dap4" serviceType="DAP4" base="/thredds/dap4/"/>
    <service name="http" serviceType="HTTPServer" base="/thredds/fileServer/"/>
  </service>
  <dataset name="Precip Set" ID="precipset">
    <metadata inherited="true">
      <serviceName>all</serviceName>
      <dataType>GRID</dataType>
      <documentation>Multi-sensor precipitation estimates</documentation>
      <creator>
        <name>National Weather Service</name>
        <contact url="http://water.weather.gov/precip/" email="AHPS.Precip@noaa.gov"/>
      </creator>
    <keyword>Precipitation</keyword>
    <date type="created">2013</date>
    </metadata>
    <dataset name="nws_precip_conus_20130913.nc" ID="precipset/nws_precip_conus_20130913.nc"
          urlPath="precipscan/nws_precip_conus_20130913.nc">
      <dataSize units="Mbytes">1.710</dataSize>
      <date type="modified">2014-10-16T16:19:53Z</date>
    </dataset>
    <dataset name="nws_precip_conus_20130912.nc" ID="precipset/nws_precip_conus_20130912.nc"
          urlPath="precipscan/nws_precip_conus_20130912.nc">
      <dataSize units="Mbytes">1.710</dataSize>
      <date type="modified">2014-10-16T16:19:53Z</date>
    </dataset>
    <dataset name="nws_precip_conus_20130911.nc" ID="precipset/nws_precip_conus_20130911.nc"
          urlPath="precipscan/nws_precip_conus_20130911.nc">
      <dataSize units="Mbytes">1.710</dataSize>
      <date type="modified">2014-10-16T16:19:53Z</date>
    </dataset>
    <dataset name="nws_precip_conus_20130910.nc" ID="precipset/nws_precip_conus_20130910.nc"
          urlPath="precipscan/nws_precip_conus_20130910.nc">
      <dataSize units="Mbytes">1.710</dataSize>
      <date type="modified">2014-10-16T16:19:53Z</date>
    </dataset>
    <dataset name="nws_precip_conus_20130909.nc" ID="precipset/nws_precip_conus_20130909.nc"
          urlPath="precipscan/nws_precip_conus_20130909.nc">
      <dataSize units="Mbytes">1.710</dataSize>
      <date type="modified">2014-10-16T16:19:53Z</date>
    </dataset>
  </dataset>
  ~~~

## Managing datasetRoot and datasetScan Elements

You can have as many `datasetRoot` and `datasetScan` elements as you want, for example:

~~~xml
<datasetRoot path="model" location="/data/ncep" />
<datasetRoot path="obs" location="/data/raw/metars" />
<datasetRoot path="cases/001" location="C:/casestudy/data/001" />
<datasetScan path="myData" location="/data/ncep/run0023" name="NCEP/RUN 23" serviceName="myserver" />
<datasetScan path="myData/gfs" location="/pub/ldm/gfs" name="NCEP/GFS" serviceName="myserver" />
~~~

The `datasetRoot` and `datasetScan` are said to define a **data root**.

### The Rules for Data Roots

* Each accessible dataset must be associated with a data root, i.e. the beginning part of its URL path must match a data root path.
  If there are multiple matches, the longest match is used.
* Each data root must have a unique `path` for all catalogs used by the TDS.
  Note: Because the TDS uses the set of all given path values to map URLs to datasets, each path value MUST be unique across all config catalogs on a given TDS installation. 
  Duplicates will cause warning messages in the `catalogInit.log` file.
* The directory pointed to by `location` should be absolute
* The locations may be used in multiple data roots

For example, using the above data roots, the following matches would be made:

| urlPath | file |
| model/run0023/mydata.nc | /data/ncep/run0023/mydata.nc |
| obs/test.nc | /data/raw/metars/test.nc |
| myData/mydata.nc | /data/ncep/run0023/mydata.nc |
| myData/gfs/mydata.nc | /pub/ldm/gfs/mydata.nc |
| cases/001/test/area/two | C:/casestudy/data/001/test/area/two |

The structure of a full OPeNDAP URL for the first urlPath above would look like:

~~~
http://hostname:port/thredds/dodsC/model/run0023/mydata.nc
|<---  server   --->|<----->|<--->|<--->|<-   filename  ->|
                        |      |     |
           webapp name -|      |     |- data root
                               |
                      service -|
~~~

where:

* `http://hostname:port` is the server\'s hostname and port. 
  By using relative service base URLs, you never have to specify this explicitly in your catalogs.
  This means you can change hosts or ports without having to rewrite your catalogs.
* `/thredds` is the name of the web application, taken from the `thredds.war` file.
* `/dodsC` maps to a servlet inside the web application, here it would be the OPeNDAP servlet.
* `/model` is the `path`, associated with the directory location `/data/ncep/`.
* `/run0023/mydata.nc` is the relative filename, and so is mapped to `/data/ncep/run0023/mydata.nc`.

## More datasetScan Element

### Including Only the Desired Files

A `datasetScan` element can specify which files and directories it will include with a filter element (see [spec](https://www.unidata.ucar.edu/software/thredds/current/tds/catalog/InvCatalogServerSpec.html#filter_Element){:target="_blank"} for more details).
When no filter element is given, all files and directories are included in the generated catalog(s).
Adding a filter element to your `datasetScan` element allows you to include (and/or exclude) the files and directories as desired.
The `datasetScan` element used in the `enhancedCatalog.xml` catalog shipped as a default catalog with the TDS included the following:

~~~xml
<filter>
  <include wildcard="*eta_211.nc" />
</filter>
~~~

To exclude the 00Z runs, the filter could be modified to:

~~~xml
<filter>
    <include wildcard="*eta_211.nc" />
    <exclude wildcard="*00_eta_211.nc" />
</filter>
~~~

The `include` and `exclude` elements both determine which datasets they match on whether their wildcard pattern (given by the `wildcard` attribute) or regular expression (given by the `regExp` attribute) match the dataset name.
By default, includes and excludes apply only to regular files (`atomic` datasets).
You can specify that they apply to directories (collection datasets) as well by using the `atomic` and `collection` attributes.
For example, if the your data directory contained a `badData/` directory, it could be excluded by adding the following to the filter:

~~~xml
<exclude wildcard="badData" atomic="false" collection="true" />
~~~

### Exercise - exclude the 12Z runs from the `enhancedCatalog.xml` catalog

1. Edit `$(tds.content.root.path)/thredds/enhancedCatalog.xml` and update the `<filter>` element as follows:

~~~xml
<filter>
    <include wildcard="*eta_211.nc" />
    <exclude wildcard="*12_eta_211.nc" />
</filter>
~~~

2. Restart Tomcat
3. Check the dataset in your [browser](http://localhost:8080/thredds/catalog/testEnhanced/catalog.html){:target="_blank"}

### Sorting Datasets

By default, datasets are listed in decreasing lexigraphic order by the dataset name.
A sort element can be added to a `datasetScan` element to specify an increasing lexigraphic order:

~~~xml
<sort>
    <lexigraphicByName increasing="true" />
</sort>
~~~

Currently, the lexigraphic increasing or decreasing sort algorithm is the only one supported.

### Dataset IDs

All generated datasets are given an `ID`.
The `ID`s are simply the path of the dataset appended to the `datasetScan` `path` value or, if one exists, the `ID` of the `datasetScan` element.
So, if we have the following configuration:

~~~
<datasetScan name="NCEP NAM 12km" path="NAM_12km" location="/machine/tds/data/nam_12km">
~~~

and for the data file `NAM_CONUS_12km_20141010_0000.grib2`, the value of the dataset `ID` would be

`NAM_12km/NAM_CONUS_12km_20141010_0000.grib2`.

### Naming Datasets

By default, all datasets are named with the name of their underlying file.
By adding a `namer` element, you can specify more human readable dataset names.
For instance, the following `namer` element causes any dataset named `NCEP NAM_12km` to be renamed with the value of `replaceString`:

~~~xml
<namer>
  <regExpOnName regExp="NCEP NAM 12km" replaceString="NCEP NAM 12km model output" />
</namer>
~~~

More complex renaming is possible as well.
The `namer` uses a [regular expression](https://www.regular-expressions.info/){:target="_blank"} match on the dataset name.
If the match succeeds, any regular expression [capturing groups](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html){:target="_blank"} are used in the replacement string.

A capturing group is a part of a regular expression enclosed in parenthesis.
When a regular expression with a capturing group is applied to a string, the substring that matches the capturing group is saved for later use.
The captured strings can then be substituted into another string in place of capturing group references, `$n`, where `n` is an integer indicating a particular capturing group. 
The capturing groups are numbered according to the order in which they appear in the match string.

For example, the regular expression `Hi (.*), how are (.*)?` when applied to the string "Hi \"Fred, how are you?\" would capture the strings \"Fred\" and \"you\".
Following that with a capturing group replacement in the string \"$2 are $1.\" would result in the string \"you are Fred.\"

Here's an example namer:

~~~xml
<namer>
  <regExpOnName regExp="nws_precip_conus_(\d{4})(\d{2})(\d{2}).nc"
                replaceString="NWS CONUS Precipitation for $2-$3-$1"/>
</namer>
~~~

the regular expression has 3 capturing groups

* The first capturing group, \"(\d{4})\", captures four digits, in this case the year.
* The second capturing group, \"(\d{2})\", captures two digits, in this case the month.
* The third capturing group, \"(\d{2})\", captures two digits, in this case the day of the month.

When applied to the dataset name \"nws_precip_conus_20130910.nc\", the strings \"2013\", \"09\", and \"10\" are captured.
After replacing the capturing group references in the replaceString attribute value, we get the name \"NWS CONUS Precipitation 2013-09-10\".

### Adding timeCoverage Elements

A `datasetScan` element may contain an `addTimeCoverage` element.
The `addTimeCoverage` element indicates that a `timeCoverage` metadata element should be added to each dataset in the collection and describes how to determine the time coverage for each datasets in the collection.

Currently, the `addTimeCoverage` element can only construct start/duration `timeCoverage` elements and uses the dataset name to determine the start time.
As described in the \"Naming Datasets\" section above, the `addTimeCoverage` element applies a regular expression match to the dataset name.
If the match succeeds, any regular expression capturing groups are used in the start time replacement string to build the start time string.
The values of the following attributes are used to determine the time coverage:

1. Either the `datasetNameMatchPattern` or the `datasetPathMatchPattern` attribute gives a regular expression used to match on the dataset `name` or `path`, respectively.
   If a match is found, a `timeCoverage` element is added to the dataset.
   The match pattern should include capturing groups which allow the match to save substrings from the dataset name.
2. The `startTimeSubstitutionPattern` attribute value has all capture group references (\"$n\") replaced by the corresponding substring that was captured during the match.
   The resulting string is used as the start value of the resulting `timeCoverage` element.
3. The `duration` attribute value is used as the duration value of the resulting `timeCoverage` element.

For instance, adding

~~~xml
<addTimeCoverage datasetNameMatchPattern="nws_precip_conus_(\d{4})(\d{2})(\d{2}).nc"
                 startTimeSubstitutionPattern="$1-$2-$3T00:00:00"
                 duration="24 hours" />
~~~

to a `datasetScan` element and given a data file named

`nws_precip_conus_20130910.nc`

results in the following `timeCoverage` element:

~~~
<timeCoverage>
    <start>2013-09-10T00:00:00</start>
    <duration>24 hours</duration>
</timeCoverage>
~~~

## Adding a Latest Proxy Datasets

With a real-time archive, it is convenient to define a \"proxy\" dataset that always points to the most recent dataset in a collection.
Other types of proxy datasets may be useful as well and the `addProxies` element provides a place for describing proxy datasets.
Currently, only two `addProxies` child elements are defined.
They are both \"Latest\" proxy elements.
The `simpleLatest` element adds a proxy dataset which proxies the existing dataset whose name is lexigraphically greatest (which finds the latest dataset assuming a timestamp is part of the dataset name).
The `latestComplete` element behaves similarly to `simpleLatest` except that the proxied dataset does not include any datasets that have been modified more recently than a given time limit, e.g., you could specify you want the most recent (lexigraphically) dataset that hasn\'t been modified for 60 minutes.
Both the `simpleLatest` and `latestComplete` elements must point to an existing service element.

To add a \"Latest\" dataset, we could add:

~~~xml
<service name="latest" serviceType="Resolver" base="" />
~~~

to our catalog and

~~~xml
<addProxies>
    <latestComplete name="latestComplete.xml" top="true" serviceName="latest" lastModifiedLimit="60" />
</addProxies>
~~~

to our `datasetScan` element.
This would result in the following dataset being at the top of the collection of datasets:

~~~xml
<dataset name="latestComplete.xml" serviceName="latest" urlPath="latestComplete.xml" />
~~~

The `latestComplete` element includes a `name` attribute which provides the name of the proxy dataset, the `serviceName` attribute that references the service used by the proxy dataset, the `top` attribute which indicates if the proxy dataset should appear at the top or bottom of the list of datasets in this collection, and the `lastModifiedLimit` which feeds into the algorithm which determines which dataset is being proxied.

The `simpleLatest` element allows for the same attributes as the `latestComplete` element minus the `lastModifiedLimit` attribute.

In this case, all the attributes have default values:
 * `name` attribute defaults to `"latest.xml"`, 
 * `top` attribute defaults to `"true"`, 
 * `serviceName` attribute defaults to `"latest"`.