---
title: THREDDS Web Coverage Service
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: wcs_ref.html
---

The THREDDS WCS Server implements the OGC Web Coverage Service (WCS) 1.0.0 specification.
It serves gridded data in GeoTIFF or NetCDF format allowing WCS clients to specify a subset of a gridded dataset and download the resulting GeoTIFF or netCDF files.

## Which files can be served through the WCS server?

Data files must contain gridded data.
The NetCDF-Java Common Data Model must be able to identify the coordinates system used.
Check this by opening in the Grid Panel of the ToolsUI application.
There should be one or more variables shown as a `GeoGrid` or `Coverage`.
Currently due to `WCS` and `GeoTIFF` limitations, the X and Y axes must be regularly spaced. 

## Configuration

WCS is enabled by default.
To disable the service, by add the following to the TDS config file, located at `${tds.content.root.path}/thredds/threddsConfig.xml`:

<WCS>
  <allow>fakse</allow>
</WCS>

If you are not using the dafault services of the TDS, the service element's `serviceType` and `base` attribute values must be as follows in the configuration catalog:

~~~xml
<service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
~~~

The dataset to be served must reference this service (or a containing compound service) by the service name:

~~~xml
<dataset ID="sample" name="Sample Data" urlPath="sample.nc">
  <serviceName>wcs</serviceName>
</dataset>
~~~

The dataset can be configured by `datasetRoot` or `datasetScan` as appropriate (see [Basics of Configuration Catalogs](basic_config_catalog.html)).
They are listed in the resulting THREDDS catalogs as are other datasets.
`WCS` clients may not be able to directly use the THREDDS catalogs to find the WCS services but the catalogs are useful for users to browse and for separate search services (e.g., OGC catalog services).  

## Serving Remote Dataset

The TDS can also serve remote datasets with the WCS protocol if configured to do so.
It must be explicitly configured in the `${tds.content.root.path}/thredds/threddsConfig.xml` configuration file.
This is done by adding an allowRemote element to the WCS element as follows:

~~~xml
<WCS>
  <allow>true</allow>
  <allowRemote>true</allowRemote> 
  ...
</WCS>
~~~

A slight extension of the WCS Dataset URL format allows the TDS to serve remote datasets.
The dataset is identified by adding the parameter dataset whose value is a URL:

~~~
http://servername:8080/thredds/wcs?dataset=datasetURL
~~~

The URL must be a dataset readable by the NetCDF-Java library, typically an OPeNDAP dataset on another server.
It must have `gridded` data, with identifiable coordinate systems, etc.

For example, an OPeNDAP URL might be:

~~~
http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/gac/AG14day.nc 
~~~

This can be served remotely as a WCS dataset with this URL:

~~~
http://servername:8080/thredds/wcs?dataset=http://las.pfeg.noaa.gov/cgi-bin/nph-dods/data/oceanwatch/nrt/gac/AG14day.nc
~~~

## Capabilities/Limitations

### Current WCS 1.0.0 Implementation (version=1.0.0)

The current TDS implementation of WCS 1.0 has the following restrictions:

1. No interpolation is available (i.e., interpolationMethod="none").
2. CRS/SRS
  * All CRS/SRS are listed as "WGS84(DD)" even though it may have little relation to the actual CRS of the data.
  * CRS is horizontal, XY, only (see Range below for vertical, Z)
  * The response coverage is in the native CRS of the data (as the "No Interpolation" item implies).
  * The netCDF-Java library understands a number of projections (a subset of  the CF convention grid mapping options, and most assuming a spherical earth) including a simple lat/lon grid [-180/180 and -90/90].
  * All BBOX requests are assumed to be in the lat/lon of the native projection.
3. Temporal selection: only one value can be specified (no list or min/max/res).
4. Range:
  * Each coverage has only one range field
  * "Vertical" is the range axis, only if the coordinate has a vertical component.
  * Range axis selection: only one value can be specified (no list or min/max/res).
