![THREDDS icon](http://www.unidata.ucar.edu/img/v3/logos/thredds-75x75.png)

# Unidata's THREDDS Project

The THREDDS project is developing middleware to bridge the gap between data
providers and data users. The goal is to simplify the discovery and use of
scientific data and to allow scientific publications and educational materials
to reference scientific data. The mission of THREDDS is for students, educators
and researchers to publish, contribute, find, and interact with data relating
to the Earth system in a convenient, effective, and integrated fashion.

The THREDDS project consists mainly of two software packages and two related
XML encodings:

* the netCDF-Java/CDM library;
* the NetCDF Markup Language (NcML);
* the THREDDS Data Server (TDS); and
* the THREDDS Catalog specification.

Source code for each of these is available from GitHub at

* https://github.com/Unidata/thredds

The latest released and snapshot software artifacts (.jar and .war files e.g.)
are available from Unidata's Maven repositories

* https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/
* https://artifacts.unidata.ucar.edu/content/repositories/unidata-snapshots/

Copyright and licensing information can be found here

* http://www.unidata.ucar.edu/software/netcdf/copyright.html  (???????)

as well as in the COPYRIGHT file accompanying the software. (??????)

More details on each of these can be found below.

## netCDF-Java/CDM

The netCDF Java library implements the Common Data Model (CDM) which provides
an interface to netCDF files and other types of scientific data formats.

For more information about netCDF-Java/CDM, see the netCDF-Java web page at

* http://www.unidata.ucar.edu/software/netcdf-java/

and the CDM web page at

http://www.unidata.ucar.edu/software/netcdf-java/CDM/index.html

You can obtain a copy of the latest released version of netCDF-Java software
from

* http://www.unidata.ucar.edu/downloads/netcdf/

More documentation can be found at

* http://www.unidata.ucar.edu/software/netcdf-java/documentation.htm

A mailing list, netcdf-java@unidata.ucar.edu, exists for discussion of all
things netCDF-Java/CDM including announcements about netCDF-Java/CDM bugs,
fixes, enhancements, and releases. For information about how to subscribe, see
the "Subscribe" link on this page

* http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/

For more general netCDF discussion, see the netcdfgroup@unidata.ucar.edu email list.

We appreciate feedback from users of this package. Please send comments,
suggestions, and bug reports to <support-netcdf-java@unidata.ucar.edu>.
Please identify the version of the package.

## NetCDF Markup Language (NcML)

NcML is an XML representation of netCDF metadata, it approximates the header
information one gets from a netCDF file with the "ncdump -h" command. NcML is
similar to the netCDF CDL (network Common data form Description Language),
except, of course, it uses XML syntax.

Beyond simply describing a netCDF file, it can also be used to describe changes
to existing netCDF files. A limited number of tools, mainly netCDF-Java based
tools, support these features of NcML

For more information about NcML, see the NcML web page at

http://www.unidata.ucar.edu/software/netcdf/ncml/

## THREDDS Data Server (TDS)

The THREDDS Data Server (TDS) is a web server that provides metadata and data
access for scientific datasets, using a variety of remote data access protocols.

For more information about the TDS, see the TDS web page at

* http://www.unidata.ucar.edu/software/tds/

You can obtain a copy of the latest released version of netCDF-Java software
from

* http://www.unidata.ucar.edu/downloads/thredds/

A mailing list, thredds@unidata.ucar.edu, exists for discussion of the TDS and
THREDDS catalogs including announcements about TDS bugs, fixes, enhancements,
and releases. For information about how to subscribe, see the
"Subscribe" link on this page

* http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/

We appreciate feedback from users of this package. Please send comments,
suggestions, and bug reports to <support-thredds@unidata.ucar.edu>.
Please identify the version of the package.

## THREDDS Catalogs

THREDDS Catalogs can be thought of as logical directories of on-line data
resources, encoded as XML documents, which provide a place for annotations and
other metadata about the data resources to reside. This is how THREDDS-enabled
data consumers find out what data is available from data providers.

THREDDS Catalog documentation (including the specification) is available at

* http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/