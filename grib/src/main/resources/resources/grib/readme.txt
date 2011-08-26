Notes for grib tables

GRIB2
 - stored in wmo/* and local/*

GRIB1
 - stored in tables/*
 - starting from robb kambic's work in earlier version. He processed all tables into a uniform format. No documentation
   about the original sources. He filled in missing values with "varN" which probably has to be undone.
 - also see tables/tables.txt

GRIB1 CHANGES
 8/24/2011 refactored grib1 table processing: defaults to wmo table. missing entries are taken from default table if exist.

 8/24/2011 no significant differences between wmo_2_v1.tab, wmo_2_v2.tab, wmo_2_v3.tab, removed first 2

 8/24/2011 Spot check on WRF AMPS (Grib1) showed it was using ncar_0_200.tab. Correct table is taken from
    http://www.mmm.ucar.edu/rt/amps/wrf_grib and is now named wrf_amps.wrf. These differ in all entries >= 248.
    center=60,subcenter=255,version=2. Correspond with kevin manning (kmanning@ucar.edu) to get a subcenter assigned to
    AMPS and to properly version their tables.

 8/25/2011 Check current wmo table (wmo_2_v3.tab) against whats at http://dss.ucar.edu/docs/formats/grib/gribdoc/ (10.2).
    units differ for entries 10, 19, 53, 76, 120. switch to using dss doc (wmo-grib1.dss).

 8/25/2011 Some differences between wmo-grib1.dss and ncep tables (nceptab_3.tab). compare to dss ncep tables when those are fixed.
    at this point, id say all grib2 tables are suspect.

 8/25/2011 spot check our grib1 tables against http://dss.ucar.edu/metadata/ParameterTables/
  WMO_GRIB1.98-0.128.xml: minor changes in desc; units 57, 58, 212; 15 missing in our table
  WMO_GRIB1.59-0.2.xml: many extra in ours; units are messed up in about 25 of ours; different 53, 198
  WMO_GRIB1.34-0.3.xml: about 10 extra in ours.
  WMO_GRIB1.7-1.132.xml: udunits differ: 3,103,178,179;