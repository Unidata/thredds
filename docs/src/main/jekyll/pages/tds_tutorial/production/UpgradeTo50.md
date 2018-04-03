---
title: Upgrading to CDM / TDS version 5
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: upgrade_to_5.html
---

## Requirements

* Java 8 is now required
* Tomcat 8 (servlet 3.1)
* On the command line when starting up Tomcat/TDS, you must specify `-Dtds.content.root.path=<content root>` where `<content root>` points to the top of the content directory.
  Note that this is `${tomcat_home}/content/`, not`${tomcat_home}/content/thredds/`. Don't forget the trailing slash. For example:

  ~~~bash
  -Dtds.content.root.path=/opt/tomcat-home/content/
  ~~~

## Overview

The configuration catalogs and internal state of the TDS has been extensively reworked to be able to scale to large numbers of catalogs, datasets, and internal objects without excessive use of memory.
A running TDS can be triggered to reread the configuration catalogs without having to restart.
It can be configured to reread only changed catalogs, for fast incremental updates. Other features have been added to make writing configuration catalogs more maintainable, including the `<catalogScan>` element, and default and standard services.

The other major enhancement is that `GridDataset` is replaced by `FeatureDatasetCoverage`, to better support very large feature collections.
The Coverage API works with _coordinate  values_ (not _array indices_), which solves various intractable problems that arise when using array index subsetting on large collections.

A number of API enhancements have been made to take advantage of evolution in the Java language, for example _try-with-resource_ and _foreach_ constructs.
The use of these make code simpler and more reliable.

Deprecated classes and methods have been removed, and the module structure and third-party jar use has been improved.

## API Changes

### Unsigned Types

* `DataType` now has unsigned types: `UBYTE`, `USHORT`, `UINT`, `ULONG`
* `Array`, `ArrayScalar`, `ArrayByte`, `ArrayInt`, `ArrayShort`, `ArrayLong` factory and constructor methods now require `isUnsigned` parameter.
* `Array.factory(class, shape)` &rarr; `Array.factory(DataType, shape)` or `Array.factory(dataType, class, shape)`
* `Array.get1DJavaArray(Class)` &rarr; `Array.get1DJavaArray(DataType)` or `Array.get1DJavaArray(Class, isUnsigned)`
* Remove `Array.setUnsigned()`, `Variable.setUnsigned()`
* `Variable.getUnsigned()` &rarr; `Variable.getDataType().getUnsigned()`
* `new Attribute(String name, List values)` &rarr; `new Attribute(String name, List values, boolean isUnsigned)`
* `StructureDataScalar.addMember(String name, String desc, String units, DataType dtype, boolean isUnsigned, Number val)` &rarr; `StructureDataScalar.addMember(String name, String desc, String units, DataType dtype, Number val)`

### Variable Length (vlen) Dimensions and Variables

* The CDM data model is clarified to allow _vlen_ dimensions only in the outermost (fastest changing) Dimension.
* Reading a Variable with only a single _vlen_ Dimension will result in a regular Array.
* Reading a Variable with a nested _vlen_ Dimension will result in an ArrayObject containing regular Arrays of independent lengths.
* In both cases the returned array's `DataType` will be the primitive type.
* Previously the exact Array class and DataType returned from a read on a vlen was not well-defined.
* Use `Array.isVlen()` to discover if an Array represents vlen data.
* `ArrayObject.factory(Class classType, Index index)` is now `ArrayObject.factory(DataType dtype, Class classType, boolean isVlen, Index index)`
* Use `Array.makeVlenArray(int[] shape, Array[] data)` to construct _vlen_ data.
* See [here](variable_length_data.html) for more information.

### AutoCloseable

_AutoCloseable_ was introduced in Java 7, along with the _try-with-resources_ language feature.
Use of this feature makes code more readable and more reliable in ensuring that resources (like file handles) are released when done.
We strongly recommend that you modify your code to take advantage of it wherever possible.
For example:

