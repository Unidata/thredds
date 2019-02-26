---
title: GRIB Files in the CDM
last_updated: 2018-10-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: grib_files_cdm.html
---

## Overview

As of CDM version 4.3, GRIB datasets are handled as collections of GRIB files.
A GRIB file is an unordered collection of GRIB records.
A GRIB dataset is a therefore a collection of GRIB records in one or more files.
A GRIB dataset can only operate on local files.
A THREDDS Data Server (TDS) can make GRIB datasets remotely accessible, e.g. through OPeNDAP, WMS, or the NetCDF Subset Service (NCSS).

The CDM can only read GRIB files, it cannot write them.
It can, however, rewrite GRIB into netCDF using CF Conventions.
Before version 4.3.13, it can only write netCDF-3 format files, which are typically 4-20 times larger than GRIB.
As of 4.3.13, the CDM can [write to netCDF-4](netcdf4_c_library.html) format, with file sizes comparable to GRIB, typically within a factor of two.

A GRIB collection must follow these _homogeneity constraints_:

* The GRIB records must be either GRIB-1 or GRIB-2, you cannot mix different editions in the same collection.
* The GRIB collection should be coherent, e.g. from the same model (you can mix multiple runs from the same model, however). 
  This is because the user does not have access to the metadata in the individual records, but only to global and variable attributes describing the collections of GRIB records.
* The GRIB records should all be from the same center and subcenter, since these are used for table lookups. 
  In principle, one could relax this if all records use only standard WMO entries.
  The global metadata may be misleading, however.
  Different table versions may be mixed in the same collection in GRIB-1.
* The GRIB records may have different reference dates. (This was not true in versions before 4.3)

In addition:

* A best practice is that all GRIB records in the collection should use the same Grid Definition (GDS).
  If there is more than one GDS in the collection, each GDS will be placed in a separate CDM group.
  This can be a problem for older software that doesn\'t deal with groups.
* Global attributes are taken from a single record, and so may be misleading if these vary within the collection.
  For example:
  * The originating center and subcenter.
  * The master and local table version (GRIB-2).
  * The generating process type.
  * The generating and background process name, if known.

## Indexing

For each GRIB file, a GRIB index file is written with suffix **.gbx9**.
This file contains everything in the GRIB file except the data.
Generally it is 300-1000 times smaller than the original file.
Once written, it typically never has to be rewritten.
If the GRIB file changes, the CDM should detect that and rewrite the index file.
If there is any doubt about that, delete the index file and let it get recreated.

For each GRIB collection, a CDM collection index file is written with suffix **.ncx4**.
This file contains all the metadata and the coordinates for the collection.
It is usually fairly small (a few dozen KBytes to a few MBytes for a large collection), and once created, makes accessing the GRIB collection very fast.
In general it will be updated if needed, but one can always delete it and let it be recreated.

If one opens a single GRIB file in the CDM, a **gbx9** and **ncx4** file will be created for that file. If one opens a collection of multiple GRIB files, a **gbx9** file is created for each file, and one **ncx4** file is created for the entire collection.

Both kinds of index files are binary, private formats for the CDM, whose format may change as needed.
Your application should not depend in any way on the details of these formats.

### Moving GRIB files

When GRIB index files (_gbx9_) are created, they store the name of the GRIB data file.
However, this is not used except for debugging.
So you can move the data files and the _gbx_ files as needed.
The CDM index files (_ncx4_) also store the names of the GRIB data files, and (usually) needs the GRIB files to exist there.
So if you move the GRIB and GRIB index files, it\'s best to delete the _ncx4_ files and re-create them after the move.

The use of external tables in GRIB is quite problematic (read here for more details). Nonetheless, GRIB files are in wide use internationally and contain invaluable data. The CDM is a general-purpose GRIB reading library that makes GRIB data available through the CDM/NetCDF API, that is, as multidimensional data arrays and CF-compliant metadata and coordinates.

## GRIB Tables

