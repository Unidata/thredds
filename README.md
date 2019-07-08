![THREDDS icon](http://www.unidata.ucar.edu/images/logos/netcdfjava_tds_150x150.png)
<br>
<br>

# Unidata's THREDDS Project

The Thematic Real-time Environmental Distributed Data Services (THREDDS) project is developing middleware to bridge the gap between data providers and data users.
The goal is to simplify the discovery and use of scientific data and to allow scientific publications and educational materials to reference scientific data.
The mission of THREDDS is for students, educators, and researchers to publish, contribute, find, and interact with data relating to the Earth system in a convenient, effective, and integrated fashion.

The THREDDS project consists mainly of four software packages and two related XML encodings:

* the netCDF-Java/CDM library
* the NetCDF Markup Language (NcML)
* the THREDDS Data Server (TDS)
* the THREDDS Catalog specification
* Rosetta
* Siphon

A little information about each member of the THREDDS family can be found below.

## netCDF-Java and the THREDDS Data Server

Prior to version `5.0`, the netCDF-Java and TDS codebase was combined and managed at this repository.
The `v4.6.x` line of development is still combined, and continue to be found in this repository under the `master` [branch](https://github.com/Unidata/thredds/tree/master).

Going forward (`v5.0`), the netCDF-Java codebase, including the NcML and THREDDS Catalog specifications, can be found at:

https://github.com/Unidata/netcdf-java

The THREDDS Data server codbase can be found at:

https://github.com/Unidata/tds

While the versioning of the netCDF-Java library and the TDS have traditionally been in lockstep, it is anticipated that `v5.0` is the last time this will be the case.

We ask that any new issues be opened in the appropriate repository.
Our community email lists (netcdf-java@unidata.ucar.edu and thredds@unidata.ucar.edu) as well as our support email addresses will continue to be available as a resource.

## Rosetta

Rosetta is a web-based service that provides an easy, wizard-based interface to faciliate the transformation of ASCII based in-situ datafiles into Climate and Forecast (CF) compliant netCDF files.
More on Rosetta can be found [here](https://github.com/unidata/rosetta).

## Siphon

The newest addition to the THREDDS project is the python based data access library Siphon.
While Siphon was initially focused on access data hosted on a TDS, it has since expanded its capabilities to access remote data hosted behind other web services.
More information on Siphon can be found [here](https://github.com/unidata/siphon).