~~~java
 try (NetcdfFile ncfile = NetcdfFile.open(location)) {
    ...
 } catch (IOException ioe) {
   // handle ioe here, or propagate by not using catch clause
 }
~~~

* The following now implement _AutoCloseable_, so can be the target of _try-with-resource_:
 * `NetcdfFile`, `NetcdfFileWriter`
 * `HTTPMethod`, `HTTPSession`
 * `FeatureDataset`, `CoverageDataset`, `CoverageDatasetCollection`
 * `CFPointWriter`, `Grib2NetcdfFile`
 * ArrayStructure (deprecate _finish()_)
 * `PointFeatureCollectionIterator` (`finish()` method now deprecated)
 * `StructureDataIterator`, `PointFeatureIterator`, `PointFeatureCollectionIterator`, `NestedPointFeatureCollectionIterator`
 * `thredds.client.catalog.tools.DataFactory.Result`

### Iterable

_Iterable_ was introduced in Java 7, along with the _foreach_ language feature, and makes code more readable with less boilerplate.
For example:

~~~java
for (StructureData sdata : myArrayStructure) {
    // ...
}
~~~

* The following now implement `Iterable<>`, and so can be the target of _foreach_:
 * `Range` implements `Iterable<Integer>` (replace `first()`, `last()`, `stride()` methods)
 * `ArrayStructure` implements `Iterable<StructureData>` (replace `getStructureDataIterator()` method)
 * `PointFeatureIterator` extends `Iterator<PointFeature>`  (`finish()` method deprecated)
  * In order for `PointFeatureIterator` to implement `Iterator<PointFeature>`, the `hasNext()` and `next()` methods cannot throw `IOException`.
    The interface is changed to remove `throws IOException`, which will now be wrapped in `RuntimeException`.
 * `PointFeatureCollection` implements `Iterable<PointFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationTimeSeriesFeatureCollection` implements `Iterable<StationTimeSeriesFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `ProfileFeatureCollection` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `TrajectoryFeatureCollection` implements `Iterable<TrajectoryFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationProfileFeature` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationProfileFeatureCollection` implements `Iterable<StationProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `SectionFeature` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `SectionFeatureCollection` implements `Iterable<SectionFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)

### ucar.nc2.util.DiskCache2

* All instances of `DiskCache2` now have one cleanup thread
* The `DiskCache2.exit()` method is now static and need only be called once when the application is exiting.
* `DiskCache2.setLogger()` is removed.
* `DiskCache2.cleanCache(File dir, StringBuffer sbuff, boolean isRoot)` is now `DiskCache2.cleanCache(File dir, Formatter sbuff, boolean isRoot)`
* deprecated methods are removed: `setCachePathPolicy(int cachePathPolicy, String cachePathPolicyParam)`, `setPolicy(int cachePathPolicy)`
* logging of routine cache cleanup is now at `DEBUG` level

### ucar.ma2.Range

 * `Range.copy(String name)` replaced by `Range.setName(String name)`
 * `Range.getIterator()` deprecated, use `Range.iterator()`

Currently a `Range` is specified by _start:end:stride_
In the future, may be extended with subclasses `RangeScatter` and `RangeComposite`
You should use the iterator now to ensure correct functionality.
To iterate over the values of the `Range`:

~~~java
for (int i=range.first(); i<=range.last(); i+= range.stride()) {    // REPLACE THIS
 // ...
}

for (int i : range) {  // USE THIS
 // ...
}
~~~

### ucar.nc2.util.cache

* `FileCache` and `FileFactory` take a `DatasetUrl` instead of a String `location`

### ucar.nc2.dataset

In order to disambiguate remote protocols, all using _http:_, the utility method `DatasetUrl.findDatasetUrl(location)` is used to determine the protocol and capture the result in a `DatasetUrl` object.
Because this can be expensive, the `DatasetUrl` should be calculated once and kept for the duration of the dataset access.
When the protocol is already known, the `DatasetUrl(ServiceType protocol, String location)` constructor may be used.
The API is changed to allow/require the application to compute these `DatasetUrls`.

