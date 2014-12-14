
12/09/2014 caron - download from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
  The latest versions 14(.0.0) (GRIB edition 2) and 23(.0.0) (BUFR and CREX) are effective as from 5 November 2014.
  - put BUFRCREX_22_0_1.zip zip file into src/main/sources/wmo/
  - unzip and put BUFR_22_0_1_Table(A|C|D)_en.xml, BUFRCREX_22_0_1_(CodeFlag|TableB)_en.xml  into resources/bufrTables/wmo
  - modify resources/bufrTables/local/tablelookup.csv
  - modify ucar.nc2.iosp.bufr.tables.CodeFlagTables, ucar.nc2.iosp.bufr.tables.WmoXmlReader, ucar.nc2.iosp.bufr.tables.TableA