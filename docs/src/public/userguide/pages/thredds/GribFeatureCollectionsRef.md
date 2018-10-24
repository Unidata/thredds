---
title: GRIB Feature Collections
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: grib_feature_collections_ref.html
---

## Overview

GRIB Feature Collection Datasets are collections of GRIB records, which contain gridded data, typically from numeric model output.
Because of the complexity of how GRIB data is written and stored, the TDS has developed specialized handling of GRIB datasets called **GRIB Feature Collections**.

* The user specifies the collection of GRIB-1 or GRIB-2 files, and the software turns them into a dataset.
* The indexes, once written, allow fast access and scalability to very large datasets.
* Multiple horizontal domains are supported and placed into separate groups.
* Interval time coordinates are fully supported.

Also see:

* [Feature Collection overview](feature_collections_ref.html)
* [GRIB specific configuration](grib_collection_config_ref.html)
* [GRIB Collection FAQ](tds_grib_faq.html)
* [GRIB Feature Collection Tutorial](grib_feature_collections.html)
* [Partitions](partitions_ref.html)
* [CDM GRIB Collection Processing](grib_files_cdm.html)

### Multiple Dataset Collections

When a GRIB Collection contains multiple runtimes, and the valid times (forecast times) overlap, a `TwoD` time dataset is created.
From that, a `Best` time dataset is also created.

### Multiple Groups

When a GRIB Collection contains multiple horizontal domains (i.e. distinct Grid Definition Sections (GDS)), each domain gets placed into a seperate group (CDM) or Dataset (TDS).

### Generated URLs

Collection endpoints are of the form:

`path/partitionName/groupName`

where:

* `path` : collection path
* `partitionName` : used to disambiguate multiple dataset types within a collection: _TwoD or Best
* `groupName` : used only when there are multiple groups (horizontal coordinates): group name or empty

## History

### Version 4.5

The GRIB Collections framework has been rewritten in CDM version 4.5, in order to handle large collections efficiently.
Some of the new capabilities in version 4.5 are:

* GRIB Collections now keep track of both the `reference` time and `valid` time.  
  The collection is partitioned by reference time.
* A collection with a single reference time will have a single partition with a single time coordinate.
*  A collection with multiple reference times will have partitions for each reference time, plus a `PartitionCollection` that represents the entire collection.
   Very large collections should be partitioned by directory and/or file, creating a tree of partitions.
* A `PartitionCollection` has two datasets (kept in separate groups): the `TwoD` and the `Best` dataset.
* The `TwoD` dataset has two time coordinates - `reference` time (a.k.a. _run time_) and `forecast` time (a.k.a. _valid time_), and corresponds to the FMRC `TwoD` dataset. 
  The `forecast` time is two dimensional, corresponding to all the times available for each reference time.
* The `Best` dataset has a single `forecast` time coordinate, the same as 4.3 GRIB Collections and FMRC Best datasets.
  If there are multiple GRIB records corresponding to the same forecast time, the record with the smallest offset from its reference time is used.

Implementation notes:

* The `featureType` attribute is now `GRIB1` or `GRIB2`.
* For each GRIB file, a grib index is written, named `<grib filename>.gbx9`. 
  Once written, this never has to be rewritten.
* For each `reference` time, a cdm index is written, named `<collection.referenceTime>.ncx2`. 
  This occasionally has to be rewritten when new CDM versions are released, or if you modify your GRIB configuration.
* For each `PartitionCollection`, a cdm index is written named `<collection name>.ncx2`. 
  This must be rewritten if any of the collection files change.
* The cdm indexing uses extension **.ncx2**, in order to coexist with the .ncx indexes of previous versions.
  If you are upgrading to 4.5, and no longer running earlier versions, _remove_ the ncx files (but save the `gbx9` files).
* For large collections, especially if they change, the THREDDS Data Manager (TDM) must be run as a separate process to update the index files.
  Generally it is strongly recommended to run the TDM, and configure the TDS to only read and never write the indexes.
* Collections in the millions of records are now feasible.
  Java 7 NIO2 package is used to efficiently scan directories.

### Version 4.6

The GRIB Collections framework has been rewritten in CDM version 4.6, in order to handle very large collections efficiently.
Oh wait we already did that in 4.5.
Sorry, it wasn\'t good enough.

* `TimePartition` can now be set to `directory` (default), `file`, a time period, or `none`. 
  Details [here](partitions_ref.html).
* Multiple `reference` times are handled more efficiently, e.g. only one index file typically needs to be written.
* Global attributes are promoted to dataset properties in the catalog
* Internal changes:
  * Internal memory use has been reduced.
  * Runtime objects are now immutable, which makes caching possible.
  * `RandomAccessFiles` are kept in a separate pool, so they can be cached independent of the Collection objects.

### Version 5.0

The GRIB Collections framework has been significantly improved in CDM version 5.0, in order to handle very large collections efficiently.
* Collection index files now use the suffix **.ncx4**. 
  These will be rewritten first time you access the files.
* The gbx9 files do NOT need to be rewritten, which is good because those are the slow ones.
* Defaults
  * You no longer need specify the `dataFormat` or `dataType`, as these are automatically added
  * It is recommended to not specify the set of `services` used, but accept the default set of services.
 