* `NetcdfDataset.acquireDataset()` takes a `DatasetUrl` instead of a String location.
* the general method of `NetcdfDataset.openDataset()` takes a DatasetUrl instead of a String location.
Variants use a String location, and call `DatasetUrl.findDatasetUrl(location)`.

* `CoordinateAxis2D.getMidpoints()` was deprecated and now removed, use `getCoordValuesArray()`

### ucar.nc2.ft.PointFeature

* Added method `getTimeUnit()`. An implementation exists in `ucar.nc2.ft.point.PointFeatureImpl`, so if your
`PointFeature` extends it, you shouldn't need to do any work.
* Removed method `getObservationTimeAsDate()`. Instead, use `getObservationTimeAsCalendarDate().toDate()`.
* Removed method `getNominalTimeAsDate()`. Instead, use `getNominalTimeAsCalendarDate().toDate()`.
* Removed method `getData()`. Instead, use `getDataAll()`.

### ucar.ma2.MAMath

* Added method `equals(Array, Array)`. It is intended for use in `Object.equals()` implementations.
This means, among other things, that corresponding floating-point elements must be exactly equal, not merely within
some epsilon of each other.
* Added method `hashCode(Array array)`. It is intended for use in `Object.hashCode()` implementations and is
compatible with `equals(Array, Array)`.
* Renamed `isEqual(Array, Array)` to `nearlyEquals(Array, Array)`. This was done to avoid (some) confusion with the new
`equals(Array, Array)`, and to highlight that this method performs _approximate_ comparison of floating-point numbers,
instead of the exact comparison done by `equals(Array, Array)`.

### Coordinate Systems

* `ucar.nc2.dataset.CoordTransBuilderIF` is split into `ucar.nc2.dataset.builder.HorizTransformBuilderIF` and `ucar.nc2.dataset.builder.VertTransformBuilderIF`
* `HorizTransformBuilderIF` now uses `AttributeContainer` instead of `NetcdfDataset`, `Variable`
* `CoordinateTransform.makeCoordinateTransform(NetcdfDataset ds, Variable ctv)` is now `ProjectionCT makeCoordinateTransform(AttributeContainer ctv)`
* Previously, the optional _false_easting_ and _false_northing_ should match the units of the _x_ and _y_ projection coordinates in `ucar.nc2.dataset.CoordinateSystem`
 * `List<Dimension> getDomain()` &rarr; `Collection<Dimension> getDomain()`
 * boolean `isSubset(List<Dimension> subset, List<Dimension> set)` &rarr; `isSubset(Collection<Dimension> subset, Collection<Dimension> set)`

### Feature Datasets

* `ucar.nc2.dt.TypedDatasetFactory` has been removed. Use `ucar.nc2.ft.FeatureDatasetFactoryManager`
* `ucar.nc2.dt.grid` is deprecated (but not removed) and is replaced by `ucar.nc2.ft2.coverage`
* `ucar.nc2.dt.point` and `ucar.nc2.dt.trajectory` have been removed, replaced by `ucar.nc2.ft.\*`
* In `FeatureDataset`, deprecated methods `getDateRange()`, `getStartDate()`, `getStartDate()` have been removed
* In `FeatureDataset`, mutating method removed: `calcBounds()`

### Point Feature Datasets (`ucar.nc2.ft` and `ucar.nc2.ft.point`)

* `FeatureCollection` has been renamed to `ucar.nc2.ft.DsgFeatureCollection` for clarity.
* `SectionFeature` and `SectionFeatureCollection` have been renamed to `TrajectoryProfileFeature`, `TrajectoryProfileFeatureCollection` for clarity.
 * `FeatureType.SECTION` renamed to `FeatureType.TRAJECTORY_PROFILE` for clarity.
 * `NestedPointFeatureCollection` has been removed, use `PointFeatureCC` and `PointFeatureCCC` instead when working with `DsgFeatureCollection` in a general way.
