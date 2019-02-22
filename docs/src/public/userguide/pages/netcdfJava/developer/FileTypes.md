---
title: CDM File Types
last_updated: 2018-10-10
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: ncj_file_types.html
---

The NetCDF-Java/CDM library provides a uniform API to many different scientific file formats and remote access protocols.
Generally, application programmers should respect the encapsulation of these formats.
When information is needed about the underlying file type, `NetcdfFile.getFileTypeId()`, `NetcdfFile.getFileTypeVersion()`, and `NetcdfFile.getFileTypeDescription()` methods can be called.

The **Id** must be unique and once registered, will never change, so that application code can test against it.
**Version** information should have a standard form for each file type, but the form may differ between file types.
The **Description** is human-readable and may present variations when appropriate, such as adding processing history, etc.
The **Reference URL**(s) in this table are informational, and may change as needed.

To support this functionality, `getFileTypeId()`, `getFileTypeVersion()`, and `getFileTypeDescription()` methods have been added to the IOServiceProvider interface as of version 4.0.46.
You will need to add these methods to your IOServiceProvider implementations.

To register your format/IOServiceProvider, or to send corrections and additions to this table, please send email to support-netcdf-java@unidata.ucar.edu.

| Id | Description | Reference URL
| BUFR | WMO Binary Universal Form | http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html |
| CINRAD | Chinese Level-II Base Data | http://www.cinrad.com/ |
| DMSP | Defense Meteorological Satellite Program | http://dmsp.ngdc.noaa.gov/ |
| DORADE | DOppler RAdar Data Exchange Format |http://www.eol.ucar.edu/rsf/UserGuides/SABL/DoradeFormat/DoradeFormat.html http://www.eol.ucar.edu/instrumentation/airborne-instruments/eldora/ |
| F-TDS | Ferret I/O Service Provider and Server-side Analysis | http://ferret.pmel.noaa.gov/LAS/documentation/the-ferret-thredds-data-server-f-tds |
| FYSAT | Chinese FY-2 satellite image data in AWX format | http://satellite.cma.gov.cn/ |
| GempakGrid| GEMPAK Gridded Data | https://www.unidata.ucar.edu/software/gempak/ |
| GempakSurface | GEMPAK Surface Obs Data| https://www.unidata.ucar.edu/software/gempak/ |
| GINI | GOES Ingest and NOAAPORT Interface | http://weather.unisys.com/wxp/Appendices/Formats/GINI.html |
| GRIB-1| WMO GRIB Edition 1 | http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB1-Contents.html |
| GRIB-2 | WMO GRIB Edition 2 | http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB2_062006.pdf |
| GTOPO | USGS GTOPO digital elevation model | http://edc.usgs.gov/products/elevation/gtopo30/gtopo30.html |
| HDF4 | Hierarchical Data Format, version 4 | http://www.hdfgroup.org/products/hdf4/ |
| HDF5 | Hierarchical Data Format, version 5 | http://www.hdfgroup.org/HDF5/ |
| McIDASArea| McIDAS area file | http://www.ssec.wisc.edu/mcidas/doc/misc_doc/area2.html |
| McIDASGrid | McIDAS grid file | http://www.ssec.wisc.edu/mcidas/doc/prog_man/2006  http://www.ssec.wisc.edu/mcidas/doc/prog_man/2006/formats-20.html#22077 |
| netCDF | NetCDF classic format | https://www.unidata.ucar.edu/software/netcdf/index.html |
| netCDF-4 | NetCDF-4 format on HDF-5 | https://www.unidata.ucar.edu/software/netcdf/index.html |
| NEXRAD-2 | NEXRAD Level-II Base Data | http://www.ncdc.noaa.gov/oa/radar/radarresources.html http://www.tsc.com/SETS/_3TDWR.htm |
| NEXRAD-3 | NEXRAD Level-III Products | http://www.ncdc.noaa.gov/oa/radar/radarresources.html |
| NLDN | National Lightning Detection Network | http://www.vaisala.com/weather/products/aboutnldn.html |
| NMCon29 | NMC Office Note 29 | http://www.emc.ncep.noaa.gov/mmb/data_processing/on29.htm |
| OPeNDAP | Open-source Project for a Network Data Access Protocol | http://opendap.org/ |
| SIGMET | SIGMET-IRIS weather radar | http://www.vaisala.com/en/defense/products/weatherradar/Pages/IRIS.aspx |
| UAMIV | CAMx UAM-IV formatted files| http://www.camx.com/ |
| UniversalRadarFormat | Universal Radar Format | ftp://ftp.sigmet.com/outgoing/manuals/program/cuf.pdf |
| USPLN | US Precision Lightning Network | http://www.uspln.com/ |