---
title: PointFeature Collections
last_updated: 2018-10-15
sidebar: tdsTutorial_sidebar
toc: false
permalink: pointfeature_collection_ref.html
---

## Overview

A Point Feature Collection is a collection of files which the CDM can recognize as containing [Point Features](pointfeature_ref.html).

### Constraints on Point Feature Collections

* The component files of the collection must all be recognized as **Point** or **Station** Feature type by the CDM software.
* The component files must be partitioned by time.
  The starting time must be part of the filename, in a way that can be extracted with a [DateExtractor](feature_collections_ref.html#date-extractor).
* The component files are assumed to be homogenous, that is, they contain the same collection of variables and attributes, and they must be on the same horizontal and vertical grid.
  The component files can differ only in their time coordinates and the actual data values.

## Example Point Feature Collections

### station data

~~~xml
<featureCollection name="Metar Station Data" harvest="true" 
                   featureType="Station" path="nws/metar/ncdecoded">
  <collection spec="/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc$" />
  <update startup="true" rescan="0 0/15 * * * ? *" trigger="allow"/>
  <protoDataset choice="Penultimate" />
  <pointConfig datasetTypes="cdmrFeature Files"/>
</featureCollection>
~~~

### point data

~~~xml
<featureCollection name="Surface Buoy Point Data" harvest="true" 
                   featureType="Point" path="nws/buoy/ncdecoded">
  <collection spec="/data/ldm/pub/decoded/netcdf/surface/buoy/Surface_Buoy_#yyyyMMdd_HHmm#.nc$" />
  <update startup="true" rescan="0 0/15 * * * ? *" trigger="allow"/>
  <protoDataset choice="Penultimate" />
  <pointConfig datasetTypes="cdmrFeature Files"/>
</featureCollection>
~~~

### pointConfig element

Defines options on feature collections with `featureType=POINT` or `STATION`

~~~xml
<pointConfig datasetTypes="cdmrFeature Files" />
~~~

where:

* `datasetTypes`: list the dataset types that are exposed in the TDS catalog.
   The possible values are:
  * `cdmrFeature`: creates a CdmrFeature dataset and service.
     All of the files in the collection are treated as part of the same dataset.
  * `Files`: each component file of the collection is available separately, as in a `datasetScan`. 
    A `latest` file will be added.

### Notes:

* If there is a `serviceType=HTTPServer` for the Feature Collection, it is removed from the virtual datasets (all except the `Files` datasets).
* If an `pointConfig` element is not present, the default is `datasetTypes="cdmrFeature Files"`. 
  Specifying your own `pointConfig` completely overrides the `datasetTypes` default.