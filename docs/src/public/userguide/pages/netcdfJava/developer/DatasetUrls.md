---
title: Dataset URLs
last_updated: 2018-10-10
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: ncj_dataset_urls.html
---

The netCDF-Java library can read datasets from a variety of sources.
The dataset is named using a Uniform Resource Location (URL).
This page summarizes the netCDF-Java API use of URLs.


Special Note: When working with remote data services, it's important to note that not all servers handle encoded URLs.
By default, netCDF-Java will encode illegal URI characters using percent encoding (e.g. `[` will become `%5B`).
If you find you are having trouble accessing a remote dataset due to the encoding, set the java System Property `httpservices.urlencode` to `"false"` using, for example `System.setProperty("httpservices.urlencode", "false");`.

## `ucar.nc2.NetcdfFile.open(String location)`

### Local Files

`NetcdfFile` can work with local files, e.g:

* `/usr/share/data/model.nc`
* `file:/usr/share/data/model.nc`
* `file:C:/share/data/model.nc` (NOTE we advise using forward slashes **everywhere**, including Windows)
* `data/model.nc` (relative to the current working directory)

When using a file location that has an embedded `:` char, eg `C:/share/data/model.nc`, it\'s a good idea to add the `file:` prefix, to prevent the `C:` from being misinterpreted as a URL schema.

### Remote Files

`NetcdfFile` can open HTTP remote files, [served over HTTP](ncj_read_over_http.html
), for example:

* http://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.nc

The HTTP server must implement the getRange header and functionality.
Performance will be strongly affected by file format and the data access pattern.

To disambiguate HTTP remote files from OPeNDAP or other URLS, you can use `httpserver:` instead of `http:`, e.g.:

* `httpserver://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.nc`

### File Types

The local or remote file must be one of the [formats](ncj_file_types.html) that the netCDF-Java library can read.
We call this set of files Common Data Model files, or CDM files for short, to make clear that the NetCDF-Java library is not limited to netCDF files.

If the URL ends with a with `.Z`, `.zip`, `.gzip`, `.gz`, or `.bz2`, the file is assumed to be compressed.
The netCDF-Java library will uncompress/unzip and write a new file without the suffix, then read from the uncompressed file. 
Generally it prefers to place the uncompressed file in the same directory as the original file.
If it does not have write permission on that directory, it will use the [cache directory](ncj_disk_caching.html) defined by `ucar.nc2.util.DiskCache`.

## `ucar.nc2.dataset.NetcdfDataset.openFile(String location)`

`NetcdfDataset` adds another layer of functionality to the CDM data model, handling other protocols and optionally enhancing the dataset with Coordinate System information, scale/offset processing, dataset caching, etc.

* `openFile()` can open the same datasets as `NetcdfFile`, plus those listed below.
* `openDataset()` calls `NetcdfDataset.openFile()`, then optionally enhances the dataset.
* `acquireDataset()` allows dataset objects to be cached in memory for performance.

### OPeNDAP datasets

`NetcdfDataset` can open OPeNDAP datasets, which use a `dods:` or `http:` prefix, for example:

* `http://thredds.ucar.edu/thredds/dodsC/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1`
* `dods://thredds.ucar.edu/thredds/models/NCEP/GFS/Global_5x2p5deg/GFS_Global_5x2p5deg_20070313_1200.nc`

To avoid confusion with remote HTTP files, OPeNDAP URLs may use the `dods:` prefix.
Also note that when passing an OPeNDAP dataset URL to the netCDF-Java library, do not include any the access suffixes, e.g. `.dods`, `.ascii`, `.dds`, etc.

For an `http:` URL, we make a `HEAD` request, and if it succeeds and returns a header with `Content-Description="dods-dds"` or `"dods_dds"`, then we open as OPeNDAP.
If it fails we try opening as an HTTP remote file.
Using the `dods:` prefix makes it clear which protocol to use.

### NcML datasets

`NetcdfDataset` can open NcML datasets, which may be local or remote, and must end with a `.xml` or `.ncml` suffix, for example:

* `/usr/share/data/model.ncml`
* `file:/usr/share/data/model.ncml`
* `https://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.xml`

Because xml is so widely used, we recommend using the `.ncml` suffix when possible.

### THREDDS Datasets

`NetcdfDataset` can open THREDDS datasets, which are contained in THREDDS Catalogs.
The general form is:

`thredds:catalogURL#dataset_id`

where `catalogURL` is the URL of a THREDDS catalog, and `dataset_id` is the `ID` of a dataset inside of that catalog.
The `thredds:` prefix ensures that it is understood as a THREDDS dataset.
Examples:

* `thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly`
* `thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData`

In the first case, `http://localhost:8080/test/addeStationDataset.xml` must be a catalog containing a dataset with `ID` `surfaceHourly`.
The second case will open a catalog located at `c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml` and find the dataset with `ID` `AddeSurfaceData`.

`NetcdfDataset` will examine the thredds dataset object and extract the dataset URL, open it and return a `NetcdfDataset`.
If there are more than one dataset access URL, it will choose a service that it understands.
You can modify the preferred services by calling `thredds.client.catalog.tools.DataFactory.setPreferAccess()`.
The dataset metadata in the THREDDS catalog may be used to augment the metadata of the `NetcdfDataset`.

### THREDDS Resolver Datasets

`NetcdfDataset` can open THREDDS Resolver datasets, which have the form

`thredds:resolve:resolverURL`

The `resolverURL` must return a catalog with a single top level dataset, which is the target dataset.
For example:

`thredds:resolve:https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/latest.xml`

