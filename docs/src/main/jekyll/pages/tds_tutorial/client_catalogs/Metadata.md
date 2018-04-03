---
title: Client Catalog Metadata
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_metadata.html
---

## Describing datasets

{%include note.html content="
Reference documentation - A complete listing of available properties can be found in the catalog specification.
" %}

So far, we've used the `name`, `serviceName`, and `urlPath` attributes to tell THREDDS how to treat our datasets.
However, there are a lot of optional properties, or _metadata_, that can be added to help _other_ applications and digital libraries know how to &ldquo;do the right thing&rdquo; with our data.
Here is a sample of them:

* The `collectionType` attribute is used on collection datasets to describe the relationship of their nested datasets.
* The `dataType` is a simple classification that helps clients to know how to display the data (e.g. `Image`, `Grid`, `Point` data, etc).
* The `dataFormatType` describes what format the data is stored in (e.g. `NetCDF`, `GRIB-2`, `NcML`, etc).
  This information is used by data access protocols like OPeNDAP and HTTP.
* The combination of the naming `authority` and the `ID` attributes should form a globally-unique identifier for a dataset.
  In the TDS, it is especially important to add the `ID` attribute to your datasets.

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III" collectionType="TimeSeries">
  <dataset name="January Averages" serviceName="odap" urlPath="sage/avg/jan.nc"
      ID="jan.nc" authority="unidata.ucar.edu">
    <dataType>Trajectory</dataType>
    <dataFormatType>NetCDF</dataFormatType>
  </dataset>
</dataset>
~~~

{%include note.html content="
Reference documentation - A complete listing of necessary attributes can be found here.
" %}

## Exporting THREDDS datasets to digital libraries

The `harvest` attribute indicates that the dataset is at the right level of granularity to be exported to digital libraries or other discovery services.
Elements such as `summary`, `rights`, and `publisher` are needed in order to create valid entries for these services.

~~~xml
<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III" harvest="true">
  <contributor role="data manager">John Smith</contributor>
  <keyword>Atmospheric Chemistry</keyword>
  <publisher>
    <long_name vocabulary="DIF">Community Data Portal, National Center for Atmospheric Research, University Corporation for Atmospheric Research</long_name>
    <contact url="http://dataportal.ucar.edu" email="cdp@ucar.edu"/>
  </publisher>
</dataset>
~~~

## Sharing metadata

When a catalog includes multiple datasets, it can often be the case that they have share properties.
For example:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
  <dataset name="January Averages" urlPath="sage/avg/jan.nc" ID="jan.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
  <dataset name="February Averages" urlPath="sage/avg/feb.nc" ID="feb.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
  <dataset name="March Averages" urlPath="sage/avg/mar.nc" ID="mar.nc" serviceName="odap" authority="unidata.ucar.edu" dataFormatType="NetCDF"/>
</dataset>
~~~

Rather than declare the same information on each dataset, you can use the `metadata` element to factor out common information:

~~~xml
<service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/"/>

<dataset name="SAGE III Ozone Loss Experiment" ID="Sage III">
  <metadata inherited="true"> <!-- 1 -->
    <serviceName>odap</serviceName> <!-- 2 -->
    <authority>unidata.ucar.edu</authority> <!-- 2 --> 
    <dataFormatType>NetCDF</dataFormatType> <!-- 2 -->
  </metadata>

  <dataset name="January Averages" urlPath="sage/avg/jan.nc" ID="jan.nc"/> <!-- 3 -->
  <dataset name="February Averages" urlPath="sage/avg/feb.nc" ID="feb.nc"/> <!-- 3 -->   
  <dataset name="Global Averages" urlPath="sage/global.nc" ID="global.nc" authority="fluffycats.com"/> <!-- 4 -->
</dataset>
~~~

* <1> The `metadata` element with `inherited="true"` implies that all the information inside the `metadata` element applies to the current dataset and all nested datasets.
* <2> The `serviceName`, `authority`, and `dataFormatType` are declared as elements.
* <3> These datasets use all the metadata values declared in the parent dataset.
* <4> This dataset overrides `authority`, but uses the other 2 metadata values

## When should I use a metadata element?

Both the `dataset` and `metadata` elements are containers for metadata called the `threddsMetadata` group.
When the metadata is specific to the dataset, put it directly in the `dataset` element.
When you want to share it with all nested datasets, put it in a `metadata` `inherited="true"` element.
