---
title: GRIB Feature Collections Tutorial
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: grib_feature_collections.html
---

## GRIB Feature Collection

The featureCollection element is a way to tell the TDS to serve collections of [CDM Feature Datasets](ncj_feature_datasets.html).
Currently this is used mostly for [gridded data](ncj_grid_data_type.html) whose time and spatial coordinates are recognized by the CDM software stack.
In this tutorial, we will work with featureCollection for collections of GRIB files.

## Creating a GRIB Feature Collection

Download {% include link_file.html file="tds_tutorial/grib/catalogGribfc1.xml" text="catalogGribfc1.xml" %}, place it in `${tds.content.root.path}/thredds` directory and add a `catalogRef` to it from your main catalog.
Here\'s what we have to work with:

~~~xml
<<?xml version="1.0" encoding="UTF-8"?>
<catalog name="Test GribCollections"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink">

  <service name="all" serviceType="Compound" base="">
    <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/"/>
    <service name="HTTPServer" serviceType="HTTPServer" base="/thredds/fileServer/"/>
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/"/>
    <service name="wms" serviceType="WMS" base="/thredds/wms/"/>
    <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/grid/"/>
    <service name="cdmremote" serviceType="CdmRemote" base="/thredds/cdmremote/"/>
    <service name="ncml" serviceType="NCML" base="/thredds/ncml/"/>
    <service name="uddc" serviceType="UDDC" base="/thredds/uddc/"/>
    <service name="iso" serviceType="ISO" base="/thredds/iso/"/>
  </service>

  <featureCollection name="FNL" featureType="GRIB1" path="gribfc/LocalFNLCollection"> <!-- 1 -->
    <metadata inherited="true"> <!-- 2 -->
      <serviceName>all</serviceName> <!-- 3 -->
      <documentation type="summary">LOCAL FNL's TO TEST TIME PARTITION</documentation> <!-- 4 -->
    </metadata>

    <collection name="LOCAL_FNL"
                spec="<path-to-workshop-data>/data/gribfc_tutorial/basic/ds083.2/data/**/fnl_.*_00.grib1.gbx9$" <!-- 5 -->
                timePartition="1 month" <!-- 6 -->
                dateFormatMark="#fnl_#yyyyMMdd_HH" /> <!-- 7 -->

    <tdm rewrite="test" /> <!-- 8 -->
  </featureCollection>


</catalog>
~~~

1. A THREDDS `featureCollection` is defined, of type `GRIB1`. 
   All contained datasets will all have a path starting with `gribfc/LocalFNLCollection`.
2. All the metadata contained here will be inherited by the contained datasets.
3. The services to be used are defined in a compound `service` type called `all`.
4. You can add any metadata that is appropriate.
5. The collection of files is defined, using a collection specification string. 
   Everything under `<path-to-data>/data/gribfc_tutorial/basic/ds083.2/data` will be scanned for files with names that match the regular expression `fnl_.*_00_c$`
6. The collection will be split into a time partition by `1 month` chunks.
7. A date will be extracted from the filename by matching the characters after `fnl_` with `yyyyMMdd_HH`.
   An example filename is `fnl_20100104_12_00_00.grib1.gbx9`, so the date will be year `2010`, month `01`, day `04` and hour `12`.
8. Read in the collection when the TDM starts up, and test that the indices are up to date.
   Note: the TDS does not do anything other than read index files that must already exist on disk.

Now, run the TDM.
Once finished, start the TDS.

The resulting top level web page for the dataset looks like:

{% include image.html file="tds/tutorial/grib/gribfc_basic_top.png" alt="Collection Top Level Catalog" caption="" %}

The TDS has created a number of datasets out of the GRIB collection, and made them available through the catalog interface.

There is:

* \"Full Collection\" dataset : all the data is available with two dimensions of time: a reference time, and a valid time.
* \"Latest Reference Time\" dataset: All of the data from the latest reference time, e.g. latest model run.
* For each `1 month` partition of the data, folder which you can click into, and follow the directory hierarchy. 
  For example selecting the `FNL-2010-01` dataset:

{% include image.html file="tds/tutorial/grib/gribfc_basic_1.png" alt="Collection Second Level Catalog" caption="" %}

we now see an entry for the `1 month` collection virtual dataset (`FNL-2010-01`), as well as a listing of raw files that make up the virtual `1 month` collection:

Each raw dataset corresponds to a single GRIB file on disk.
The only access method for a raw file is `HTTPServer`:

{% include image.html file="tds/tutorial/grib/gribfc_basic_raw.png" alt="Raw Access" caption="" %}

In comparison, the `1 month` collection does **not** have an `HTTPServer` access method, but rather it has the full suite of access methods appropriate for gridded data:

{% include image.html file="tds/tutorial/grib/gribfc_basic_collection.png" alt="Collection Access" caption="" %}

It\'s instructive to look at the \"XML view\" of the catalog, by removing the query (after the \"?\") and changing the \"html\" extension to \"xml\", giving this URL:

~~~bash
localhost:8080/thredds/catalog/gribfc/LocalFNLCollection/LOCAL_FNL-2010-01/catalog.xml
~~~

and this is the result:

~~~xml
<?xml version="1.0" encoding="ISO-8859-1"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" version="1.2" name="FNL-2010-01" xmlns:xlink="http://www.w3.org/1999/xlink">
  <service name="VirtualServices" base="" serviceType="Compound">
    <service name="ncdods" base="/thredds/dodsC/" serviceType="OPENDAP"/>
    <service name="wcs" base="/thredds/wcs/" serviceType="WCS"/>
    <service name="wms" base="/thredds/wms/" serviceType="WMS"/>
    <service name="ncss" base="/thredds/ncss/grid/" serviceType="NetcdfSubset"/>
    <service name="cdmremote" base="/thredds/cdmremote/" serviceType="CdmRemote"/>
    <service name="ncml" base="/thredds/ncml/" serviceType="NCML"/>
    <service name="uddc" base="/thredds/uddc/" serviceType="UDDC"/>
    <service name="iso" base="/thredds/iso/" serviceType="ISO"/>
  </service>
  <service name="HTTPServer" base="/thredds/fileServer/" serviceType="HTTPServer"/>
  <dataset name="FNL-2010-01" urlPath="gribfc/LocalFNLCollection/LOCAL_FNL-2010-01" ID="gribfc/LocalFNLCollection/LOCAL_FNL-2010-01">
    <documentation type="summary">Multiple reference, unique time Grib Collection</documentation>
    <metadata inherited="true">
      <serviceName>VirtualServices</serviceName>
      <dataType>GRID</dataType>
      <dataFormat>GRIB-1</dataFormat>
      <documentation type="summary">LOCAL FNL's TO TEST TIME PARTITION</documentation>
      <property name="Originating_or_generating_Center" value="US National Weather Service, National Centres for Environmental Prediction (NCEP)"/>
      <property name="Originating_or_generating_Subcenter" value="0"/>
      <property name="GRIB_table_version" value="0,2"/>
      <property name="Generating_process_or_model" value="Analysis from GDAS (Global Data Assimilation System)"/>
      <property name="file_format" value="GRIB-1"/>
      <property name="Conventions" value="CF-1.6"/>
      <property name="history" value="Read using CDM IOSP GribCollection v3"/>
      <property name="featureType" value="GRID"/>
      <geospatialCoverage>
        <northsouth>
          <start>-90.0</start>
          <size>180.0</size>
          <resolution>1.0</resolution>
          <units>degrees_north</units>
        </northsouth>
        <eastwest>
          <start>0.0</start>
          <size>359.0</size>
          <resolution>1.0</resolution>
          <units>degrees_east</units>
        </eastwest>
        <name>global</name>
      </geospatialCoverage>
      <timeCoverage>
        <start>2010-01-01T00:00:00Z</start>
        <end>2010-01-31T18:00:00Z</end>
      </timeCoverage>
      <variableMap xlink:title="variables" xlink:href="/thredds/metadata/gribfc/LocalFNLCollection/LOCAL_FNL-2010-01?metadata=variableMap"/>
    </metadata>
    <dataset name="Raw Files">
