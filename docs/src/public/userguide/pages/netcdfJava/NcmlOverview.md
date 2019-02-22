---
title: NcML Overview
last_updated: 2018-04-02
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: ncml_overview.html
---

## Overview
NcML is an XML representation of netCDF metadata, (approximately) the header information one gets from a netCDF file with the "ncdump -h" command.
NcML is similar to the netCDF [CDL](https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_guide){:target="_blank"} (network Common data form Description Language), except, of course, it uses XML syntax.

NcML development is happening on two fronts:

1. [NcML-2.2](#ncml-22) as implemented by the [NetCDF-Java library](http://www.unidata.ucar.edu/software/netcdf-java/index.html){:target="_blank"}.
2. [ncML-Gml](https://www.researchgate.net/publication/221646728_NcML-GML_Encoding_NetCDF_Datasets_Using_GML){:target="_blank"} is an extension of ncML core schema, based on GML grammar.
   It uses both NcML and [GML](http://en.wikipedia.org/wiki/Geography_Markup_Language){:target="_blank"} to create a bridge to GIS Systems.
   This work is led by Stefano Nativi at the [Università degli Studi di Firenze](https://www.unifi.it/){:target="_blank"}.

## NcML 2.2

### NcML as output

The simplest use of NcML is to describe the metadata and structural content of a netCDF file, like the output of `ncdump -h`. There are several ways to generate NcML ouput from an existing netCDF file:

* In the ToolsUI application, use the NcML tab to open a NetCDF dataset, and the NcML will be displayed.
  You can edit and then save the NcML with the "Save NcML" button.
* In the NetCDF-Java library, use `NetcdfFile.writeNcML()` or `NcMLWriter().writeXML()`.
* Use the NetCDF-Java library `ncdump` application:
  ~~~bash
  java -classpath netcdfAll.jar ucar.nc2.NCdumpW <NetCDF-3 filename> -ncml
  ~~~
* In the netCDF C library, version 3.6.1 or later , use `ncdump -x<NetCDF filename>`

### Using NcML to create a NetCDF-3 file

Using the NetCDF-Java library, you can also use NcML to create a new netCDF-3 file, similar to the ncgen program.

* Using a text or XML editor, create the NcML file with an "xml" or "ncml" file extension. 
  Open the NcML file with `NetcdfDataset.open()`, then call `ucar.nc2.FileWriter.writeFile()`.
* In the ToolsUI application, open an existing NcML file in the NcML tab, or create a new one.
  You can edit and save the NcML with the **Save NcML** button.
  You can create the equivalent binary NetCDF file with the **Write NetCDF** button.

### Using NcML to modify an existing CDM file

Using the NetCDF-Java library, you can use NcML to modify an existing CDM file, and then write a new netCDF-3 format file with those modifications in it.

* Using a text or XML editor, create the NcML file with an "xml" or "ncml" file extension. 
  Reference the existing file using the location attribute, and add, change or delete metadata in the NcML as in this [example](ncj_basic_ncml_tutorial.html#exercise-3-read-in-metadata-from-existing-netcdf-file-and-modify).
* Write the new file as a netCDF-3 format file:
  * Use the NetCDF-Java library nccopy application to write the new file, e.g.:
    ~~~java
    java -Xmx1g -classpath netcdfAll-4.3.jar ucar.nc2.dataset.NetcdfDataset -in myFile.ncml -out myFile.nc
    ~~~
  * In the ToolsUI application, open the NcML file in the NcML tab.
    You can create the equivalent binary NetCDF file with the "Write NetCDF" button.

### Advanced NcML

A more advanced use is to modify existing NetCDF files, as well as to create "virtual" NetCDF datasets, for example through aggregation. In that case, you'll have to read more:

* [Tutorial](ncj_basic_ncml_tutorial.html)
* [Aggregation](ncj_ncml_aggregation.html)
* [Cookbook Examples](ncj_ncml_cookbook.html)
* [Annotated Schema for Netcdf-Java 4](ncj_annotated_ncml_schema.html)
* [ncml-2.2.xsd](www.unidata.ucar.edu/schemas/netcdf/ncml-2.2.xsd)

### Acknowledgments and History

Earlier work in defining XML representations for netCDF data included Bear Giles DTD and XML tools in March 2000, a proposed DTD by Stefano Nativi and Lorenzo Bigagli at the University of Florence in May 2000, and a version developed by John Caron in February 2001.

The original NcML working group consisted of John Caron (Unidata/UCAR), Luca Cinquini (SCD/NCAR), Ethan Davis (Unidata/UCAR), Bob Drach (PCMDI/LLNL), Stefano Nativi (University of Florence), and Russ Rew (Unidata/UCAR).

In the first implementation of NcML (version 2.1), there were three parts to NcML with separate schema documents:

* **NcML Core Schema** represented the existing netCDF-3 data model
* **NcML Coordinate System** extended NcML Core Schema and extended the netCDF data model to add explicit support for general and georeferencing coordinate systems
* **NcML Dataset** extended NcML Core Schema to use NcML to define a netCDF file, similar to the `ncgen` command line tool, as well as to redefine, aggregate, and subset existing netCDF files.

**NcML Coordinate System** is now superceded by **NcML-GML**. 
**NcML Core Schema** and **NcML Dataset** have been combined into a single **NcML Schema**, and some of the NcML Dataset syntax and functionality has been modified.