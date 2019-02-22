---
title: CDM utility programs
last_updated: 2018-10-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: cdm_utility_programs.html
---

Below are useful command-line utilities that can be called from the CDM library.
The easiest way to use these is to grab the latest netcdfAll.jar file.

* [ncdump](#ncdump): prints the textual representation of a dataset to standard output
* [nccopy](#nccopy): copies a CDM dataset to a netCDF-3 (default) or netCDF-4 file
* [nccompare](#nccompare): compares two [CDM files](ncj_file_types.html) for semantic equivalence
* [BufrSplitter](#bufrsplitter): copies a BUFR file’s messages into separate output files, depending on type
* [CatalogCrawler](#catalogcrawler): crawl a catalog, optionally open datasets to look for problems
* [CFPointWriter](#cfpointwriter): copies a CDM point feature dataset to CF/NetCDF format
* [GribCdmIndex](#gribcdmindex): write GRIB Collection Indexes
* [FeatureScan](#featurescan): scans a directory to find CDM datasets and determines their FeatureTypes

## ncdump

Prints the textual representation of a dataset to standard output.
Similar functionality to the [`ncdump`](https://www.unidata.ucar.edu/software/netcdf/netcdf-4/newdocs/netcdf/ncdump.html){:target="_blank"} utility program.
By default, just the header (`ncdump -h`) is output.
This application works on any [CDM file](ncj_file_types.html), not just netCDF files.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.NCdumpW
    filepath [-cdl | -ncml] [-c | -vall] [-v varName1;varName2;..] [-v varName(0:1,:,12)]
~~~

where:

* `filepath`: pathname of any CDM file.
* `-cdl`: show CDL (strict mode)
* `-ncml`: show NcML (default)
* `-c`: show data for coordinate variables only
* `-vall`: show data for all variables
* `-v varName1;varName2;..`: show data for these variables, use variable’s full names (including groups if present)
* `-v varName(0:1,:,12)`: show data for a section of this variable only, using FORTRAN 90 section specification


## nccopy

Copies a dataset to a netCDF-3 (default) or netCDF-4 file.
The input may be any CDM dataset, including OPeNDAP URLs, NcML, GRIB files, etc.
If the dataset uses the extended data model, you must write to netCDF-4.
If writing to netCDF-4, you must have the netcdf-4 C library loaded.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.write.Nccopy [options]
  Options:
  * -i, --input
       Input dataset.
  * -o, --output
       Output file.
    -f, --format
       Output file format. Allowed values = [netcdf3, netcdf4, netcdf4_classic,
       netcdf3c, netcdf3c64, ncstream]
       Default: netcdf3
    -isLargeFile, --isLargeFile
       Write to large file format. Only used in NetCDF 3. Allowed values =
       [standard, grib, none]
       Default: false
    -st, --strategy
       Chunking strategy. Only used in NetCDF 4. Allowed values = [standard,
       grib, none]
       Default: standard
    -d, --deflateLevel
       Compression level. Only used in NetCDF 4. Allowed values = 0 (no
       compression, fast) to 9 (max compression, slow)
       Default: 5
    -sh, --shuffle
       Enable the shuffle filter, which may improve compression. Only used in
       NetCDF 4. This option is ignored unless a non-zero deflate level is specified.
       Default: true
    -h, --help
       Display this help and exit
       Default: false
~~~

## nccompare

Compares two [CDM files](ncj_file_types.html) for semantic equivalence.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.util.CompareNetcdf2
    file1 file2 [-showEach] [-compareData]
~~~

where
* `-showEach`: show details of comparing each object
* `-compareData`: compare data also
* `file1`: first file to compare
* `file2`: second file to compare

## BufrSplitter

Copies a BUFR file\'s messages into separate output files, depending on message type.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.iosp.bufr.writer.BufrSplitter
    --fileSpec <fileIn> --dirOut <dirOut>
~~~

where
* `--fileSpec`: file to split
* `--dirOut`: output directory of split operation

## CFPointWriter

Copies a CDM point feature dataset to CF/NetCDF format.
The CF conventions target NetCDF-3, but you can also write NetCDF-4 files in classic mode.
For that, you must first install the C library.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.ft.point.writer.CFPointWriter [options]
  Options:
  * -i, --input
       Input file.
  * -o, --output
       Output file.
    -f, --format
       Output file format. Allowed values = [netcdf3, netcdf4, netcdf4_classic,
       netcdf3c, netcdf3c64, ncstream]
       Default: netcdf3
    -st, --strategy
       Chunking strategy. Only used in NetCDF 4. Allowed values = [standard,
       grib, none]
       Default: standard
    -d, --deflateLevel
       Compression level. Only used in NetCDF 4. Allowed values = 0 (no
       compression, fast) to 9 (max compression, slow)
       Default: 5
    -sh, --shuffle
       Enable the shuffle filter, which may improve compression. Only used in
       NetCDF 4. This option is ignored unless a non-zero deflate level is specified.
       Default: true
    -h, --help
       Display this help and exit
       Default: false
~~~

## GribCdmIndex

Write GRIB Collection Indexes from an XML file containing a [GRIB `<featureCollection>`](grib_feature_collections_ref.html) XML element.

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.grib.collection.GribCdmIndex [options]
  Options:
  * -fc, --featureCollection
       Input XML file containing <featureCollection> root element
    -update, --CollectionUpdateType
       Collection Update Type
       Default: always
    -h, --help
       Display this help and exit
       Default: false
~~~

Example:

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.grib.collection.GribCdmIndex -fc /data/fc/gfs_example.xml
~~~

Note that the output file is placed in the root directory of the collection, as specified by the [Collection Specification](collection_spec_string_ref.html) of the GRIB [`<featureCollection>`](feature_collections_ref.html).

## FeatureScan

Scans all the files in a directory to see if they are [CDM files](ncj_file_types.html) and can be identified as a particular feature type.

~~~basj
java -Xmx1g -classpath netcdfAll-<version>.jar ucar.nc2.ft.scan.FeatureScan directory [-subdirs]
~~~

where

* `directory`: scan this directory
* `-subdirs`: recurse into subdirectories

## CatalogCrawler

Crawl a catalog, optionally open datasets to look for problems.

~~~bash
Usage: thredds.client.catalog.tools.CatalogCrawler [options]
  Options:
  * -cat, --catalog
       Top catalog URL
    -t, --type
       type of crawl. Allowed values=[all, all_direct, first_direct,
       random_direct, random_direct_middle, random_direct_max]
       Default: all
    -o, --openDataset
       try to open the dataset
       Default: false
    -skipScans, --skipScans
       skip DatasetScans
       Default: true
    -catrefLevel, --catrefLevel
       skip Catalog References > nested level
       Default: 0
    -sh, --showNames
       show dataset names
       Default: false
    -h, --help
       Display this help and exit
       Default: false
~~~

This example will crawl the named catalog, two levels of Catalog References, and try to open all datasets it finds, skipping any DatasetScans:

~~~bash
java -Xmx1g -classpath netcdfAll-<version>.jar thredds.client.catalog.tools.CatalogCrawler
  --catalog http://thredds.ucar.edu/thredds/catalog/catalog.xml  --catrefLevel 2 --openDataset
~~~

results:

~~~bash
thredds.client.catalog.tools.CatalogCrawler
   topCatalog='http://thredds.ucar.edu/thredds/catalog/catalog.xml'
   type=all, showNames=false, skipDatasetScan=true, catrefLevel=2, openDataset=true

 Catalog <http://thredds.ucar.edu/thredds/catalog/catalog.xml> read ok

   CatalogRef http://thredds.ucar.edu/thredds/catalog/idd/forecastModels.xml (Forecast Model Data)

         CatalogRef http://thredds.ucar.edu/thredds/catalog/grib/NCEP/DGEX/CONUS_12km/catalog.xml (DGEX CONUS 12km)
   Dataset 'Full Collection (Reference / Forecast Time) Dataset' opened as type=FMRC
   Dataset 'Best DGEX CONUS 12km Time Series' opened as type=GRID
   Dataset 'Latest Collection for DGEX CONUS 12km' opened as type=GRID

         CatalogRef http://thredds.ucar.edu/thredds/catalog/grib/NCEP/DGEX/Alaska_12km/catalog.xml (DGEX Alaska 12km)
   Dataset 'Full Collection (Reference / Forecast Time) Dataset' opened as type=FMRC
   Dataset 'Best DGEX Alaska 12km Time Series' opened as type=GRID
   Dataset 'Latest Collection for DGEX Alaska 12km' opened as type=GRID

 ...

that took 327204 msecs
count catalogs = 76
count catrefs  = 4831
count skipped  = 33
count datasets = 252
count filterCalls = 5516

count open = 215
count fail = 3
count failException = 0
~~~