* In all the Point Feature classes, `DateUnit`, `Date`, and `DateRange` have been replaced by `CalendarDateUnit`, `CalendarDate`, and `CalendarDateRange`:
 * In `PointFeature` and subclasses, deprecated methods `getObservationTimeAsDate()`, `getNominalTimeAsDate()` have been removed
 * In `ProfileFeature`, `getTime()` returns `CalendarDate` instead of `Date`
 * In `PointFeature` implementations and subclasses, all constructors use `CalendarDateUnit` instead of `DateUnit`, and all `subset()` and `flatten()` methods use `CalendarDateRange`, not `DateRange`
 * In `CFPointWriter` subclasses, all constructors use `CalendarDateUnit` instead of `DateUnit`
* In `PointFeature`, deprecated method `getData()` is removed; usually replace it with `getDataAll()`
* In `PointFeatureCollection`, mutating methods are removed: `setCalendarDateRange()`, `setBoundingBox()`, `setSize()`, `calcBounds()`
* The time and altitude units for the collection can be found in the `DsgFeatureCollection`, and you can get the collection object from `PointFeature.getFeatureCollection()`
* In `PointFeatureIterator` and subclasses, methods `setCalculateBounds()`, `getDateRange()`, `getCalendarDateRange()`, `getBoundingBox()`, `getSize()` have been removed. That information is obtained from the `DsgFeatureCollection`.
* In `PointFeatureIterator` and subclasses, `setBufferSize()` bas been removed.
* In `PointFeatureCollection` and subclasses, `getPointFeatureIterator()` no longer accepts a `bufferSize` argument.

### Coverage Feature Datasets (`ucar.nc2.ft2.coverage`)

* Completely new package `ucar.nc2.ft2.coverage` that replaces `ucar.nc2.dt.grid`
  The class `FeatureDatasetCoverage` replaces `GridDataset`.
* Uses of classes in `car.nc2.dt.grid` are deprecated, though the code is still in the core jar file for now.
* For new API see [Coverage Datasets](coverage_datasets.html)
* `FeatureType.COVERAGE` is the general term for `GRID`, `FMRC`, `SWATH`, `CURVILINEAR` types.
  Previously, `GRID` was used as the general type, now it refers to a specific type of Coverage.
  Affects `FeatureDatasetFactoryManager.open(FeatureType wantFeatureType, ...)`

### Shared Dimensions

* `Group.addDimension` and `Group.addDimensionIfNotExists` methods now throw an `IllegalArgumentException` if the
dimension isn't shared.
* `NetcdfFileWriter.addDimension` methods no longer have an `isShared` parameter. Such dimensions should always be
shared and allowing them to be private is confusing and error-prone.

### Catalog API

* All uses of classes in `thredds.catalog` are deprecated. If you still need these, you must add `legacy.jar` to your path.
* TDS and CDM now use `thredds.server.catalog` and `thredds.client.catalog`. The APIs are different, but with equivalent functionality to thredds.catalog.
* `thredds.client.DatasetNode` now has `getDatasetsLogical()` and `getDatasetsLocal()` that does or does not dereference a `CatalogRef`, respectively.
  You can also use `getDatasets()` which includes a dereferenced catalog if it has already been read.


## TDS Data Services

### Netcdf Subset Service (NCSS)

NCSS queries and responses have been improved and clarified.
Generally the previous queries are backwards compatible. See [NCSS Reference](netcdf_subset_service_ref.html) for details.

New functionality:
* 2D time can now be handled for gridded datasets, with addition of `runtime` and `timeOffset` parameters.
* Handling of interval coordinates has been clarified.
* Use `ensCoord` to select an ensemble member.

Minor syntax changes:
* Use `time=all` instead of `temporal=all`
* For station datasets, `subset=stns` or `subset=bb` is not needed.
  Just define `stns` or a bounding box.


