NCL GRIB1 table notes

 9/7/2011 download NCL version 6.0.0 from ESG website.
   The grib1 tables are in ncl_ncarg-6.0.0/ni/src/ncl/*gtb*.h; copy to resources/grib1/ncl
   also see:
    - NCL describes their processing of GRIB: http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB
    - NCL describes their GRIB1 table handling : http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB1-built-in-parameter-tables

  Compare a few random tables:

 1)Grib1ParamTable{center_id=98, subcenter_id=-1, version=128, name='ECMWF local table 2: Version Number 128 (Standard).', path='resources/grib1/ecmwf/local_table_2_version_128'}
   Grib1ParamTable{center_id=0, subcenter_id=0, version=0, name='ecmwf_128_gtb.h', path='C:/dev/github/thredds/grib/src/main/resources/resources/grib1/ncl/ecmwf_128_gtb.h'}
   20 desc
     Clear sky surface photosynthetically active radiation
     Clear sky surface PAR
  **No key 61 (GridParameter{number=61, name='TPO', description='Total precipitation from observations', unit='Millimetres.100.+.number.of.stations'}) in second table
   77 desc
     Vertical velocity in the hybrid eta vertical coordinate system
     Eta-coordinate vertical velocity
   156 desc
     Gepotential Height
     Height
   232 cleanUnits
     kg.m-2.s
     kg.m-2.s-1
  **No key 255 (GridParameter{number=255, name='-', description='Indicates a missing value', unit=''}) in second table
  **No key 999 (GridParameter{number=999, name='.', description='', unit='null'}) in second table

  ***Check if entries are missing in first table
  **No key 75 (GridParameter{number=75, name='CRWC', description='Cloud rain water content', unit='kg.kg-1'}) in first table
  **No key 76 (GridParameter{number=76, name='CSWC', description='Cloud snow water content', unit='kg.kg-1'}) in first table

 2)Compare
 Grib1ParamTable{center_id=34, subcenter_id=-1, version=3, name='WMO_GRIB1.34-0.3.xml', path='resources/grib1/dss/WMO_GRIB1.34-0.3.xml'}
 Grib1ParamTable{center_id=0, subcenter_id=0, version=0, name='jma_3_gtb.h', path='C:/dev/github/thredds/grib/src/main/resources/resources/grib1/ncl/jma_3_gtb.h'}
 146 desc
   Cloud workfunction
   Cloud work function
 162 desc
   Clear sky upward longwave flux
   Clear sky upward long wave flux
 163 desc
   Clear sky downward longwave flux
   Clear sky downward long wave flux
 204 desc
   Downward shortwave radiation flux
   Downward short wave radiation flux
 205 desc
   Downward longwave radiation flux
   Downward long wave radiation flux
 211 desc
   Upward shortwave radiation flux
   Upward short wave radiation flux
 212 desc
   Upward longwave radiation flux
   Upward long wave radiation flux

11/21/2011 remove ncep 128-141 from table, use screen scrape