...
~~~

You can click around in these pages to familiarize yourself with the various datasets.

{%include question.html content="
Did the \"all\" service we defined actually get used?
" %}

## GRIB Feature Collection with multiple GDS

The second GRIB feature collection we will explore can be found in {% include link_file.html file="tds_tutorial/grib/catalogGribfc1.xml" text="catalogGribfc2.xml" %}:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog name="Test GribCollections"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink">

  <dataset name="CMC Forecast Models">
    <metadata inherited="true">
      <authority>edu.ucar.unidata</authority>
      <documentation xlink:href="https://www.canada.ca/en/environment-climate-change/services/science-technology/centres/quebec.html#cmc" xlink:title="CMC Home page"/>
      <documentation type="rights">Freely available</documentation>
      <documentation type="processing_level">Transmitted through Unidata Internet Data Distribution.</documentation>
      <documentation type="processing_level">Read by CDM Grib Collection.</documentation>
      <creator>
        <name vocabulary="DIF">Environment Canada (EC) / Canadian Meteorological Centre (CMC)</name>
        <contact url="http://gcmd.nasa.gov/Aboutus/software_docs/help_guide.html" email="http://gcmd.nasa.gov/MailComments/MailComments.jsf?rcpt=gcmduso"/>
      </creator>
      <publisher>
        <name vocabulary="ADN">University Corporation for Atmospheric Research (UCAR)/Unidata</name>
        <contact url="http://www.unidata.ucar.edu/" email="support@unidata.ucar.edu"/>
      </publisher>
    </metadata>
    <dataset name="Regional Deterministic Prediction System (RDPS)">
      <metadata inherited="true">
        <documentation xlink:href="https://weather.gc.ca/model_forecast/index_e.html"
                       xlink:title="RDPS home page"/>
      </metadata>

      <featureCollection name="RDPS North America 15km"
                         featureType="GRIB2"
                         harvest="true"
                         path="grib/CMC/RDPS/NA_15km">

        <metadata inherited="true">
          <documentation type="summary">Environment Canada (EC) / Canadian
            Meteorological Centre (CMC) Regional Deterministic Prediction System
            (RDPS) North America 15 km domain.
          </documentation>
        </metadata>

        <collection spec="<path-to-workshop-data>/data/gribfc_tutorial/multiple_gds/RDPS/CMC_RDPS_ps15km_#yyyyMMdd_HHmm#.grib2.gbx9"
                    name="CMC_RDPS_NA_15km"
                    timePartition="file"
                    olderThan="5 min"/>

        <tdm rewrite="test"/>
      </featureCollection>
    </dataset>
  </dataset>
</catalog>
~~~~

First, stop the TDS.
Next, update the spec path with the correct path on your disk, add a `catalogRef` to your main `catalog.xml`, and run the TDM.
Once the TDM is finished running, start the TDS.
Browse to the catalog and...woah:

{% include image.html file="tds/tutorial/grib/multi_gds_tds.png" alt="multi gds woah..." caption="" %}

What we see is that the \"Grib files\" are actually composed of message that are defined on very different grids.
By examining these files in ToolsUI, we can see there are indeed two GDS.
It appears that one GDS is associated with more \"primary\" modeled output; the second GDS appears to be associated with \"derived\" parameters.

Insert the following into catalogGribfc2.xml somewhere inside the `featureCollection` element:

~~~xml
<gribConfig>
  <gdsName hash='-1788043676' groupName='Model Fields'/>
  <gdsName hash='925211856' groupName='Derived Fields'/>
</gribConfig>
~~~

This allows us to name the different GDS group with something a little more readable.
Remove all of the generated .ncx4 files associated with these GRIB files, re-run the TDM, and restart the TDS.
Now our catalog looks like this:

{% include image.html file="tds/tutorial/grib/multi_gds_tds_rename.png" alt="multi gds woah..." caption="" %}

## GRIB Feature Collection with spurious GDS

Sometimes the GDS for new products gets incorrectly encoded.
This is generally fixed in a \"reasonable\" amount of time (for a sufficiently broad definition of reasonable).
While this might cause a hiccup for real-time data streams, it can absolutely wreak havoc for data archives.
This is a tale of what can happen, and how to fix it.

The names of offenders have been masked to protect the not-so-innocent

John \"data archive\" Doe (we\'ll just call him Joe) notices an issue on his TDS with output GRIB messages from the...uhh...NDFDDDD model that comes across the, ummm, Schmonduit data feed from the LDM.
He decides to investigate and opens the collection of NDFDDDD GRIB files with ToolsUI and notices...

{% include image.html file="tds/tutorial/grib/gribfc20.png" alt="GRIB GDS ToolsUI" caption="" %}

The bottom table shows that there are two distinct GDS in this collection.
Some variables use one, some use the other.
Joe is not happy.
Joe is sad.
The column marked \"hash\" shows the GDS hash codes, an yep, there are two.
Both GDS have the same `nx` and `ny`, which is a bit _suspicious_.
Joe goes into conspiracy mode.
Using ToolsUI, he select both GDSs, then right click on them and select \"compare GDS\" only to see this:

{% include image.html file="tds/tutorial/grib/gribfc21.png" alt="compare GDS" caption="" %}

These two grids are displaced by **.367** and **.300** km, respectively.
Joe thinks to himself...\"[wut](https://media.makeameme.org/created/wut-5a6943.jpg){:target="_blank}?\"
He opens this dataset up in the coordinate system tab and sees that the `x,y` grid spacing is 2.5 km.
Joe begins to ponder...
\"It\'s possible that some of these variables are displaced 3/10 km, but that might be pretty dumb on a 2.5 km grid.
Maybe it\'s possible that there is a error in the generation of these GRIB records, and that in fact all of the variables should be on the same grid.
Yeah...totes a mistake on NSCHMEPS part, probably.
I\'ll report this, and they\'ll fix it because they are awesome, but now what do I do in the meantime?\"

