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