### CdmrFeature Service

A new TDS service has been added for remote access to CDM Feature Datasets.

* Initial implementation for Coverage (Grid, FMRC, Swath) datasets, based on the new Coverage implementation in `ucar.nc2.ft2.coverage`.
* Target is a python client that has full access to all of the coordinate information and coordinate based subsetting capabilities of the Java client.
* Compatible / integrated with the Netcdf Subset Service (NCSS), using the same web API.

### `ThreddsConfig.xml`

You no longer turn catalog caching on or off, but you can control how many catalogs are cached (see
[here](tds_config_ref.html) for the new syntax).

The following is no longer used:

~~~xml
<Catalog>
  <cache>false</cache>
</Catalog>
~~~

* By default, most services are enabled, but may still be turned off in `threddsConfig.xml`.

## Catalogs

### Catalog Schema changes

Schema version is now `1.2`.

### Client Catalogs

* `<service>` elements may not be nested inside of `<dataset>` elements, they must be directly contained in the `<catalog>` element.

### Server Configuration Catalogs

* The `<catalogScan>` element is now available, which scans a directory for catalog files (any file ending in xml)
* The `<datasetFmrc>` element is no longer supported
* `<datasetRoot>` elements may not be contained inside of *service* elements, they must be directly contained in the `<catalog>` element
* `<service>` elements may not be nested inside of `<dataset>` elements, they must be directly contained in the `<catalog>` element.
* `<service>` elements no longer need to be explicitly defined in each config catalog, but may reference user defined global services
* If the `datatype/featureType` is defined for a dataset, then the `<service>` element may be omitted, and the default set of services for that `datatype` will be used.
* The `expires` attribute is no longer used.

### Viewers

* `thredds.servlet.Viewer` has `InvDatasetImpl` changed to `Dataset`
* `thredds.servlet.ViewerLinkProvider` has `InvDatasetImpl` changed to `Dataset`
* `thredds.server.viewer.dataservice.ViewerService` has `InvDatasetImpl` changed to `Dataset`

### DatasetScan

* `addID` is no longer needed, ids are always added
* `addDatasetSize` is no longer needed, the dataset size is always added
* With `addLatest`, the `service` name is no longer used, it is always `Resolver`, and the correct service is automatically added.
  Use `addLatest` attribute for simple case.
