---
title: TDS Online Tutorial
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar 
permalink: tds_tutorial_index.html
toc: false
---

The THREDDS Data Server (TDS) is a web server that provides metadata and data access for scientific datasets, using OPeNDAP, OGC WMS and WCS, HTTP, and other remote data access protocols.
The TDS is developed and supported by Unidata, a division of the University Corporation for Atmospheric Research (UCAR), and is sponsored by the National Science Foundation.

Some of the technology in the TDS:

* THREDDS [Dataset Inventory Catalogs](updateme) are used to provide virtual directories of available data and their associated metadata. 
  These catalogs can be generated dynamically or statically.
* The [Netcdf-Java/CDM library](updateme) reads NetCDF, OpenDAP, and HDF5 datasets, as well as other binary formats such as GRIB and NEXRAD into a Common Data Model (CDM), essentially an (extended) netCDF view of the data.
  Datasets that can be read through the Netcdf-Java library are called CDM datasets.
* TDS can use the [NetCDF Markup Language](updateme) (NcML) to modify and create virtual aggregations of CDM datasets.
* An integrated server provides [OPeNDAP](http://www.opendap.org/){:target="_blank"} access to any CDM dataset.
  OPeNDAP is a widely used, subsetting data access method extending the HTTP protocol.
* An integrated server provides bulk file access through the HTTP protocol.
* An integrated server provides data access through the [OpenGIS Consortium (OGC) Web Coverage Service (WCS)](http://www.opengeospatial.org/standards/wcs){:target="_blank"} protocol, for any "gridded" dataset whose coordinate system information is complete.
* An integrated server provides data access through the [OpenGIS Consortium (OGC) Web Map Service (WMS)](http://www.opengeospatial.org/standards/wms){:target="_blank"} protocol, for any "gridded" dataset whose coordinate system information is complete.
  This software was developed by Jon Blower (University of Reading (UK) E-Science Center) as part of the [ESSC Web Map Service for environmental data](http://behemoth.nerc-essc.ac.uk/ncWMS/godiva2.html){:target="_blank"} (aka Godiva2).
* The integrated [ncISO server](updateme){:target="_blank"} provides automated metadata analysis and ISO metadata generation.
* The integrated [NetCDF Subset Service](updateme){:target="_blank"} allows subsetting certain CDM datasets in coordinate space, using a REST API.
  Gridded data subsets can be returned in [CF-compliant](http://cfconventions.org/cf-conventions/v1.6.0/cf-conventions.html){:target="_blank"} netCDF-3 or netCDF-4. Point data subsets can be returned in CSV, XML, or [CF-DSG v1.6](http://cfconventions.org/cf-conventions/v1.6.0/cf-conventions.html#discrete-sampling-geometries){:target="_blank"} netCDF files.

The THREDDS Data Server is implemented in 100% Java\*, and is contained in a single war file, which allows very easy installation into a servlet container such as the open-source Tomcat web server.
Configuration is made as simple and as automatic as possible, and we have made the server as secure as possible.
The library is freely available and the source code is released under the (MIT-style) netCDF library license.

\*Writing to netCDF-4 files is supported through the netCDF C library only.

Much of the realtime data available over the Unidata Internet Data Distribution (IDD) is available through a demonstration THREDDS Data Server hosted at Unidata at [http://thredds.ucar.edu/](http://thredds.ucar.edu/thredds/catalog.html){:target="_blank"}.
You are welcome to browse and access these meteorological datasets.
If you need regular access to large amounts of data, please contact <support-idd@unidata.ucar.edu>.
