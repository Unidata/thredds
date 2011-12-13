 ECMWF (GRIBEX) notes

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

 12/5/2011 look closer at the wmostd/* tables
  - all are identical except for minor spelling and capitilization
  - except table_2_version_003.mf which may be just a french translation
  - unit differences with dss/WMO_GRIB1.xml as above

 12/5/2011 look closer at the cen074/* tables
  - version 174 and 175 are identical



