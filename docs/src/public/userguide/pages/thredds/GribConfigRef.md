---
title: GRIB Collection Config
last_updated: 2019-02-01
sidebar: tdsTutorial_sidebar
toc: false
permalink: grib_collection_config_ref.html
---

## Overview

There are a number of ways that the user can intervene in the processing of GRIB Collections, all of them controlled by `<gribConfig>` elements inside the `<featureCollection>` elements of the TDS configuration catalogs or inside NcML.
It\'s important to understand when these elements are applied and when index files need to be regenerated.

There are two stages of processing.

The first stage creates `GribCollections`, which are collections of GRIB records stored in one or more files. `GribCollections` try to make all of the records in the collection available to the user by creating a dataset with two time dimensions, a `reference` time and a `forecast` time.
At this stage, errors in encoding the `GDS` (Grid Definition Section, which defines the horizontal coordinate system) can be fixed with `gdsHash`.
Since distinct GDS will generate separate Groups in the CDM object model, fixing errors here will prevent spurious extra groups from being created.
One can also control how GRIB records are collected into Variables with `pdsHash`, how time intervals are handled with `intvFilter`, and a few other options described below.

If you make changes to any `gribConfig` parameters that affect stage one processing, you need to delete the CDM index files (**.ncx4**) to force them to be recreated.
You do _not_ need to delete the **gbx9** index files.

The second stage happens when the `GribCollection` is turned into a `NetcdfFile` by the GRIB IOSP.
At this point, the correct GRIB tables must be identified, and decisions on how to name `Groups` and `Variables` are made.
Changes to these settings may happen at any time, without having to recreate the collection indices.
They will take affect the next time the TDS starts.

These instructions are tailored for TDS users.
To work with Grib Collections in client software using the CDM stack, see [CDM Grib Files](grib_files_cdm.html).

## Stage One: GribCollection Creation

### gdsHash: Fix errors in GDS encoding

~~~xsd
<xsd:element name="gdsHash" minOccurs="0">
  <xsd:complexType>
    <xsd:attribute name="from" type="xsd:int" use="required"/>
    <xsd:attribute name="to" type="xsd:int" use="required"/>
  </xsd:complexType>
</xsd:element>
~~~

The CDM creates a different group for each different GDS (Grid Definition Section) used in the GRIB collection.
It identifies the GDS by creating a hashcode for it, and then creates a separate group for each unique hashcode.
Unfortunately, in some cases, GRIB records have GDSs that differ in minor ways, such as the fifth decimal place in the starting x and/or y coordinate.
It\'s clear that these are minor defects in the writing of the GRIB records.
If desired, the user can fix these problems through NcML or in the TDS gribConfig element.

First, one must find the GDS hashcodes by using ToolsUI.
In the `IOSP/GRIB1(2)/GribCollection` tab, enter the GRIB file name, which then shows the records in the file.
Select the GDS (at the bottom), right click for the context menu, and choose: compare GDS.
This will show the differences in the GDS and the corresponding hashcodes.
If you confirm that they are, in fact, the same GDS, then you can fix this problem by merging the two groups.
For example:

~~~xml
<gribConfig>
  <gdsHash from="1450218978" to="1450192070"/>
</gribConfig>
~~~

This changes those variables using GDS hashcode `1450218978` to use `1450192070`, which essentially merges the two groups and eliminates the spurious group in the resulting CDM index file.

Sometimes you want to remove spurious records altogether.
To do so, set the gdsHash to `0`:

~~~xml
<gribConfig>
  <gdsHash from="1450218978" to="0"/>
</gribConfig>
~~~

will ignore any records with GDS hashcode `1450218978`.

### pdsHash: Control how PDS are made into variables

~~~xds
<xsd:element name="pdsHash" minOccurs="0" maxOccurs="unbounded">
  <xsd:complexType>
    <xsd:sequence>
      <xsd:element name="useGenType" minOccurs="0" maxOccurs="1"/>
      <xsd:element name="useTableVersion" minOccurs="0" maxOccurs="1"/>
      <xsd:element name="mergeIntv" minOccurs="0" maxOccurs="1"/>
    </xsd:sequence>
  </xsd:complexType>
</xsd:element>
~~~

Information from the GRIB record, in particular the PDS (Product Definition Section) is used to group GRIB records into CDM Variables containing multidimensional arrays.
This is done by creating a \"CDM hashcode\" of each record, and then combining all records with the same hashcode into one variable.
The CDM makes certain choices on how to do this, that may need to be overridden for a particular dataset.
Currently most of these setting ar per dataset, and cannot be applied at a finer granularity, with the exception of `intvFilter`, which applies to specific Variables.