The use of external tables in GRIB is quite problematic (read {% include link_file.html file="https://doi.org/10.5065/vkan-dp10" text="here" %} for more details).
Nonetheless, GRIB files are in wide use internationally and contain invaluable data.
The CDM is a general-purpose GRIB reading library that makes GRIB data available through the CDM/NetCDF API, that is, as multidimensional data arrays and [CF](http://cfconventions.org/){:target="_blank"}-compliant metadata and coordinates.

Because of flaws in the design of GRIB and flaws in actual practice when writing GRIB, any general purpose GRIB reader can only make a best effort in interpreting arbitrary GRIB records. 
It is therefore necessary, for anything other than casual use, to carefully examine the output of CDM GRIB datasets and compare this against the documentation.
In particular, GRIB records may refer to local tables that are missing or incorrect in the CDM, and they may override standard WMO tables without the CDM being able to detect that they are doing so.
It is often necessary for users to contact the data producer to obtain the correct tables for the particular dataset they want to read.
This is also necessary for other GRIB reading tools like [wgrib](http://www.cpc.ncep.noaa.gov/products/wesley/wgrib.html){:target="_blank"} (NCEP) and [ecCodes](https://confluence.ecmwf.int/display/ECC){:target="_blank"} (ECMWF).

The CDM has a [number of ways](grib_tables_cdm.html) to allow you to use new tables or override incorrect ones globally or by dataset.
The good news is that if users contribute these fixes back to the CDM, everyone can take advantage of them and the set of \"correct\" datasets will grow.
The WMO has greatly improved the process of using the standard tables, and hopefully GRIB data producers will continue to [improve methods for writing GRIB](grib_tables_cdm.html) and maintaining local tables.

## Opening a GRIB Collection in the CDM

The CDM is used primarily to open single GRIB files, and the TDS is used to manage large and very large collections of files.
Here is a summary of the ways that an application might use the CDM to open GRIB files.

### Single Data File Mode

Pass the local data file location to any of the standard dataset opening classes:
* `ucar.nc2.NetcdfFile.open(String location)`
* `ucar.nc2.dataset.NetcdfDataset.openFile(String location)`
* `ucar.nc2.dt.grid.GridDataset.open(String location)`
* `ucar.nc2.ft.FeatureDatasetFactoryManager.open(FeatureType.GRID, String location, CancelTask task, Formatter errlog);`

The GRIB Index (**.gbx9**) and GRIB Collection index (**.ncx4**) files will be created as needed.

### Collection Index Mode

If the GRIB Collection index (**.ncx4**) already exists, one can pass that to any of the standard dataset opening classes.
In this case, the collection is created from reading the ncx4 file with no checking against the original data file(s).
The original data files are only accessed when data is requested from them.
Be careful not to move the data files once the index files are created.
If you do need to move the data files, its best to recreate the Collection index files (ncx4).

### Creating a GRIB Collection Index

You can use a [command line tool](cdm_utility_programs.html#gribcdmindex) that uses a complete [GRIB `<featureCollection>` element](grib_feature_collections_ref.html) to define the GRIB Collection, and generates the CDM index (ncx4) file.

For simple cases, you can create the ncx4 file based on a collection spec using ToolsUI: `IOSP/GRIB1(2)/GribCollection`.
Enter the collection spec and hit Enter.
To write the index file, hit the \"Write Index\" button on the right.
Give it a memorable name and hit Save.
It\'s is currently not possible to pass GRIB Collection Configuration elements in this way.

### NcML Aggregation

**NcML Aggregations on GRIB files are not supported in versions 4.3 and above. You must use GRIB collections.**

In versions 4.2 and before, Grib files were typically aggregated using NcML Aggregations. While this could work if the GRIB files were truly homogenous, in practice this often has problems; the aggregation would appear ok, but in fact be incorrect in various subtle ways. This was one of the motivations for developing GRIB collections, which collects the GRIB records into multidimensional arrays and can (mostly) figure out the right thing to do without user intervention.

### Using NcML to pass GRIB Collection Configuration options

You can use NcML to open a **single** GRIB file, and modify the way GRIB records are processed.
All of the configuration options that you can use inside the TDS `<gribConfig>` element can be used inside the `<iospParam>` element of the NcML, for example:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="E:/ncep/NDFD_CONUS_5km_conduit_20120119_1800.grib2">
 <iospParam>
   <gdsHash from="-2121584860" to="28944332"/>
   <pdsHash>
     <useTableVersion>true</useTableVersion>
   </pdsHash>
 </iospParam>
</netcdf>
~~~

See [GRIB Collection Configuration](grib_collection_config_ref.html) for a description of all of the options.

Note that you **cannot** use NcML to open a **collection** of GRIB files.
You must generate the GRIB Collection index file in a [separate step](#creating-a-grib-collection-index).

## Mapping a GRIB Collection into Multidimensional Variables

A GRIB file is an unordered collection of GRIB records.
A GRIB record consists of a single 2D (x, y) slice of data.
The CDM library reads a GRIB file and creates a 2, 3,4, or 5 dimension Variable (time, ensemble, z, y, x), by finding the records with the same parameter, with different time / level / ensemble coordinates.
This amounts to [guessing the dataset schema](https://www.unidata.ucar.edu/blogs/developer/en/entry/dataset_schemas_are_lost_in){:target="_blank"} and the intent of the data provider, and is unfortunately a bit arbitrary. 
Most of our testing is against the NCEP operational models from the [IDD](https://www.unidata.ucar.edu/projects/index.html#idd){:target="_blank"}, and so are influenced by those.
Deciding how to group the GRIB records into CDM Variables is one of the main source of problems.
It uses the following GRIB fields to construct a unique variable.

### GRIB-1 Variables
* Table Version (octet 4 of PDS)
* Parameter Number (octet 9 of PDS)
* if (param > 127) the Center and Subcenter ids
* Level Type (octet 10 of PDS)
* if a vertical layer
* if a time interval, the Statistical Process Type (octet 21 of PDS)
*  the GDS hashcode

The GRIB-1 variable name is:

`%paramName[_%level][_layer][_%interval][_%statName]`

where:
* `%paramName` = parameter name from GRIB-1 table 2 (cleaned up).
  If unknown, use `VAR_%d-%d-%d-%d` (see below)
* `%level` = short form of level name from GRIB-1 table 3, if defined.
* `_layer` = added if its a vertical layer (literal)
*  `%timeInterval` = time interval name (eg "12_hour" or "mixed")
*  `%statName` = name of statistical type if applicable, from GRIB-1 table 5

The GRIB-1 variable id is:

`VAR_%d-%d-%d-%d[_L%d][_layer][_I%s][_S%d]`

where:
*  `%d-%d-%d-%d` = center-subcenter-tableVersion-paramNo
*  `L%d` = level type  (octet 10 of PDS), if defined.
*  `_layer` = added if its a vertical layer (literal)
*  `I%s` = interval name (eg "12_hour" or "mixed") if a time interval
*  `S%d` = stat type (octet 21 of PDS) if applicable

### GRIB-2 Variables

* PDS Template
* Parameter Discipline, Category, Number
* if local tables are used, the Center and Subcenter ids
* Level Type 1
* if a vertical layer
* if a time interval, the Statistical Process Type (Code table 4.10)
* if a probability, the Probability Type (Code table 4.9)
* if it exists, the Derived forecast Type (Code table 4.7)
* if the generating process type is 6 or 7 (meaning of parameter changes to "parameter error")
* the GDS hashcode

The GRIB-2 variable name is:

`%paramName[_error][_%level][_layer][_%interval][_%statName][_%ensDerivedType][_probability_%probName]`

where:
* `%paramName` = parameter name from GRIB-2 table 4.2 (cleaned up). 
  If unknown, use `VAR_%d-%d-%d_FROM%d-%d` = `VAR_discipline-category-paramNo_FROM_center-subcenter`
* `%level` = short form of level name from GRIB-2 table 4.5, if defined.
* `_layer` = added if its a vertical layer (literal)
* `%timeInterval` = time interval name (eg \"12_hour\" or \"mixed\")
* `%statName` = name of statistical type if applicable, from GRIB-2 table 4.10
* `%ensDerivedType` = name of ensemble derived type if applicable, from GRIB-2 table 4.7
* `%probName` = name of probability type if applicable

The GRIB-2 variable id is:

`VAR_%d-%d-%d[_error][_L%d][_layer][_I%s_S%d][_D%d][_Prob_%s]`

where:
* `VAR_%d-%d-%d` = `discipline-category-paramNo`
* `L%d` = level type code
* `I%s` = time interval name (e.g. \"12_hour\" or \"mixed\")
* `S%d` = statistical type code if applicable
* `D%d` = derived type code if applicable
* `Prob_%s` = probability name if applicable

See `ucar.nc2.grib.grib1.Grib1Rectilyser.cdmVariableHash()` and `ucar.nc2.grib.grib2.Grib2Rectilyser.cdmVariableHash()` for complete details.

## Lower level interface to GRIB files

One can use the CDM to process GRIB records individually, without building the CDM multidimensional variables.
Note that this functionality is not part of a supported public API, and is subject to change. 
However these APIs are relatively stable.

For GRIB1 reading, use the classes in `ucar.nc2.grib.grib1`:

~~~java
RandomAccessFile raf = new RandomAccessFile(filepath, "r");
Grib1RecordScanner reader = new Grib1RecordScanner(raf);
while (reader.hasNext()) {
    ucar.nc2.grib.grib1.Grib1Record gr1 = reader.next();
    // do good stuff
}
raf.close();
~~~

or similarly for GRIB2, use the classes in `ucar.nc2.grib.grib2`:

~~~java
RandomAccessFile raf = new RandomAccessFile(filepath, "r");
Grib2RecordScanner scan = new Grib2RecordScanner(raf);
while (scan.hasNext()) {
    ucar.nc2.grib.grib2.Grib2Record gr2 = scan.next();
    // do stuff
}
raf.close();
~~~

The details vary a bit between GRIB1 and GRIB2.
To read the data from a GRIB1 record:

~~~java
float[] data = gr1.readData(raf);
~~~

To read the data from a GRIB2 record:

~~~java
Grib2SectionDataRepresentation drs = gr2.getDataRepresentationSection();
float[] data = gr2.readData(raf, drs.getStartingPosition());
~~~