If Joe\'s intuition is correct, then a `gdsConfig` element in the TDS config catalog above will save the day.
Unlike in our last example, Joe uses `<gdsHash from="-2121584860" to="28944332"/>`:

~~~xml
<featureCollection name="NDFDDDD-CONUS_5km_SChmonduit" featureType="GRIB2" path="gribfc/ndfddd"> <!-- 1 -->
  <metadata inherited="true">
    <dataFormat>GRIB-2</dataFormat>
  </metadata>
  <collection spec="/machine/tds/tutorial/ndfdddd/.*grib2$" <!-- 2 -->    
              dateFormatMark="#NDFDDDD_CONUS_5km_Schmonduit_#yyyyMMdd_HHmm" />
  <gribConfig> <!-- 3 -->
      <gdsHash from="-2121584860" to="28944332"/>
  </gribConfig>
</featureCollection>
~~~

1. A THREDDS `featureCollection` is defined, of type `GRIB2`.
   All contained datasets will all have a path starting with gribfc/ndfd.
2. Make sure you specify GRIB-2 dataFormat, or else nothing will work.
3. Subdirectories of `/machine/tds/tutorial/ndfddd` will be scanned for files with names that end with `grib2`. 
   A date will be extracted from the filename by matching the characters after the \"NDFDDDD_CONUS_5km_Schmonduit_\" with `yyyyMMdd_HHmm`.
   An example filename is `NDFDDD_CONUS_5km_Schmonduit_20120124_2000.grib2`, so the date will be year `2012`, month `01`, day `24`, hour `20`, minute `00`.
4. A configuration element that is specific to GRIB collections. 
   In this case we are combining records with GDS hashcode `-2121584860` into GDS `28944332`.

Now Joe is sad because this affects the generation of the CDM index (ncx4) files.
To have this \"merging\" of GDSs take effect, Joe needs to delete all ncx4 files associated with the collection and regenerate them (TDM to the rescue!).
Joe turns the crank, stops the TDS, removes the ncx4 files, regenerates the index files using the TDM, starts the TDS, and lives happily ever after...until the next model upgrade...[TO BE CONTINUED](https://i.chzbgr.com/full/9015886336/hDB9E1BC1/){:target="_blank"}