* `fileSort`: by default, datasets at each collection level are listed in increasing order by filename.
  To change to decreasing order, use the [fileSort](tds_dataset_scan_ref.html#filesSort,filesSort) element.
* `sort`: deprecated in favor of `filesSort`
* User pluggable classes implementing `UserImplType` (`crawlableDatasetImpl`, `crawlableDatasetFilterImpl`, `crawlableDatasetLabelerImpl`,
`crawlableDatasetSorterImpl`) are no longer supported. (This was never officially released or documented).
* `DatasetScan` details are [here](server_side_catalog_specification.html)

=== Standard Services

* The TDS provides standard service elements, which know which services are appropriate for each Feature Type.
* User defined services in the root catalog are global and can be referenced by name in any other config catalog.
* User defined services in non-root catalogs are local to that catalog and override (by name) any global services.
* All services are enabled unless explicitly disabled
** Except for remote catalog services
* Standard service details are <<reference/Services#,here>>

### FeatureCollections

* The [update](feature_collections_ref.html#update) element default is now `startup="never"`, meaning do not update collection on startup, and use existing indices when the collection is accessed.
* The [fileSort](tds_dataset_scan_ref.html#filesSort,filesSort) element is now inside the `featureCollection` itself, so it can be processed uniformly for all types of feature collections.
  When a collection shows a list of files, the files will be sorted by increasing name.
  To use a decreasing sort, use the element `<filesSort increasing="false" />` inside the `featureCollection` element.
  This supersedes the old way of placing that element in the `<gribConfig>` element, or the older verbose `lexigraphicByName` element:

  ~~~xml
    <filesSort>
      <lexigraphicByName increasing="false" />  // deprecated
    </filesSort>
  ~~~

* Feature Collection details are [here](feature_collections_ref.html)

### Recommendations for 5.0 catalogs

* Put all `<datasetRoot>` elements in root catalog.
* Put all `<catalogScan>` elements in root catalog.
* Use `StandardServices` when possible.
  Annotate your datasets with `featureType` / `dataType`.
* Put all user-defined `<service>` elements in root catalog.
* Only use user-defined `<service>` elements in non-root catalogs when they are experimental or truly a special case.

### Recommendations for ESGF

You must determine the number of datasets that are contained in all of your catalogs.
To get a report, enable [Remote Management](remote_management_ref.html), and from https://server/thredds/admin/debug, select "Make Catalog Report".
This may take 5-20 minutes, depending on the numbers of catalogs.

Add the [<ConfigCatalog>](tds_config_ref.html#configuration-catalog) element to `threddsConfig.xml`:

~~~xml
<ConfigCatalog>
  <keepInMemory>100</keepInMemory>
  <reread>check</reread>
  <dir>/tomcat_home/content/thredds/cache/catalog/</dir>
  <maxDatasets>1000000</maxDatasets>
</ConfigCatalog>
~~~

where:

* `keepInMemory`: using the default value of 100 is probably good enough.
* `reread`: use value of _check_ to only read changed catalogs when restarting TDS.
* `dir` is where the catalog cache files are kept.
  Use the default directory (or symlink to another place) unless you have a good reason to change.
* `maxDatasets`: this is the number you found in step 1.
  Typical values for ESGF are 1 - 7 million.
  This is a maximum, so its ok to make it bigger than you need.

Here are some additional, optional changes you can make to increase maintainability:

1. Place all `datasetRoot` elements in the top catalog
2. Place all `service` elements in the root catalog (_catalog.xml_).
   These can be referenced from any catalog.
3. Remove `<service>` elements from non-root catalogs.
4. Add a [catalogScan](server_side_catalog_specification.html#catalogScan) element to the root catalog, replacing the list of catalogRefs listing all the other catalogs.
* This assumes that other catalogs live in a subdirectory under the root, for example `${tds.content.root.path}/thredds/esgcet/**`.

  For example:

  ~~~xml
  <?xml version='1.0' encoding='UTF-8'?>
  <catalog name="ESGF Master Catalog" version="1.2"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
        xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.2.xsd">

    <datasetRoot location="/esg/data" path="esg_testroot"/>
    <datasetRoot location="/esg/arc/data/" path="esg_obs4MIPs"/>
    <datasetRoot location="/esg/cordex/data/" path="esg_cordex"/>
    <datasetRoot location="/esg/specs/data/" path="esg_specs"/>

    <service base="/thredds/dodsC/" desc="OpenDAP" name="gridded" serviceType="OpenDAP">
      <property name="requires_authorization" value="false"/>
      <property name="application" value="Web Browser"/>
    </service>

    <service base="" name="fileservice" serviceType="Compound">
      <service base="/thredds/fileServer/" desc="HTTPServer" name="HTTPServer" serviceType="HTTPServer">
        <property name="requires_authorization" value="true"/>
        <property name="application" value="Web Browser"/>
        <property name="application" value="Web Script"/>
      </service>

      <service base="gsiftp://cmip-bdm1.badc.rl.ac.uk/" desc="GridFTP" name="GridFTPServer" serviceType="GridFTP">
        <property name="requires_authorization" value="true"/>
        <property name="application" value="DataMover-Lite"/>
      </service>

      <service base="/thredds/dodsC/" desc="OpenDAP" name="OpenDAPFiles" serviceType="OpenDAP">
        <property name="requires_authorization" value="false"/>
        <property name="application" value="Web Browser"/>
      </service>
    </service>

    <catalogScan name="ESGF catalogs" path="esgcet" location="esgcet" />

  </catalog>
  ~~~
