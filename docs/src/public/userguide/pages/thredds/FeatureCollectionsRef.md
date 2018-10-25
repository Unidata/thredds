---
title: Feature Collections
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: feature_collections_ref.html
---

## Overview

The `featureCollection` element is a way to tell the TDS to serve collections of [CDM Feature Datasets](feature_datasets_ref.html).
Currently this is used for gridded and point datasets whose time and spatial coordinates are recognized by the CDM software stack.
This allows the TDS to automatically create logical datasets composed of collections of files, and to allow subsetting in coordinate space on them, through the WMS, WCS, Netcdf Subset, and CDM Remote Feature services.

Feature Collections have been undergoing continual development and refinement in the recent version of the TDS, and as you upgrade there are (mostly) minor changes to configuration and usage.
The `featureCollection` element was first introduced TDS 4.2, replacing the `fmrcDataset` element in earlier versions.
TDS 4.2 allowed `featureType = FMRC`, `Point`, and `Station`.
TDS 4.3 added `featureType = GRIB`, used for collections of GRIB files.
TDS 4.5 changed this usage to `featureType = GRIB1` or `GRIB2`.
TDS 5.0 added refinements for performance.

**Only serve GRIB files with featureCollection=GRIB1 or GRIB2.**
**Do not use FMRC, or NcML Aggregations on GRIB files.**

A fair amount of the complexity of feature collections is managing the collection of files on the server, both in creating indexes for performance, and in managing collections that change.
For high-performance servers, it is necessary to let a background process manage indexing, and the THREDDS Data Manager (TDM) is now available for that purpose.

This document gives an overview of Feature Collections, as well as a complete syntax of allowed elements.

Specific topics covered here are:

