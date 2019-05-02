Notes for grib-1 tables (global)

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
  - "As far as I can tell from my repository history, aside from one correction (in late 2009) where the rain water and ice water were
     getting all confused, it looks like I've used the same table since at least early 2009."

 8/25/2011 Check current wmo table (wmo_2_v3.tab) against whats at http://dss.ucar.edu/docs/formats/grib/gribdoc/ (10.2).
  - units differ for entries 10, 19, 53, 76, 120. switch to using dss doc (wmo-grib1.dss).
  - correspond with dob dattorre at dss to get proper managemnt of NCAR GRIB tables (subcenters, generating process ids).

 8/25/2011 Some differences between wmo-grib1.dss and ncep tables (nceptab_3.tab). compare to dss ncep tables when those are fixed.
  at this point, id say all grib2 tables are suspect.

 8/25/2011 spot check our grib1 tables against http://dss.ucar.edu/metadata/ParameterTables/
    WMO_GRIB1.59-0.2.xml: many extra in ours; units are messed up in about 25 of ours; different 53, 198
  WMO_GRIB1.34-0.3.xml: about 10 extra in ours.
  WMO_GRIB1.7-1.132.xml: udunits differ: 3,103,178,179;
  WMO_GRIB1.xml against wmo_2_v3.tab: udunits 4 wrong, 3 right;

 8/26/2011 try using tables from http://dss.ucar.edu/metadata/ParameterTables/ instead of ours (resources/tables)
  - place in resources/grib1/dss
  - WMO_GRIB1.xml ("WMO GRIB1 Parameter Code Table 3") needed a few corrections, comparing it to
    ftp://www.wmo.int/Documents/MediaPublic/Publications/CodesManual_WMO_no_306/WMO306_Vol_I.2_2010_en.pdf
  - Im seeing some missing entries in the dss NCEP tables. NCEP probably using version 2. What is the difference between WMO version 3 and version 2 or 1 ?
  - "i think you are supposed to use 2 or 3 when you dont have any local entries ("international exchange"), and 128-254 is available when you do."
  - move original, unmodified tables to resources/grib1/tablesOld. probably move to sources eventually if not needed in release.

 8/26/2011 screen scrape http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html to get WMO version 2 hopefully
   - now in resources/grib1/ncep/table2.htm
   - compare to tablesOld/wmo_2_v2.tab: 11 udunit differences
   - compare to WMO_GRIB1.xml: param 98 (ice divergence 1/s vs m/s). typo or correction?  Check with NCEP.

 8/28/2011 use afwa.tab for 57-1-2
   - remove all entries in standard section

 8/29/2011
   - waiting for ncep
   - Bill Anderson contacted me from fnmoc, with follow up to Mary Clifford (in operations?).  They are apparently still not versioning tables.
   - put out call for grib1 example files.

 8/30/2011
   - Dave blodgett sends sample file from center 9-157 (North Central River Forecasting Center), params 61 (version 2) and 237 (version 128).
     Suspicious use of nceptab_3.tab for param 237. also, reference date changes which we dont handle correctly so time coord is wrong.
     claim is that the inclusion of parameter 237 is inadvertant, and should be ignored.

 9/1/2011 waiting for ncep response. previous email with Jeff Ator:
   Q: "We are reviewing our use of NCEP local tables for grib and bufr, looking for errors.
   I thought it would be worth checking to see if there are any official "machine readable" version of the tables? Also, when im examining the tables at
   http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml, should i assume that these are only for center 7, or also for centers 8 and 9?
   should I assume that these are for all subcenters of center 7? there seem to be about 15 subcenters.
   another way to ask this question: what do we know for sure about which centers/subcenters these tables apply to?"
   A: "All of our local tables are for originating center 7.  I'm not aware of any products we generate which use 8 or 9."
   which i guess means: dont use ncep tables for anything other than center 7. which we do. a lot.

 9/1/2011 use old tables as backup
   - need a judgement on what is right. remove wildcards -1 -1 (match all for center), as thats too optimistic.
   - should we ignore stuff thats in WMO part of table. I know ecmwf does override.

 9/1/2011 ecmwf
   - download "grib_api" software from http://www.ecmwf.int/products/data/software/download/grib_api.html
   - extract grib_api-1.9.9/definitions/grib1/*.table to resources/grib1/ecmwf/
   - these appear to include wmo standard tables and ecmwf local tables
   - no delimiter between name and units - cant parse unsupervised

 9/2/2011 ecmwf
   - download "gribex" software from http://www.ecmwf.int/products/data/software/grib.html
   - extract gribex_000370/gribtables/* to resources/grib1/ecmwf/
   - these appear to include wmo standard tables and local tables for center 98 (ecmwf), 74 (ukmet RFSC), 80 (rome NMC), 85 (toulouse RMSC)
   - wmo versions 1,2,3 all agree with each other
   - udunit differences with dss wmo version 3:
       10 udunits
         Dobson
         Dobson.(kg.m-2)
       19 udunits
         K.m-1
         K.s-1
       37 udunits
         m2.s-2
         m2.s-1
       53 udunits
         kg.kg-1
         kg.m-2
       76 udunits
         kg.m-2
         kg.kg-1
       94 udunits
         m.s-1
         m.s.-1
       120 udunits
         W.m-3.sr-1
         W.m-1.sr-1
  - comparision of random table (98-0-140) with dss: missing 5 params in DSS
   **No key 200 (GridParameter{number=200, name='MAXSWH', description='Maximum of significant wave height', unit='m'}) in first table
   **No key 217 (GridParameter{number=217, name='TMAX', description='Period corresponding to maximum individual wave height', unit='s'}) in first table
   **No key 218 (GridParameter{number=218, name='HMAX', description='Maximum individual wave height', unit='m'}) in first table
   **No key 253 (GridParameter{number=253, name='BFI', description='Benjamin-Feir index', unit='-'}) in first table
   **No key 254 (GridParameter{number=254, name='WSP', description='Wave spectral peakedness', unit='s**-1'}) in first table

9/2/2011
 - download tables from ftp.cpc.ncep.noaa.gov/wd51we/wgrib/
 - copy into resources/grib1/ncep, and append .tab to filenames
 - turns out this is the format that robb used
 - gribtab file has -1:7:-1:2, indicating its should be used for all center 7 version 2 tables
 - compare to dss standard: many differences
 - reanal_grib also has -1:7:-1:2 (!). robb assigns to version 132, must double check
 - with cleanup, ncep/reanal_grib.tab matches tablesOld/ncep_reanal_132.tab (i did not do  units cleanup)

9/2/2011 use of tablesOld
 - too many problems, remove use and add individuals back into local
 - examine files in cdmUnitTest/formats/grib1 to see what not covered
 - for 57:-1:2: compare tableOld/afwa.tab against WMO standard. all ok except:
     4 udunits
       K.m2.kg-1.s-1
       km2/kg/s
     58 udunits
       kg.m-2
       kg/kg
     76 udunits
       kg.m-2
       kg/kg
   did they really modify the units but keep the same desc? assume not. delete all entries < 128

 - for 58:42:2: compare tableOld/af_2.tab against WMO standard. all ok except:
      118 desc
        Brightness temperature
        max wave height
      118 udunits
        K
        m
    assume they really overrided this entry. delete all ebtries < 128 except 118.

9/7/2011
 - NCL describes their processing of GRIB: http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB
 of note:
   Dimensions:
      ensemble or probability (4.3.0 or later.)
      initial_time // (run time)
      forecast_time
      level
 - NCL describes their GRIB1 table handling : http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB1-built-in-parameter-tables

11/14/2011
 - from  http://www.weatheroffice.gc.ca/grib/what_is_GRIB_e.html:
   "The parameters used in the type of levels used in GRIB from the CMC are compatible with tables published in the NCEP Note 388."
   so we will use NCEP tables for center 54, for both grib1 and grib2.

01/16/2012  create wmoTable3.xml
 - start with WMO306_Vol_I.2_2010_en.pdf from http://www.wmo.int/pages/prog/www/WMOCodes/VolumeI2.html#VolumeI2
 - ftp://ftp.wmo.int/Documents/MediaPublic/Publications/CodesManual_WMO_no_306/WMO306_Vol_I.2_2010_en.pdf
 - put in standard XML form, from old embedded strings.
 - all info is now in this table, which must be maintained by hand.

01/28/2014 added grib1 tables for ECMWF
 - Tables are generated from the localConcept files from GRIB-API (software from ECMWF)
 - Use the main method in /ucar/nc2/grib/grib1/tables/EcmwfLocalConcepts to generate table files
 - see /src/main/sources/ecmwfGribApi/README.txt for more details
 - Removed all ECMWF tables (2.98.*) from the ecmwf and ncl directories, as these are now coming from GRIB-API

 5/28/2014 moving AFWA tables from "local" to own "afwa" directory

 12/01/2017 sarms
 - Start using grib1 tables for ecmwf from the package ecCodes
 - src/main/sources/ecmwfGribApi/README.txt is now src/main/sources/ecmwfEcCodes/README.txt