#### useGenType: GRIB-2 only.

Control whether generating type (octet `12`) is used in the CDM hashcode.
If true, records with different generating types will create different variables.
Default is `false`.
Set this to true if, in the same dataset, you have records with the same `discipline-category-parameter` but with different generating types that should be made into different variables.
By default, different generating types will be ignored.

~~~xml
<gribConfig>
  <pdsHash>
    <useGenType>true</useGenType>
  </pdsHash>
</gribConfig>
~~~

#### useTableVersion: GRIB-1 only.

Control whether table version (octet `4`) is used in the CDM hashcode.
If true, records with different table versions will create different variables.
Default is `false`.
Set this to true if, in the same dataset, you have records with the same parameter number but with different table versions that should be made into different variables.
By default, different table versions will be ignored.

~~~xml
<pdsHash>
  <useTableVersion>true</useTableVersion>
</pdsHash>
~~~

#### intvMerge: GRIB-1 and GRIB-2.

Control whether time intervals are merged.
If false, separate variables are created for each time interval length.
Default is `true`, which will generate \"mixed interval variables\" if a variable has records of different interval lengths.

~~~xml
<pdsHash>
  <intvMerge>false</intvMerge>
</pdsHash>
~~~

#### useCenter: GRIB-1 only.

Control whether center (octet `5`) and subcenter (octet `26`, if > `0`) is used in the CDM hashcode when the parameter number > `127`.
Default is `false`. Set this to true if, in the same dataset, you have records with the same parameter from different centers that should be made into different variables.
By default, different centers will be ignored.

~~~xml
<pdsHash>
  <useCenter>true</useCenter>
</pdsHash>
~~~

### intvFilter: filter on Time Interval

~~~xsd
<xsd:element name="intvFilter" minOccurs="0" maxOccurs="unbounded">
  <xsd:complexType>
    <xsd:sequence>
      <xsd:element name="variable" minOccurs="0" maxOccurs="unbounded">
        <xsd:complexType>
          <xsd:attribute name="id" type="xsd:string" use="required"/>
          <xsd:attribute name="prob" type="xsd:string" use="optional"/>
        </xsd:complexType>
      </xsd:element>
    </xsd:sequence>
    <xsd:attribute name="excludeZero" type="xsd:boolean" use="optional"/>
    <xsd:attribute name="intvLength" type="xsd:int" use="optional"/>
    <xsd:attribute name="interval" type="xsd:string" use="optional"/>
  </xsd:complexType>
</xsd:element>
~~~

