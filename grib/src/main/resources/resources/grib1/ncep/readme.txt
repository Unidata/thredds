11/21/2011 screen scrape http://www.nco.ncep.noaa.gov/pmb/docs/on388/
 - use program ucar.nc2.grib.grib1.tables.NcepHtmlScraper

01/13/2012 also need to deal with the following tables for NCEP:
  - TABLE A (Generating Process or Model from Originating Center 7) : http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablea.html
    already have resources/grib1/ncep/ncepTableA.xml read by NcepHtmlScraper
    (center 9 handcoded in  ucar.nc2.grib.grib1.tables.Grib1Tables)

  - Table B (GRID IDENTIFICATION) is encoded by hand in program ucar.nc2.grib.grib1.tables.Grib1GdsPredefined

  - Table C (National subcenters) http://www.nco.ncep.noaa.gov/pmb/docs/on388/tablec.html
    matches WMO common table 12 (subccenters)

  - Table 3 (level types) http://www.nco.ncep.noaa.gov/pmb/docs/on388/table3.html
    currently hand encoded in ucar.nc2.grib.grib1.Grib1ParamLevel

  - Table 5 http://www.nco.ncep.noaa.gov/pmb/docs/on388/table5.html
     currently hand encoded in ucar.nc2.grib.grib1.Grib1ParamTime

01/13/2012 also need to use correct tables for FNMOC. see fnmoc readme

01/14/2012 remove resources/grib1/ncep/*.tab (wgrib formatted tables)
 - regenerate with NcepHtmlScraper, however there are hand corrections to add units, etc

05/16/2013 caron
    screen scrape http://www.nco.ncep.noaa.gov/pmb/docs/on388/
  - use program ucar.nc2.grib.grib1.tables.NcepHtmlScraper
  - table 3, table(s) 2 and table A

07/31/2013  caron
  - NcepHtmlScraper had bug in table3, where the description was incorrect. regenerate.
    why didnt we notice this, for NCEP special level handling.

03/04/2014  lansing
  - Tables provided by users at NASA (Hiroko Kato, David M. Mocko, Hualan Rui, Fan Fang, Beaudoing) who assure us that they are the only tables and that
  they have never changed.
    See TDS-511 or e-support VNI-792787 for history.  Tables are located in /share/testdata/support/VNI-792787/DefinitiveTablesFromNASAPeople
    7:4:130
    7:12:130
    7:138:130

    Also provided were these links for additional README support:
    -ftp://hydro1.sci.gsfc.nasa.gov/data/s4pa/NLDAS/README.NLDAS1.pdf
    -ftp://hydro1.sci.gsfc.nasa.gov/data/s4pa/NLDAS/README.NLDAS2.pdf

    Adding to the confusion is that the above provided documentation refers to "File A" data and "File B" data, with some notes
    suggesting that these different data files use the "same" parameter tables with slightly different units/parameter descriptions/etc.

08/21/2014 caron
  - AFAICT, GLDAS and NLDAS files -> ncepGrib1-7-4-130, ncepGrib1-7-12-130, ncepGrib1-7-138-130
  - seems likely that nasa has hijacked center/subcenter/version (?)
  - add original files to sources/nasa

06/01/2015 sarms
  - moved original files to source/nasa
  - updated grib1 table /grib/src/main/resources/resources/grib1/ncep/ncepGrib1-130.xml by hand
    to match what is shown in source/nasa/README.NLDAS1.pdf (description not properly shown in the
    *.txt files sent to us).
  - See jira for more details: https://bugtracking.unidata.ucar.edu/browse/TDS-674
  - The update was focused on disambiguating some of the parameters by adding the platform name
    from which they were obtained (i.e. replacing "Downward Shortwave Radiation Flux" with
    "Downward Shortwave Radiation Flux from GOES-UMD Pinker".

11/17/2016 sarms
  - updated ncep grib1 tables using ucar/nc2/grib/grib1/tables/NcepHtmlScraper.java

12/01/2017 sarms
  - updated ncep grib1 tables using ucar/nc2/grib/grib1/tables/NcepHtmlScraper.java
