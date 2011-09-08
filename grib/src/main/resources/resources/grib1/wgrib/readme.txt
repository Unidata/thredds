notes for tables in ncep

8/26/2011 screen scrape http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html to get WMO version 2 hopefully
   - now in resources/grib1/ncep/table2.htm
   - compare to tablesOld/wmo_2_v2.tab: 11 udunit differences
   - compare to WMO_GRIB1.xml: param 98 (ice divergence 1/s vs m/s). typo or correction?  Check with NCEP.

9/2/2011
 - download tables from ftp.cpc.ncep.noaa.gov/wd51we/wgrib/
 - copy into resources/grib1/ncep, and append .tab to filenames
 - turns out this is the format that robb used
 - gribtab file has -1:7:-1:2, indicating its should be used for all center 7 version 2 tables
 - compare to dss standard: many differences
 - reanal_grib also has -1:7:-1:2 (!). robb assigns to version 132, must double check
 - with cleanup, ncep/reanal_grib.tab matches tablesOld/ncep_reanal_132.tab (i did not do units cleanup)

 9/2/2011
 - compare
  GribPDSParamTable{center_id=7, subcenter_id=-1, version=140, name='nceptab_140.tab', path='resources/grib1/ncep/nceptab_140.tab'}
  GribPDSParamTable{center_id=7, subcenter_id=-1, version=140, name='nceptab_140.tab', path='resources/grib1/tablesOld/nceptab_140.tab'}
    168 desc
       Mean icing potential
       Near IR beam downward solar flux
     169 desc
       Maximum icing potential
       Near IR diffuse downward solar flux
     170 desc
       Mean in-cloud turbulence potential
       Rain water mixing ratio
     171 desc
       Maximum in-cloud turbulence potential
       Snow mixing ratio
     172 desc
       Mean cloud air turbulence potential
       Momentum flux
     173 desc
       Maximum cloud air turbulence potential
       Mass point model surface
     174 desc
       Cumulonimbus horizontal extent
       Velocity point model surface
     175 desc
       Pressure at cumblonimbus base
       Model layer number from bottom up
     176 desc
       Pressure at cumblonimbus top
       Latitude -90 to +90
     177 desc
       Pressure at embedded cumblonimbus base
       East longitude 0-360
     178 desc
       Pressure at embedded cumblonimbus top
       Ice mixing ratio
     179 desc
       ICAO height at cumblonimbus base
       Graupel mixing ratio
     181 desc
       ICAO height at embedded cumblonimbus base
       x-gradient of log pressure
     182 desc
       ICAO height at embedded cumblonimbus top
       y-gradient of log pressure
 - so where the %^$#! did  tablesOld/nceptab_140.tab come from ??

 9/2/2011
  - file from 7-8-2 (NCEP/Aviation Weather) has param=236 not found in dss/WMO_GRIB1.7-0.2.xml, but is in ncep/table2.htm.
  - i had previously noticed that dss ncep tables were missing some entries.

 9/2/2011
  - complete uses from example files:
  all:
         7-0-0: count = 9
       7-0-129: count = 276
       7-0-130: count = 54
         7-0-2: count = 20603
         7-1-2: count = 1
     7-138-130: count = 37
      7-15-131: count = 903
         7-2-2: count = 7170
         7-4-3: count = 16
         7-8-2: count = 120
        74-0-2: count = 45
      78-255-2: count = 3

  local
        7-0-129: count = 276
        7-0-130: count = 54
          7-0-2: count = 3001
      7-138-130: count = 17
       7-15-131: count = 273
          7-8-2: count = 120
         74-0-2: count = 1

  - with dss:
  missing
        7-0-129: count = 21
          7-0-2: count = 1
      7-138-130: count = 4
          7-8-2: count = 60
         74-0-2: count = 1

  - if we remove tables by dss, and use only ncep:
 missing
         7-0-2: count = 3001
     7-138-130: count = 1
         7-8-2: count = 120
        74-0-2: count = 1


  - if we only use ncep/table2.htm, all missing values disappear. of course we dont know if they are right, all this says is that table2 has entries for
    0-255.

 - note that http://www.nco.ncep.noaa.gov/pmb/docs/on388/table2.html has html for:
    Parameter Table Version 128
    Parameter Table Version 129
    Parameter Table Version 130
    Parameter Table Version 131
    Parameter Table Version 133
    Parameter Table Version 140
    Parameter Table Version 141
   but im too lazy to scrape it, im waiting for ncep to give me the damn tables.

 - conclusion is to wait on any further work on center 7.

 9/7/2011 rename resources/grib1/ncep drectory to wgrib