In this case, `https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/latest.html` returns a catalog containing the latest dataset in the `grib/NCEP/GFS/Global_0p25deg` collection. 
`NetcdfDataset` will read the catalog, extract the THREDDS dataset, and open it as in section above.

### CdmRemote Datasets

`NetcdfDataset` can open [CDM Remote](ncj_cdmremote.html) datasets, with the form

`cdmremote:cdmRemoteURL`

for example

* `cdmremote:http://server:8080/thredds/cdmremote/data.nc`

The `cdmRemoteURL` must be an endpoint for a `cdmremote` web service, which provides index subsetting on remote CDM datasets.

### DAP4 datasets

`NetcdfDataset` can open datasets through the DAP4 protocol.
The url should either begin with `dap4:` or `dap4:http:`.
Examples:

* `dap4:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1`
* `dap4://thredds.ucar.edu:8080/thredds/models/NCEP/GFS/Global_5x2p5deg/GFS_Global_5x2p5deg_20070313_1200.nc`

To avoid confusion with other protocols using HTTP URLs, DAP4 URLs are often converted to use the `dap4:` prefix.
Also note that when passing a DAP4 dataset URL to the netCDF-Java library, do not include any of the access suffixes, e.g. `.dmr`, `.dap`, `.dst`, etc.


## `ucar.nc2.ft.FeatureDatasetFactoryManager.open()`

`FeatureDatasetFactory` creates [Feature Datasets](ncj_feature_datasets.html) for Coverages (Grids), Discrete Sampling Geometry (Point) Datasets, Radial Datasets, etc.
These may be based on local files, or they may use remote access protocols.

`FeatureDatasetFactoryManager` can open the same URLs that `NetcdfDataset` and `NetcdfFile` can open, plus the following:

### CdmrFeature Datasets

`FeatureDatasetFactoryManager` can open [CdmRemote Feature Datasets](ncj_cdmremote_feature_datasets.html), which have the form

`cdmrFeature:cdmrFeatureURL`

for example:

* `cdmrFeature:http://server:8080/thredds/cdmremote/data.nc`

The `cdmrFeatureURL` must be an endpoint for a `cdmrFeature` web service, which provides coordinate subsetting on remote Feature Type datasets.

## THREDDS Datasets

`FeatureDatasetFactoryManager` can also open CdmRemote Feature Datasets, by passing in a dataset `ID` in a catalog, exactly as in `NetcdfDataset.open` as explained above.
The general form is

`thredds:catalogURL#dataset_id`

where `catalogURL` is the URL of a THREDDS catalog, and `dataset_id` is the `ID` of a dataset inside of that catalog.
The `thredds:` prefix ensures that the URL is understood as a THREDDS catalog and dataset.
Example:

* `thredds:http://localhost:8081/thredds/catalog/grib.v5/gfs_2p5deg/catalog.html#grib.v5/gfs_2p5deg/TwoD`

If the dataset has a `cdmrFeature` service, the `FeatureDataset` will be opened through that service.
This can be more efficient than opening the dataset through the index-based services like `OPeNDAP` and `cdmremote`.

### Collection Datasets

`FeatureDatasetFactoryManager` can open collections of datasets specified with a [collection specification string](collection_spec_string_ref.html).
This has the form

`collection:spec`

`FeatureDatasetFactoryManager` calls `CompositeDatasetFactory.factory(wantFeatureType, spec)` if found, which returns a `FeatureDataset`.
Currently only a limited number of Point Feature types are supported. This is an experimental feature.

### NcML referenced datasets

NcML datasets typically reference other CDM datasets, using the `location` attribute of the `netcdf` element, for example:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
     location="file:/dev/netcdf-java-2.2/test/data/example1.nc">
...
~~~

The `location` is passed to `ucar.nc2.dataset.NetcdfDataset.openFile()`, and so can be any valid CDM dataset location.
In addition, an NcML referenced dataset location can be relative to the NcML file or the working directory:

* A relative URL resolved against the NcML location (eg `subdir/mydata.nc`).
  You must not use a `file:` prefix in this case.
* An absolute file URL with a relative path (eg `file:data/mine.nc`). 
  The file will be opened relative to the working directory.

There are a few subtle differences between using a location in NcML and passing a location to the `NetcdfDataset.openFile()` and related methods:

* In NcML, you **MUST** always use forward slashes in your paths, even when on a Windows machine.
   For example: `file:C:/data/mine.nc`. `NetcdfFile.open()` will accept backslashes on a Windows machine.
* In NcML, a relative URL is resolved against the NcML location.
  In `NetcdfFile.open()`, it is interpreted as relative to the working directory.

### NcML scan location

NcML aggregation `scan` elements use the `location` attribute to specify which directory to find files in, for example:

~~~xml
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <aggregation dimName="time" type="joinExisting">
    <scan location="/data/model/" suffix=".nc" />
  </aggregation>
</netcdf>
~~~

Allowable forms of the location for the scan directory are:
* `/usr/share/data/`
* `file:/usr/share/data/`
* `file:C:/share/data/model.nc` (NOTE we advise using forward slashes everywhere, including Windows)
* `data/model.nc` (relative to the NcML directory)
* `file:data/model.nc` (relative to the current working directory)

When using a directory location that has an embedded `:` char, e.g. `C:/share/data/model.nc`, its a really good idea to add the `file:` prefix, to prevent the `C:` from being misinterpreted as a URI schema.

Note that this is a common mistake:

`<scan location="D:\work\agg" suffix=".nc" />`

on a Windows machine, this will try to scan `D:/work/agg/D:/work/agg`.
Use

`<scan location="D:/work/agg" suffix=".nc" />`

or better

`<scan location="file:D:/work/agg" suffix=".nc" />`