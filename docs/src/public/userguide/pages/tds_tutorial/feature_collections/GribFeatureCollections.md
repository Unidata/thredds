---
title: GRIB Feature Collections Tutorial
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: grib_feature_collections.html
---

tds/tutorial/GRIBFeatureCollectionTutorial.html

{% include image.html file="sl_website-under-construction.jpeg" alt="Under Construction" caption="Under Construction" %}

## GRIB Feature Collection

The featureCollection element is a way to tell the TDS to serve collections of [CDM Feature Datasets](ncj_feature_datasets.html).
Currently this is used mostly for[gridded data](ncj_grid_data_type.html) whose time and spatial coordinates are recognized by the CDM software stack.
In this tutorial, we will work with featureCollection for collections of GRIB files.

## Creating a GRIB Feature Collection

Download {% include link_file.html file="tds_tutorial/grib/catalogGribfc.xml" text="catalogGribfc.xml" %}, place it in `${tds.content.root.path}/thredds` directory and add a `catalogRef` to it from your main catalog.
Heres the first feature collection in it:

~~~xml
<featureCollection name="FNL" featureType="GRIB1" path="gribfc/LocalFNLCollection"> <!-- 1 -->
  <metadata inherited="true"> <!-- 2 -->
    <serviceName>all</serviceName> <!-- 3 -->
    <documentation type="summary">LOCAL FNL's TO TEST TIME PARTITION</documentation> <!-- 4 -->
  </metadata>
  <collection name="ds083.2" <!-- 5 -->
              spec="<path-to-data>/data/gribfc_tutorial/basic/ds083.2/data/**/fnl_.*_00_c$"
              timePartition="directory" <!-- 6 -->
              dateFormatMark="#fnl_#yyyyMMdd_HH" /> <!-- 7 -->
  <update startup="test"/> <!-- 8 -->
</featureCollection>
~~~

1. A THREDDS `featureCollection` is defined, of type `GRIB1`. 
   All contained datasets will all have a path starting with `gribfc/LocalFNLCollection`.
2. All the metadata contained here will be inherited by the contained datasets.
3. The services to be used are defined in a compound `service` type called `all`.
4. You can add any metadata that is appropriate.
5. The collection of files is defined, using a collection specification string. 
   Everything under `<path-to-data>/data/gribfc_tutorial/basic/ds083.2/data` will be scanned for files with names that match the regular expression `fnl_.*_00_c$`
6. The collection will be split into a time partition by directory.
7. A date will be extracted from the filename by matching the characters after `fnl_` with `yyyyMMdd_HH`.
   An example filename is `fnl_20100104_12_00_c`, so the date will be year `2010`, month `01`, day `04` and hour `12`.
8. Read in the collection when the TDS starts up, and test that the indices are up to date.
 
The resulting top level web page for the dataset looks like:

<!-- insert image of html catalog here -->

The TDS has created a number of datasets out of the GRIB collection, and made them available through the catalog interface.

There is:

* \"Full Collection\" dataset : all the data is available with two dimensions of time: a reference time, and a valid time.
* \"Best Time series\" dataset: for each valid time, use the record with the smallest offfset from the refrence time.
* \"Latest Reference Time\" dataset: All of the data from the latest reference time, e.g. latest model run.
* For each directory partition of the data, folder which you can click into, and follow the directory hierarchy. 
  For example selecting the 2010 amd then the 2010.01 datasets:

<!-- insert image of html catalog here -->
{% include image.html file="tds/tutorial/grib/gribfc_basic_top.png" alt="Collection Top Level Catalog" caption="" %}

<!-- insert image of descended html catalog here -->
{% include image.html file="tds/tutorial/grib/gribfc_basic_1.png" alt="Collection Second Level Catalog" caption="" %}

For each separate reference time, there is a logical dataset, each with a \"Full\" (two time dimensions) and \"Best\" dataset.
Drilling down to the bottom of one of these:

You see that it has a \"Best Timeseries\" collection dataset as well as listing the individual files in the collection:

<!-- insert image of descended html catalog here -->
{% include image.html file="tds/tutorial/grib/gribfc_basic_raw.png" alt="Raw Access" caption="" %}

{% include image.html file="tds/tutorial/grib/gribfc_basic_collection.png" alt="Collection Access" caption="" %}

Here is listed all of the metadata for this dataset, as well as the possible access methods (OpenDAP, WMS, etc).
This is the \"HTML view\" of the catalog, with URL:

~~~bash
http://localhost:8080/thredds/catalog/gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/catalog.html?
	dataset=gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/GC
~~~

It\'s instructive to look at the \"XML view\" of the catalog, by removing the query (after the \"?\") and changing the \"html\" extension to \"xml\", giving this URL:

~~~bash
http://localhost:8080/thredds/catalog/gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/catalog.xml
~~~