GRIB makes extensive use of time intervals as coordinates.
By following [CF Cell Boundaries](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/cf-conventions.html#cell-boundaries){:target="_blank"}, time interval coordinates use an auxiliary coordinate to describe the intervals.
For example a coordinate named `time1(30)` will have an auxiliary coordinate `time1_bounds(30,2)` containing the lower and upper bounds of the time interval for each coordinate.
Currently, the CDM places all intervals in the same variable (rather than create separate variables for each interval size), unless `intvMerge` is set to false.
When all intervals have the same size, the interval size is added to the variable name. 
Otherwise the phrase \"mixed_intervals\" is added to the variable name.

Generally, the CDM places the coordinate value at the end of the interval.
For example the time interval `(0,6)` will have a coordinate value `6`.
The CDM looks for unique intervals in constructing the variable.
This implies that the coordinate values are not always unique, but the coordinate bounds pair are always unique.
Application code needs to understand this to handle this situation correctly, by checking `CoordinateAxis1D.isInterval()` or `CoordinateAxis2D.isInterval()`.

NCEP GRIB2 model output, at least, has some issues that we are slowing learning how best to deal with. There are several situations which the user can fix:

1. `excludeZero`
 
    In some NCEP models, we see time intervals with length 0. In some cases, the data arrays are
    uniformly zero, in some cases, they seem to have valid data.
    By default, intervals of length `0` are included. 
    You can choose to exclude zero length intervals by setting `excludeZero="false"`.

2. `intvLength`

   By default, intervals of all lengths are used.
   You can choose that certain parameters use only selected intervals.
   This is helpful when the parameter has redundant mixed levels, which can be derived from the set of intervals of a fixed size.

   For example, the 3 hour intervals `(0,3)`, `(3, 6)`, `(6,9)`, `(9,12)` intervals are all present, and so other intervals `(0,6)`, `(0,9)`, `(0,12)` can be ignored.

3. `interval`
   
   You can remove records of a specified interval.
   Currently this will filter all variables.
   This is _experimental_.

Examples
Here are examples using NcML:

~~~xml
<gribConfig>
  <intvFilter excludeZero="false"/> <!-- 1 -->
  <intvFilter intvLength="3">       <!-- 2 -->
    <variable id="0-1-8"/>
    <variable id="0-1-10"/>
  </intvFilter>
  <intvFilter interval="225,228"/> <!-- 3 -->
</gribConfig>
~~~

1. Do not ignore 0 length time intervals.
2. For variables `0-1-8` and `0-1-10`, only include records with time interval `length = 3`. 
   This will simplify those variables so that they only contain 3 hour intervals, instead of a mixture of intervals.
3. Exclude any records with the interval `(225,228)`.

Note that GRIB-1 uses IDs of `center-subcenter-version-param`, e.g. `7-4-2-132`, while GRIB-2 uses IDs of `discipline-category-number`, e.g. `0-1-8`.

Also see [CDM GRIB](grib_files_cdm.html) docs.

#### option: set miscellaneous values

Miscellaneous values that control how the `GribCollection` is made can be set with option elements.
All option elements are key / value pairs.

`timeUnit`

The unit of the time coordinates is taken from the first GRIB record in the collection. 
Occasionally you may want to override this.
The value must be a valid string for `ucar.nc2.time.CalendarPeriod.of( timeUnit)`

~~~xml
<gribConfig>
    <option name="timeUnit" value="1 minute" />
</gribConfig>
~~~

`unionRuntimeCoord`

When multiple reference times are in the same dataset, but they differ for different variables, unique runtime coordinates are created by default.
These can proliferate in a large collection, differing only by a few missing records.
By setting the `runtimeCoordinate` option to `union`, you can force all variables to use the same runtime coordinates, at the cost of some extra missing values.
This happens only at the leaf collections (e.g. a file or directory).

~~~xml
<gribConfig>
    <option name="runtimeCoordinate" value="union" />
</gribConfig>
~~~

## Stage Two: NetcdfFile Creation

### gdsName: Rename groups

When a dataset has multiple groups, the groups are automatically named by the projection used and the horizontal dimension size, eg `LatLon-360x720`.

A user can set group names manually in the TDS configuration catalog.
To do so, find the group hash as in the gdsHash example above.
Then use the `gdsName` element like this:

~~~xml
<gribConfig>
  <gdsName hash='-1960629519' groupName='KTUA Arkansas-Red River RFC'/>
  <gdsName hash='-1819879011' groupName='KFWR West Gulf RFC'/>
  <gdsName hash='-1571856555' groupName='KORN Lower Mississippi RFC'/>
   ...
</gribConfig>
~~~

The groupName is used in URLs, so don\'t use any special characters, like `:`.

ToolsUI will generate the XML of the GDS in a collection.
Open the collection in the `IOSP/GRIB1(2)/GribCollection` tab, and click on the \"Show GDS use\" button on the top right.
This will create a template you can then modify:

~~~xml
<gdsName hash='1450192070' groupName='Gaussian latitude/longitude-576X1152'/>
~~~

### datasetTypes (TDS only)

Define which datasets are available in the TDS catalog.
By default, all are enabled.
* `TwoD`: the full dataset with two time coordinates: runtime and forecast time
* `Best`: the \"best timeseries\" of the collection dataset, one time coordinate (forecast time)
* `Latest`: add latest resolver dataset to catalog
* `Files`: show component files, allow them to be downloaded via HTTP. (
  For File partitions which have a single file in each partition, this functionality is enabled by including an `HTTPServer` in the services.)

~~~xml
<gribConfig datasetTypes="TwoD Best Latest" />
~~~

### latestNamer (TDS only)

Rename the latest file dataset

Change the name of the latest file dataset in the collection, as listed under the Files entry (the default name is \"Latest File\").
The `datasetTypes` options `LatestFile` and `Files`, must be enabled.
Note that this does not affect dataset `urlPath`, which is always `latest.xml`.

~~~xml
<gribConfig>
  <latestNamer name="My Latest Name"/>
</gribConfig>
~~~

### bestNamer (TDS only)

Rename the Best dataset

Change the name of the `Best` dataset in the collection (the default name is \"Best Timeseries\").
The `datasetTypes` `Best` option must be selected.
Note that this does not affect dataset `urlPath`.

~~~xml
<gribConfig>
  <bestNamer name="My Best Name" />
</gribConfig>
~~~

### filesSort (TDS only)

Sort the dataset listings under the Files dataset

Sort the files [lexigraphically](https://en.wikipedia.org/wiki/Lexicographical_order){:target="_blank"}, either increasing or decreasing (default GRIB Feature Collection behavior is the same as `increasing = true`).

~~~xml
<gribConfig>
  <filesSort increasing="false" />
</gribConfig>
~~~

## gribConfig XML Schema

The `gribConfig` schema definition, version 1.2

see: [http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.2.xsd](http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.2.xsd){:target="_blank"}

~~~xsd
<xsd:complexType name="gribConfigType">
 <xsd:sequence>

  <xsd:element name="gdsHash" minOccurs="0"> <!-- 1 -->
   <xsd:complexType>
     <xsd:attribute name="from" type="xsd:int" use="required"/>
     <xsd:attribute name="to" type="xsd:int" use="required"/>
   </xsd:complexType>
  </xsd:element>

  <xsd:element name="gdsName" minOccurs="0" maxOccurs="unbounded"> <!-- 2 -->
   <xsd:complexType>
     <xsd:attribute name="hash" type="xsd:int"/>
     <xsd:attribute name="groupName" type="xsd:string"/>
   </xsd:complexType>
  </xsd:element>

  <xsd:element name="pdsHash" minOccurs="0" maxOccurs="unbounded"> <!-- 3 -->
   <xsd:complexType>
    <xsd:sequence>
      <xsd:element name="useGenType" minOccurs="0" maxOccurs="1"/>
      <xsd:element name="useTableVersion" minOccurs="0" maxOccurs="1"/>
      <xsd:element name="mergeIntv" minOccurs="0" maxOccurs="1"/>
    </xsd:sequence>
   </xsd:complexType>
  </xsd:element>

  <xsd:element name="intvFilter" minOccurs="0" maxOccurs="unbounded"> <!-- 4 -->
   <xsd:complexType>
    <xsd:sequence>
      <xsd:element name="variable" minOccurs="0" maxOccurs="unbounded">
       <xsd:complexType>
         <xsd:attribute name="id" type="xsd:string" use="required"/>
         <xsd:attribute name="prob" type="xsd:string" use="optional"/>
       </xsd:complexType>
      </xsd:element>
    </xsd:sequence>
     <xsd:attribute name="excludeZero" type="xsd:boolean" use="optional"/>
     <xsd:attribute name="intvLength" type="xsd:int" use="optional"/>
   </xsd:complexType>
  </xsd:element>

  <xsd:element name="timeUnitConvert" minOccurs="0"> <!-- 5 -->
    <xsd:complexType>
      <xsd:attribute name="from" type="xsd:int" use="required"/>
      <xsd:attribute name="to" type="xsd:int" use="required"/>
    </xsd:complexType>
  </xsd:element>

  <xsd:element name="option" minOccurs="0" maxOccurs="unbounded"> <!-- 6 -->
     <xsd:complexType>
       <xsd:attribute name="name" type="xsd:string" use="required"/>
       <xsd:attribute name="value" type="xsd:string" use="required"/>
     </xsd:complexType>
  </xsd:element>

  <xsd:element name="latestNamer" minOccurs="0" maxOccurs="1"> <!-- 7 -->
   <xsd:complexType>
     <xsd:attribute name="name" type="xsd:string" use="required"/>
   </xsd:complexType>
  </xsd:element>

  <xsd:element name="bestNamer" minOccurs="0" maxOccurs="1"> <!-- 8 -->
   <xsd:complexType>
     <xsd:attribute name="name" type="xsd:string" use="required"/>
   </xsd:complexType>
  </xsd:element>

  <xsd:attribute name="datasetTypes" type="gribDatasetTypes"/> <!-- 9 -->
</xsd:complexType>

<xsd:simpleType name="gribDatasetTypes">
 <xsd:union memberTypes="xsd:token">
  <xsd:simpleType>
    <xsd:restriction base="xsd:token">
      <xsd:enumeration value="TwoD"/>
      <xsd:enumeration value="Best"/>
      <xsd:enumeration value="Files"/>
      <xsd:enumeration value="Latest"/>
   </xsd:restriction>
  </xsd:simpleType>
 </xsd:union>
</xsd:simpleType>
~~~

1. `gdsHash`: Fix errors in GDS encoding
2. `gdsName`: Rename groups
3. `pdsHash`: Control how PDS are made into variables
4. `intvFilter`: filter on Time Interval
5. `timeUnitConvert`: do not use
6. `option`: set miscellaneous values: set miscellaneous values
7. `latestNamer` (TDS only)
8. `bestNamer` (TDS only)`: Rename the best file dataset
9. `datasetTypes` (TDS only): which datasets appear in the TDS catalog:
   * `TwoD`: the full dataset with two time dimensions, reference time and forecast time.
   * `Best`: the "best timeseries" of the collection dataset
   * `Files`: each physical file is exposed as a dataset that can be downloaded.
   * `Latest`: add latest resolver dataset to Files catalog (Files must also be selected)