* [Example catalog elements](#example-catalog-elements)
* [Description of elements in TDS configuration catalogs](#description-of-elements-in-tds-configuration-catalogs)
* [The Date Extractor](#date-extractor)
* Using [external triggers](#external-triggers) for updating collections
* Static vs Chaning Datasets ([non-GRIB](#static-vs-changing-datasets-not-grib), [GRIB](#static-vs-changing-grib-datasets))
* [NcML Modificatoins](#ncml-modifications) (non-GRIB)

For Feature Type specific information, see:

* [FMRC Collections](fmrc_ref.html)
* [Point Collections](pointfeature_ref.html)
* [GRIB Collections](grib_feature_collections_ref.html)
* [GRIB specific configuration](grib_collection_config_ref.html)
* [GRIB Collection FAQs](grib_collection_config_ref.html)

Also see:

* [THREDDS Data Manager](tdm_ref.html)

## Example catalog elements

The `featureCollection` element is a subtype of `dataset` element.
It defines a logical dataset for the TDS.
All of the elements that can be used inside of a `dataset` element can be used inside of a `featureCollection` element.

### Example 1: Simple case using defaults

~~~xml
<featureCollection name="NCEP Polar Sterographic" featureType="GRIB2" 
                   path="grib/NCEP/NAM/Polar_90km"> <!-- 1 -->
  <collection name="NCEP-NAM-Polar_90km"
              spec="/data/ldm/pub/native/grid/NCEP/NAM/Polar_90km/NAM_Polar_90km_.*\.grib2$"/> <!-- 2 -->
</featureCollection>
~~~

1. A `GRIB2` Feature Collection dataset is defined, with the \"human readable\" name of \"NCEP Polar Sterographic\". 
   Its URL path(s) will look like `http://server/thredds/<service>/grib/NCEP/NAM/Polar_90km/...`​
   The Dataset `ID` is automatically set to the path, so that its dataset page will be `http://server/thredds/catalog/grib/NCEP/NAM/Polar_90km/catalog.xml?dataset=grib/NCEP/NAM/Polar_90km/...`​
2. Defines the files in the collection as any file in the directory `/data/ldm/pub/native/grid/NCEP/NAM/Polar_90km/` that matches the regular expression `NAM_Polar_90km.*\.grib2$`. 
   In this case, it means any filename starting with `NAM_Polar_90km` and ending with `.grib2`. 
   The collection name is `NCEP-NAM-Polar_90km`, which is used for index file names, etc.

### Example 2: Specify the options explicitly

~~~xml
<featureCollection name="NCEP NAM Alaska(11km)" featureType="GRIB2" path="grib/NCEP/NAM/Alaska_11km">
  <metadata inherited="true">
    <serviceName>GribServices</serviceName> <!-- 1 -->
    <documentation type="summary">NCEP GFS Model : AWIPS 230 grid</documentation> <!-- 2 -->
  </metadata>
  <collection spec="/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/.*grib2$" <!-- 3 -->
              name="NAM_Alaska_11km"
              dateFormatMark="#NAM_Alaska_11km_#yyyyMMdd_HHmm" <!-- 4 -->
              timePartition="file" <!-- 5 -->
              olderThan="5 min"/> <!-- 6 -->
  <update startup="nocheck"/> <!-- 7 -->
  <tdm rewrite="test" rescan="0 0/15 * * * ? *" /> <!-- 8 -->
</featureCollection>
~~~

1. Arbitrary metadata can be added to the catalog.
   Here, we indicate to use the service called `GribServices` (not shown, but likely a compound service that includes all the services you want to provide for `GRIB` Feature Collections).
2. A `documention` element of type `summary` is added to the catalog for this dataset.
3. The collection consists of all files ending with `grib2` in the directory `/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/`.
4. A date will be extracted from the filename, and the files will then be sorted by date. 
   Important if the lexigraphic ordering is different that the date order.
5. Partitioning will happen at the `file` level.
6. Only include files whose `lastModified` date is more than 5 minutes old.
   This is to exclude files that are actively being created.
7. Instruct the TDS to use the collection index if it already exists, without testing if it\'s up-to-date.
8. Instruct the TDM to examine all the files to detect if they have changed since the index was written.
   Rescan every 15 minutes.

## Description of elements in TDS Configuration catalogs

### `featureCollection` element

A `featureCollection` is a kind of dataset element, and so can contain the same elements and attributes of that element.
Following is the XML Schema definition for the featureCollection element:

~~~xsd
<xsd:element name="featureCollection" substitutionGroup="dataset">
  <xsd:complexType>
    <xsd:complexContent>
      <xsd:extension base="DatasetType">
        <xsd:sequence>
          <xsd:element type="collectionType" name="collection"/>
          <xsd:element type="updateType" name="update" minOccurs="0"/>
          <xsd:element type="tdmType" name="tdm" minOccurs="0"/>
          <xsd:element type="protoDatasetType" name="protoDataset" minOccurs="0"/>
          <xsd:element type="fmrcConfigType" name="fmrcConfig" minOccurs="0"/>
          <xsd:element type="pointConfigType" name="pointConfig" minOccurs="0"/>
          <xsd:element type="gribConfigType" name="gribConfig" minOccurs="0"/>
          <xsd:element type="fileSortType" name="filesSort" minOccurs="0" />
          <xsd:element ref="ncml:netcdf" minOccurs="0"/>
        </xsd:sequence>
        <xsd:attribute name="featureType" type="featureTypeChoice" use="required"/>
        <xsd:attribute name="path" type="xsd:string" use="required"/>
      </xsd:extension>
    </xsd:complexContent>
  </xsd:complexType>
</xsd:element>

<xsd:simpleType name="featureTypeChoice">
  <xsd:union memberTypes="xsd:token">
    <xsd:simpleType>
      <xsd:restriction base="xsd:token">
        <xsd:enumeration value="FMRC"/>
        <xsd:enumeration value="GRIB1"/>
        <xsd:enumeration value="GRIB2"/>
        <xsd:enumeration value="Point"/>
        <xsd:enumeration value="Station"/>
      </xsd:restriction>
    </xsd:simpleType>
  </xsd:union>
</xsd:simpleType>
~~~

Here is an example `featureCollection` as you might put it into a TDS catalog:

~~~xml
<featureCollection name="Metar Station Data" harvest="true" 
                   featureType="Station" path="nws/metar/ncdecoded"> <!-- 1 -->
  <metadata inherited="true"> <!-- 2 -->
    <serviceName>fullServices</serviceName>
    <documentation type="summary">Metars: hourly surface weather observations</documentation>
    <documentation xlink:href="http://metar.noaa.gov/" xlink:title="NWS/NOAA information"/>
    <keyword>metar</keyword>
    <keyword>surface observations</keyword>
  </metadata>
  <collection name="metars"
              spec="/data/ldm/pub/decoded/netcdf/surface/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc$" /> <!-- 3 -->
  <update startup="test" rescan="0 0/15 * * * ? *"/> <!-- 4 -->
  <protoDataset choice="Penultimate" /> <!-- 5 -->
  <pointConfig datasetTypes="cdmrFeature Files"/> <!-- 6 -->
  <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 7 -->
    <attribute name="Conventions" value="CF-1.6"/>
  </netcdf>
</featureCollection>
~~~

1. A `featureCollection` is declared, using the name and harvest attributes declared by the dataset element.
   The `featureType` is a mandatory attribute defining the type of the feature collection.
   The `path` is also required, which defines what the URL of this collection will be.
   It must be unique over the entire TDS.
   If an `ID` attribute is not specified on the `featureCollection`, the `path` attribute is used as the `ID` (_this is a recommended idiom_).
2. As is usual with `dataset` elements, a block of metadata can be declared that will be inherited by all the datasets.
3. The collection of files is defined.
   Each dataset is assigned a nominal time by extracting a date from the filename.
4. Specify that the collection is updated, when the TDS starts and in a background thread, every 15 minutes.
5. The prototype dataset is the next-to-last in the collection when sorted by time.
6. Configuration specific to the Point feature type: expose a `cdmrRemote` service on the entire collection, and also serve all the component files using the default service, in this example the compound service `fullServices`.
7. This NcML wraps each dataset in the collection.
   This attribute overrides any existing one in the datasets; it tells the CDM to parse the station information using the CF Conventions.

### `collection` element

A `collection` element defines the collection of datasets.
Example:

~~~xml
<collection spec="/data/ldm/pub/native/satellite/3.9/WEST-CONUS_4km/WEST-CONUS_4km_3.9_.*gini$"
            dateFormatMark="#WEST-CONUS_4km_3.9_#yyyyMMdd_HHmm"
            name="WEST-CONUS_4km" olderThan="15 min" />
~~~

The XML Schema for the `collection` element:

~~~xsd
<xsd:complexType name="collectionType">
  <xsd:attribute name="spec" type="xsd:string" use="required"/> (1)
  <xsd:attribute name="name" type="xsd:token"/> (2)
  <xsd:attribute name="olderThan" type="xsd:string" /> (3)
  <xsd:attribute name="dateFormatMark" type="xsd:string"/> (4)
  <xsd:attribute name="timePartition" type="xsd:string"/> (5)
</xsd:complexType>
~~~

where

1. `spec` (required): [collection specification string](collection_spec_string_ref.html).
    In this example, the collection contains all files in the directory `/data/ldm/pub/native/satellite/3.9/WEST-CONUS_4km/` whose filename matches the regular expression `WEST-CONUS_4km_3.9.gini$`, where `.` means \"match any number of characters\" and `gini$` means \"ends with the characters gini\". If you wanted to match \".gini\", you would need to escape the \".\", ie `\.gini$`.
2. `name` (required): the collection name, which _must be unique for all collections served by your TDS_. 
   This is used for external triggers, for the CDM collection index files, and for logging and debugging messages.
   If missing, the name attribute on the `<featureCollection>` element is used.
   However, we recommend that you create a unique, immutable name for the dataset collection, and put it in this name attribute of the collection element.
3. `olderThan` (optional): Only files whose `lastModified` date is older than this are included.
    This is used to exclude files that are in the process of being written.
    However, it only applies to newly found files; that is, once a file is in the collection it is not removed because it was updated.
4. `dateFormatMark` (optional): This defines a [DateExtractor](#date-extractor), which is applied to each file in the collection to assign it a date, which is used for sorting, getting the latest file, and possibly for time partitioning. 
   In this example, the string `WEST-CONUS_4km_3.9_` is located in each file path, then the [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html){:target="_blank"} template `yyyyMMdd_HHmm` is applied to the next characters of the filename to create a date.
   A [DateExtractor](#date-extractor) can also be defined in the collection specification string, but in that case the date must be contained just in the file name, as opposed to the complete file path which includes all of the parent directory names.
   Use this _OR_ a date extractor in the specification string, but _not_ both.
5. `timePartition` (optional): Currently only used by GRIB collections, see [here](partitions_ref.html) for more info.

### `protoDataset` element

Provides control over the choice of the prototype dataset for the collection.
The prototype dataset is used to populate the metadata for the feature collection.
Note that this is not used by `GRIB` feature collections.
Example:

~~~xml
<protoDataset choice="Penultimate" change="0 2 3 * * ? *">
  <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
    <attribute name="featureType" value="timeSeries"/>
  </netcdf>
</protoDataset>
~~~

The XML Schema definition for the `protoDataset` element:

~~~xsd
<xsd:complexType name="protoDatasetType">
  <xsd:sequence>
    <xsd:element ref="ncml:netcdf" minOccurs="0"/>  (1)
  </xsd:sequence>
  <xsd:attribute name="choice" type="protoChoices"/> (2)
  <xsd:attribute name="change" type="xsd:string"/> (3)
</xsd:complexType>
~~~

1. `ncml:netcdf` (optional): ncml elements that modify the prototype dataset
2. `choice = [First | Random | Penultimate | Latest]`: select prototype from a time ordered list, using the first, a randomly selected one, the next to last, or the last dataset in the list. 
   The default is `Penultimate`.
3. `change = "cron expr"` (optional): On rolling datsets, you need to change the prototype periodically, otherwise it will get deleted eventually. 
   This attribute specifies when the `protoDataset` should be re-selected, using a [cron expression](https://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html){:target="_blank"}.

The choice of the protoDataset matters when the datasets are not homogenous:

* Global and variable attributes are taken from the prototype dataset.
* If a variable appears in the prototype dataset, it will appear in the feature collection dataset. 
  If it doesn\'t appear in other datasets, it will have missing data for those times.
* If a variable does not appears in the prototype dataset, it will not appear in the feature collection dataset, even if it appears in other datasets.

### update element

For collections that change, the `update` element provides options to update the collection, either synchronously (while a user request waits) or asynchronously (in a background task, so that requests do not wait).

For `GRIB` collections, updating of the collection by the TDS is no longer supported, for either static or dynamic collections (use the [TDM](tdm_ref.html) and the [`tdm`](#tdm-element-grib-only) element for this).
However, even for `GRIB` collections, the `update` element can be used to control if collections can be re-read by the TDS using an external trigger.

Examples:

1. ~~~xml
   <update startup="test" rescan="0 0/30 * * * ? *" trigger="false"/>
   ~~~
   * If the dataset has been updated when the TDS starts up then test it, and in a background process recheck it every 30 minutes.
   * Do not allow external triggers.
   * Note: Cannot use for GRIB collections, see `tdm` element below.

2. ~~~xml
   <update recheckAfter="15 min" />
   ~~~

   * Test if the dataset has been updated only when a request comes in for it, and the dataset hasn’t been checked for 15 minutes.

3. ~~~xml
   <update startup="never" trigger="allow" />
   ~~~

   * Never update the collection indices, but allow an external program (such as the TDM) to send a trigger telling the TDS that it should reread the collection into memory. 
   * This is useful for large collections of data where even testing if a dataset has changed can be costly.

The XML Schema definition for the update element:

~~~xsd
<xsd:complexType name="updateType">
  <xsd:attribute name="recheckAfter" type="xsd:string" /> <!-- 1 -->
  <xsd:attribute name="rescan" type="xsd:token"/>         <!-- 2 -->
  <xsd:attribute name="trigger" type="collectionUpdateType"/> <!-- 3 -->
  <xsd:attribute name="startup" type="collectionUpdateType"/> <!-- 4 -->
</xsd:complexType>
~~~

1. `recheckAfter`: This will cause a new scan whenever a request comes in and this much time has elapsed since the last scan. 
   The request will wait until the scan is finished and a new collection is built (if needed), and so is called synchronous updating.
   **This option will be ignored if you are using the rescan attribute or if you have a tdm element.**
2. `rescan`: uses a [cron expression](https://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html){:target="_blank"} to specify when the collection should be re-scanned in a background task, and re-tested to see if it has changed. 
   This is called asynchronous updating.
3. `trigger`: if set to `allow` (default), then external triggering will be allowed. 
   This allows collections to be updated by an external program (or person using a browser) sending an explicit \"trigger\" URL to the server.
   This URL is protected by HTTPS, so you must [enable triggers](#external-triggers) for this to work.
   Set this to `false` to disable triggering.
4. `startup`: `[never | nocheck | testIndexOnly | test | always]` (default _never_). 
   The collection is read on server startup, and tested whether it is up to date, depending on the [collectionUpdateType](#collectionupdatetype)

### `collectionUpdateType`

~~~xsd
<xsd:simpleType name="collectionUpdateType">
  <xsd:union memberTypes="xsd:token">
    <xsd:simpleType>
      <xsd:restriction base="xsd:token">
        <xsd:enumeration value="never"/>         <!-- 1 -->
        <xsd:enumeration value="nocheck"/>       <!-- 2 -->
        <xsd:enumeration value="testIndexOnly"/> <!-- 3 -->
        <xsd:enumeration value="test"/>          <!-- 4 -->
        <xsd:enumeration value="always"/>        <!-- 5 -->
      </xsd:restriction>
    </xsd:simpleType>
  </xsd:union>
</xsd:simpleType>
 ~~~

1. `never`: the collection is used as it is, and no checking is done. 
   The collection index must already exist.
   Use this for very large collections that you don\'t want to inadvertently scan.
2. `nocheck`: the collection index is used if it exists, without checking whether its up-to-date. 
   If it doesn\'t exist, build it.
3. `testIndexOnly`: the collection index is used if it exists and it is newer than all of its immediate children.
4. `test` or `true`: the collection\'s data files are scanned and the new collection of children is compared to the old collection.
   If there are any changes, the index is rebuilt.
5. `always`: the collection is always re-scanned and the indices are rebuilt.

### tdm element (GRIB only)

You must use the `tdm` element for GRIB collections - the `update` element no longer applies.
The [TDM](tdm_ref.html) is a separate process that uses the same configuration catalogs as the TDS, and updates GRIB collections in the background.
Example:

#### static datasets

~~~xml
<tdm rewrite="test"  />
~~~

* This example tells the TDM (**not** the TDS) to test if the dataset has changed (with respect to any existing indexes on disk), and if so update it.
  If no indexes exists on disk at the time the TDM is run, then create them
  Once the test is complete and any indexes are created, the TDM will not check again unless the process is stopped and a new session of the TDM is started.

#### dynamic datasets

~~~xml
<tdm rewrite="test" rescan="0 4,19,34,49 * * * ? *"  />
~~~

* This example tells the TDM (**not** the TDS) to test if the dataset has changed 4 times every hour, specifically, at 4,19,34, and 49 minutes past the hour.
  If the collection has changed, new indices will be recreated, and a trigger will be sent to the TDS to tell it to re-read the collection so that the new data will show up in the TDS client catalogs.

* The TDM uses the trigger

  `https://server/thredds/admin/collection/trigger?collection=name&trigger=nocheck`

  This is done by executing an `HTTP` `Get` request to the TDS.
  It is sent when the TDM has done a rescan, the collection has changed, and a new collection index was made.
  The trigger tells the TDS to read in the new collection index.

The XML Schema definition for the tdm element:

~~~
<xsd:complexType name="tdmType">
  <xsd:attribute name="rewrite" type="collectionUpdateType"/> <!-- 1 -->
  <xsd:attribute name="rescan" type="xsd:token"/>             <!-- 2 -->
</xsd:complexType>
~~~

1. `rewrite`: one of the [collectionUpdateTypes](#collectionupdatetype), except for `never`.
   The most useful value is test.
2. `rescan`: uses a  [cron expression](https://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html){:target="_blank"} to specify when the collection should be re-scanned.

### fileSort element

When a collection shows a list of files, the files will be sorted by increasing name.
To use a decreasing sort, use the `fileSort` element inside the `featureCollection` element:

~~~xml
<featureCollection ... >
  ...
  <filesSort increasing = "false" />
</featureCollection>
~~~

## Date Extractor

Feature Collections sometimes (Point, FMRC, and time partitioned GRIB) need to know how to sort the collection of files.
In those cases you need to have a date in the filename and need to specify a date extractor in the specification string or include a `dateFormatMark` attribute.

1. If the date is in the **filename only**, you can use the [collection specification string](collection_spec_string_ref.html). 
   A `spec` of:

   `/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/GFS_Alaska_191km_#yyyyMMdd_HHmm#\.grib1$`

   applied to the file 

   `/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/GFS_Alaska_191km_20111226_1200.grib1`

   would result in the extraction of the date `2011-11-26T12:00:00`.

   In this case, `yyyyMMdd_HHmm` is **positional**: 
   1. the characters in the filename before the first the `#` symbol are counted
   2. the characters between the `#` symbols are counted to determine the position of the characters to extract from the filename (positions 18 though 30)
   3. the [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html){:target="_blank"} pattern `yyyyMMdd_HHmm` is applied to the extracted characters

2. When the date is in the **directory name and not completely in the filename**, you must use the `dateFormatMark`. 
   For example with a file path:

   `/data/ldm/pub/native/grid/NCEP/GFS/Alaska_191km/20111226/Run_1200.grib1`

   Use

   `dateFormatMark="#Alaska_191km/#yyyyMMdd'/Run_'HHmm"`

   In this case, the `#` characters delineate the substring match on the entire pathname, not just the file name. 
   Immediately following the match comes the string to be parsed by the [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html){:target="_blank"} pattern, in this example:

   `yyyyMMdd'/Run_'HHmm`

   Note that the `/Run_` is enclosed in _single quotes_.
   This tells  [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html){:target="_blank"} to interpret these characters literally, and they must match characters in the filename **exactly**.

   You might also need to put the  [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html){:target="_blank"} before the substring match. 
   For example, in the following structure:

   `/dataroot/stuff/20111226/Experiment-02387347.grib1`


   let\'s pretend \"stuff\" differs for each subdirectory, so you can\'t match on it.
   However, you can match on \"Experiment\", so you can use:

   `dateFormatMark="yyyyMMdd#/Experiment#"`

   Note that whatever you match on must be **unique** in the pathname.

## External Triggers

The TDS supports a REST interface that allows authorized users to send a trigger to the TDS to tell it to reread a feature collection.
The URL for the trigger is

`https://server/thredds/admin/collection/trigger?collection=name&trigger=type`

where `name` is the collection name, and `type` is a [collectionUpdateType](#collectionupdatetype).

* Typically the trigger is used by the TDM, but it can also be used manually or by another program.
* Triggering is password protected and uses SSL (see [enabling Remote Management](remote_management_ref.html#enable-secure-sockets-layer-ssl) to enable SSL).
* You must give the role `tdsTrigger` to any [user](remote_management_ref.html#configuring-tomcat-users) you want to have the right to send a trigger.
* To enable the TDM trigger, create a [user](remote_management_ref.html#configuring-tomcat-users) named `tdm` and give that user the `tdsTrigger` role.

You can see a list of the Feature Collection datasets (and manually send a `trigger=nocheck` to specific datasets) from the TDS admin page at `https://server/thredds/admin/debug?Collections/showCollection`.
To have access to this page the user must have the role `tdsConfig` (see [enabling Remote Management](remote_management_ref.html#configuring-tomcat-users)).

## Static vs. changing datasets (**Not GRIB**)

### Static Collection - Small or Rarely Used

If you have a collection that doesn’t change, do not include an `update` element.
The first time that the dataset is accessed, it will be read in and then never changed.

### Static Collection - Fast response

If you have a collection that doesn’t change, but you want to have it ready for requests, then use:

~~~xml
<update startup ="always" />
~~~

The dataset will be scanned at startup time and then never changed.

### Large Static Collection

You have a large collection, which takes a long time to scan.
You must carefully control when/if it will be scanned.

~~~xml
<update startup ="nocheck" />
~~~

The dataset will be read in at startup time by using the existing indexes (if they exist).
If indexes don\'t exist, they will be created on startup.

If it occasionally changes, then you want to manually tell it when to rescan:

~~~xml
<update startup ="nocheck" trigger="allow" />
~~~

The dataset will be read in at startup time by using the existing indexes, and you manually tell it when to rebuild the index.
You must [enable triggers](#external-triggers).

### Changing Collection - Small or Rarely Used

For collections that change but are rarely used, use the `recheckAfter` attribute on the `update` element.
This minimizes unneeded processing for lightly used collections.
This is also a reasonable strategy for small collections which don\'t take very long to scan.

~~~xml
<update recheckAfter="15 min" />
~~~

{%include important.html content="
**Do not** include both a `recheckAfter` and a `rescan` attribute.
If you do, the `recheckAfter` will be ignored.
" %}

### Changing Collection - Fast response

When you want to ensure that requests are answered as quickly as possible, read it at startup and also update the collection in the background using `rescan`:

~~~
<update startup="test" rescan="0 20 * * * ? *" />
~~~

This [cron expression](https://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html){:target="_blank"} says to rescan the collection files \"every hour at 20 past the hour\", and rebuild the dataset if needed.

### Sporadically changing Collection

To externally control when a collection is updated, [enable remote triggers](#external-triggers), and when the dataset changes, send a trigger to the TDS.

## Static vs. changing GRIB datasets

### Changing GRIB Collection (small or large)

You have a GRIB collection that changes.
The TDS can only scan indices at startup time.
You must use the TDM to detect any changes.

~~~xml
<tdm rewrite="test" rescan="0 0/15 * * * ? *" />
~~~

The dataset will be read in at startup time by the TDS using the existing indexes, and will be scanned by the TDM every 15 minutes, which can be configured to send a trigger as needed.
For very large collections, the `rescan` schedule should be carefully considered.
For example:

~~~xml
<tdm rewrite="test" rescan="0 0 3 * * ? *" />
~~~

The dataset will be read in at TDS startup time by using the existing indexes (they must already exist).
The TDM will test if its changed \"once a day at 3 am\", and send a trigger to the TDS if needed.

### Very Large GRIB Collection that doesn\'t change

You have a very large collection, which takes a long time to scan.
You must carefully control when/if it will be scanned.

~~~xml
<update trigger="false"/>
<tdm rewrite="test"/>
~~~

The TDS never scans the collection, it always uses existing indices (which must already exist). 
Run the TDM first, then after the indices are made, you can stop the TDM and start the TDS.
Since the collection does not change, there is no need to tell the TDS to re-read the collection, so disable triggering.
If the collection is updated, the TDM will need to be ran again, and the TDS will need to be restarted.

## NcML Modifications

NcML is no longer used to define the collection, but it may still be used to modify the feature collection dataset for FMRC or Point (**not GRIB**).

~~~xml
<featureCollection featureType="FMRC" name="RTOFS Forecast Model Run Collection" path="fmrc/rtofs">
  <collection spec="c:/rps/cf/rtofs/.*ofs_atl.*\.grib2$" recheckAfter="10 min" olderThan="5 min"/> <!-- 1 -->

  <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 2 -->
    <variable name="time">
      <attribute name="units" value="hours since 1953-11-29T08:57"/>
     </variable>
  </netcdf>

  <protoDataset>
    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"> <!-- 3 -->
      <attribute name="speech" value="I'd like to thank all the little people..."/>
      <variable name="mixed_layer_depth">
       <attribute name="long_name" value="mixed_layer_depth @ surface"/>
       <attribute name="units" value="m"/>
      </variable>
     </netcdf>
  </protoDataset>

</featureCollection>
~~~

1. The collection is defined by a `collection` element, allowing any number of forecast times per file
2. When you want to modify the component files of the collection, you put an NcML-related elements inside the `featureCollection` element.
   This modifies the component files before they are turned into a gridded dataset.
   In this case we have fixed the time coordinate `units` attribute, otherwise the individual files would not get recognized as Grid datasets and the feature collection will fail.
3. When you want to modify the resulting `FMRC` dataset, you put an NcML element inside the `protoDataset` element.
   In this case we have added a global attribute named `speech` and 2 attributes on the variable named `mixed_layer_depth`.

{%include note.html content="
Remember, `featureCollection`s are your friend: a `featureCollection` may be updated in the background, but aggregations will only be updated when the user makes a request (synchronously), which means the user has to wait until the update is complete (_gonna have a bad time_).
" %}