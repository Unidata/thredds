Notes for grib-1 tables

 8/24/2011
 - starting from robb kambic's work in earlier version. He processed all tables into a uniform format. No documentation
   about the original sources. He filled in missing values with "varN" which probably has to be undone. The use of wildcards
   is suspect because it makes assumptions about wider use of tables than may be warrented. Without documentation theres no way
   to check those assumptions.
 - stored in resources/tables/*
 - also see tables/tables.txt

 8/24/2011 refactored grib1 table processing: defaults to wmo table. missing entries are taken from default table if exist.

 8/24/2011 no significant differences between wmo_2_v1.tab, wmo_2_v2.tab, wmo_2_v3.tab, removed first 2

 8/24/2011 Spot check on WRF AMPS (Grib1) showed it was using ncar_0_200.tab. Correct table is taken from
    http://www.mmm.ucar.edu/rt/amps/wrf_grib and is now named wrf_amps.wrf. These differ in all entries >= 248.
  - center=60,subcenter=255,version=2: Correspond with kevin manning (kmanning@ucar.edu) to get a subcenter assigned to
    AMPS and to properly version their tables.

 8/25/2011 Check current wmo table (wmo_2_v3.tab) against whats at http://dss.ucar.edu/docs/formats/grib/gribdoc/ (10.2).
  - units differ for entries 10, 19, 53, 76, 120. switch to using dss doc (wmo-grib1.dss).
  - correspond with dob dattorre at dss to get proper managemnt of NCAR GRIB tables (subcenters, generating process ids).

 8/25/2011 Some differences between wmo-grib1.dss and ncep tables (nceptab_3.tab). compare to dss ncep tables when those are fixed.
  at this point, id say all grib2 tables are suspect.

 8/25/2011 spot check our grib1 tables against http://dss.ucar.edu/metadata/ParameterTables/
  WMO_GRIB1.98-0.128.xml: minor changes in desc; units 57, 58, 212; 15 missing in our table
  WMO_GRIB1.59-0.2.xml: many extra in ours; units are messed up in about 25 of ours; different 53, 198
  WMO_GRIB1.34-0.3.xml: about 10 extra in ours.
  WMO_GRIB1.7-1.132.xml: udunits differ: 3,103,178,179;
  WMO_GRIB1.xml against wmo_2_v3.tab: udunits 4 wrong, 3 right;

 8/26/2011 try using tables from http://dss.ucar.edu/metadata/ParameterTables/ instead of ours (resources/tables)
  - place in resources/grib1/dss
  - WMO_GRIB1.xml ("WMO GRIB1 Parameter Code Table 3") needed a few corrections, comparing it to
    ftp://www.wmo.int/Documents/MediaPublic/Publications/CodesManual_WMO_no_306/WMO306_Vol_I.2_2010_en.pdf
  - Im seeing some missing entries in the dss NCEP tables. NCEP probably using version 2. What is the difference between WMO version 3 and version 2 and 1 ?
  - "i think you are supposed to use 2 or 3 when you dont have any local entries ("international exchange"), and 128-254 is available when you do."
  - move original, unmodified tables to resources/grib1/tablesOld. probably move to sources eventually if not needed in release.

 8/26/2011 screen scrape http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html to get WMO version 2 hopefully
   - now in resources/grib1/ncep/table2.htm
   - compare to tablesOld/wmo_2_v2.tab: 11 udunit differences
   - compare to WMO_GRIB1.xml: param 98 (ice divergence 1/s vs m/s). typo or correction?

 8/28/2011 use afwa.tab for 57-1-2
   - remove all entries in standard section