5. Supported GetCoverage response formats:
  * GeoTIFF: A grayscale 8-bit GeoTIFF file
  * GeoTIFFfloat: A floating point "Data Sample" GeoTIFF file
  * NetCDF3: A NetCDF file following the CF-1.0 convention

### Upcoming WCS 1.0.0 Implementation (version=1.0.0.1)

Same as above (version=1.0.0) except:

1. CRS/SRS
  * Some improvment in CRS encoding (i.e., not all are listed as "WGS84(DD)")
2. Temporal selection: only min/max is supported. No resolution is allowed on the min/max and no lists are allowed.
3. "Vertical" range axis selection: only min/max is supported. No resolution is allowed on the min/max and no lists are allowed.
4. Supported GetCoverage response formats:
  * NetCDF3: A NetCDF file following the CF-1.0 convention
  * GeoTIFF (Only supports requests for single time and single height.)
    * GeoTIFF: A grayscale 8-bit GeoTIFF file
    * GeoTIFF_Float: A floating point "Data Sample" GeoTIFF file

### Upcoming WCS 1.0.0+ Implementation (version=1.0.0.11)

The WCS 1.0.0+ implementation experiment is targeted to further the understanding of how FES datasets fit into the current WCS and what extensions they may require. The conclusions will be fed back to the WCS 1.2 RWG.

As currently expected, same as above (version=1.0.0.1) except:

1. CRS/SRS
 * If coverage has vertical, it is part of the CRS
2. Range:
 * Each coverage can contain multiple range fields
 * If range field is array, only min/max range axis selection allowed.
3. Supported GetCoverage response formats:
 * NetCDF3: A NetCDF file following the CF-1.0 convention
 * GeoTIFF (Only supports requests for single time and single height.)
    * GeoTIFF: A grayscale 8-bit GeoTIFF file
    * GeoTIFF_Float: A floating point "Data Sample" GeoTIFF file

## WCS Dataset URLs

All TDS WCS requests start with

~~~
http://servername:8080/thredds/wcs/ 
~~~

The next part of the path indicates which file to use

~~~
http://servername:8080/thredds/wcs/test/sst.nc 
~~~

This is typically the URL you will need to pass to a WCS client.
The WCS Client then forms various queries to the THREDDS WCS server for that dataset, e.g.:

~~~
http://servername:8080/thredds/wcs/test/sst.nc?service=WCS&version=1.0.0&request=GetCapabilities
~~~

Examples

Here are example WCS queries (REST API Call appened to the wcs access url of a dataset):

| request | REST API call |
| GetCapabilities | `?request=GetCapabilities&version=1.0.0&service=WCS` |
| DescribeCoverage | `?request=DescribeCoverage&version=1.0.0&service=WCS&coverage=<variable_name>` |
| GetCoverage (GeoTIFF) | `?request=GetCoverage&version=1.0.0&service=WCS&format=GeoTIFF&coverage=<variable_name>&time=<iso_time_string>&vertical=<vertical_level>&bbox=<west,south,east,north>` |
| GetCoverage (NetCDF3) | `?request=GetCoverage&version=1.0.0&service=WCS&format=NetCDF3&coverage=<variable_name>&time=<iso_time_string>&vertical=<vertical_level>&bbox=<west,south,east,north>` |

## WCS Clients

A few WCS clients we know of (though we haven't tried all of them):

* [OWSlib](http://geopython.github.io/OWSLib/){:target="_blank"} (WMS and WCS) [free]
* [Map Express](https://www.cadcorp.com/products/free-mapping-software/){:target="_blank"} (WMS and `WCS`) [commercial / free]
* [IDL](http://www.harrisgeospatial.com/ProductsandSolutions/GeospatialProducts/IDL.aspx){:target="_blank"} (WMS) [commercial]
* [gvSIG](http://www.gvsig.org/web/){:target="_blank"} (WMS and WCS) [free]

This one is not a general client.
It is a server site with a web interface for accessing their served data:

 * [DATAFed](http://www.datafed.net/){:target="_blank"}