and this is the result:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" xmlns:xlink="http://www.w3.org/1999/xlink" name="ds083.2-2010.01-20100101-000000" 
		version="1.0.1">
  <service name="VirtualServices" serviceType="Compound" base="">
    <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/" />
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
    <service name="wms" serviceType="WMS" base="/thredds/wms/" />
    <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/grid/" />
    <service name="cdmremote" serviceType="CdmRemote" base="/thredds/cdmremote/" />
    <service name="ncml" serviceType="NCML" base="/thredds/ncml/" />
    <service name="uddc" serviceType="UDDC" base="/thredds/uddc/" />
    <service name="iso" serviceType="ISO" base="/thredds/iso/" />
  </service>

  <dataset name="ds083.2-2010.01-20100101-000000" ID="gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/GC" 
		urlPath="gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/GC">
    <documentation type="summary">Single reference time Grib Collection</documentation>
    <metadata inherited="true">
      <serviceName>VirtualServices</serviceName>
      <dataType>GRID</dataType>
      <documentation type="summary">LOCAL FNL's TO TEST TIME PARTITION</documentation>
      <documentation type="Reference Time">2010-01-01T00:00:00Z</documentation>
      <geospatialCoverage>
        <northsouth>
          <start>-90.0</start>
          <size>180.0</size>
          <resolution>-1.0</resolution>
          <units>degrees_north</units>
        </northsouth>
        <eastwest>
          <start>0.0</start>
          <size>360.0</size>
          <resolution>1.0</resolution>
          <units>degrees_east</units>
        </eastwest>
        <name>global</name>
      </geospatialCoverage>
      <timeCoverage>
        <start>2010-01-01T00:00:00Z</start>
        <end>2010-01-01T00:00:00Z</end>
      </timeCoverage>
      <variableMap xlink:href="/thredds/metadata/gribfc/LocalFNLCollection/ds083.2-2010/ds083.2-2010.01/ds083.2-2010.01-20100101-000000.ncx2/GC?metadata=variableMap" 
			xlink:title="variables" />
    </metadata>
  </dataset>
</catalog>
~~~

You can click around in these pages to familiarize yourself with the various datasets.

## GRIB Feature Collection with multiple GDS
The second feature collection in catalogGribfc.xml has:
(create new exercise using data\gribfc_tutorial\multiple_gds\RDPS)

## GRIB Feature Collection with spurious GDS

The third feature collection in `catalogGribfc.xml` has:

~~~xml
<featureCollection name="NDFD-CONUS_5km_conduit" featureType="GRIB2" path="gribfc/ndfd"> <!-- 1 -->
  <metadata inherited="true">
    <dataFormat>GRIB-2</dataFormat>
  </metadata>
  <collection spec="/machine/tds/tutorial/ndfd/.*grib2$" <!-- 2 -->    
              dateFormatMark="#NDFD_CONUS_5km_conduit_#yyyyMMdd_HHmm" />
  <gribConfig> <!-- 3 -->
      <gdsHash from="-2121584860" to="28944332"/>
  </gribConfig>
</featureCollection>
~~~

1. A THREDDS `featureCollection` is defined, of type `GRIB2`.
   All contained datasets will all have a path starting with gribfc/ndfd.
2. Make sure you specify GRIB-2 dataFormat, or else nothing will work.
3. Subdirectories of `/machine/tds/tutorial/ndfd` will be scanned for files with names that end with `grib2`. 
   A date will be extracted from the filename by matching the characters after the \"NDFD_CONUS_5km_conduit_\" with `yyyyMMdd_HHmm`.
   An example filename is `NDFD_CONUS_5km_conduit_20120124_2000.grib2`, so the date will be year `2012`, month `01`, day `24`, hour `20`, minute `00`.
4. A configuration element that is specific to GRIB collections. 
   In this case we are combining records with GDS hashcode `-2121584860` into GDS `28944332`.

Open up the ToolsUI `IOSP/GRIB2/Grib2Collection` tab, and enter the `"/work/tds/tutorial/ndfd/.*grib2$"` into the collection spec, you will see something like:

{% include image.html file="tds/tutorial/grib/gribfc20.png" alt="GRIB GDS ToolsUI" caption="" %}

The bottom table shows that there are two distinct GDS in this collection.
The column marked \"hash\" shows the GDS hash code that you use in the TDS configuration table.
However, both GDS have the same `nx` and `ny`, which is a bit _suspicious_.
Select both GDS, then right click on them and select \"compare GDS\" to get this:

{% include image.html file="tds/tutorial/grib/gribfc21.png" alt="compare GDS" caption="" %}

This compares the `x` and `y` coordinates of the two GDS.
These are displaced by **.367** and **.300** km, respectively.
If you open this dataset up in the coordinate system tab, you will see that the `x,y` grid spacing is 2.5 km.
It\'s possible that some of these variables are displaced 3/10 km, and it\'s possible that there is a error in generating these GRIB records, and that in fact all of the variables should be on the same grid.
If the latter, then the `gdsConfig` element in the TDS config catalog above will fix the problem.

This effects the generation of the CDM index (ncx4) files.
To have this take affect, delete any ncx4 files and regenerate.