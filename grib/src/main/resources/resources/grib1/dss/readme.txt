DSS GRIB1 table notes

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

 9/2/2011
   - comparision of random table (98-0-140) from ECMWF GRIBEX with DSS tables: missing 5 params in DSS
    **No key 200 (GridParameter{number=200, name='MAXSWH', description='Maximum of significant wave height', unit='m'}) in first table
    **No key 217 (GridParameter{number=217, name='TMAX', description='Period corresponding to maximum individual wave height', unit='s'}) in first table
    **No key 218 (GridParameter{number=218, name='HMAX', description='Maximum individual wave height', unit='m'}) in first table
    **No key 253 (GridParameter{number=253, name='BFI', description='Benjamin-Feir index', unit='-'}) in first table
    **No key 254 (GridParameter{number=254, name='WSP', description='Wave spectral peakedness', unit='s**-1'}) in first table
  - use tables from ECMWF GRIBEX, remove DSS tables for center 98

 9/2/2011
  - ignore center 7, whats missing is:
      60-0-129: count = 1
        60-0-2: count = 49
        60-1-2: count = 2

 11/01/2011
  - WMO_GRIB1.xml param 39 and 40 both have name "Vertical velocity" with different units.
    Change 39 to "Pressure vertical velocity"


