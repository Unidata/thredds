# grib2/readme.txt

03/03/2011 Atsushi (AShimazaki@wmo.int) sends me XML tables.
 - here is where they apparently live at WMO, see link on this page:
   http://www.wmo.int/pages/prog/www/WMOCodes/TDCFtables.html#TDCFtables

12/7/2011
 - edward glen (edward.glen@metservice.com) sends a file from center (canadian met) with parameter 0-4-192.
   from this page: http://www.weatheroffice.gc.ca/grib/what_is_GRIB_e.html referencing NCEP (at bottom), Ive decided to
    map all of center 54 to NCEP tables.

12/8/2011
 - jeff.brogden@noaa.gov sent me a new HRRR (hi res rapid refresh) model output E:/work/brogden/hrrr_20111207-1400F0000.grib2
   center is 59 with lots of local params. i am going to assume all FSL files should use NCEP center 7 tables

12/8/2011 new WMO tables found from http://www.wmo.int/pages/prog/www/WMOCodes/TDCFtables.html#TDCFtables
 - GRIB2 8.0
 - BUFR 17.0 (not integrated yet)
 - Common Codes 02Nov11
 - move older tables into src/main/sources to reduce jar size

 01/30/2011
 - correction to 0-15-3 (Vertically Integrated Liquid)

    Hello John,

    The correction was proposed by NOAA/NCEP.  It was reported that the proposer has not seen the data in km/m.  You can therefore practically correct the unit.
    The approval process by WMO on the correction is underway, but I think it will be corrected in May 2012.

    Best regards,
    Atsushi

    On 30 January 2012 17:35, John Caron <caron@unidata.ucar.edu> wrote:
    >
    > Hi Atsushi:
    >
    > I have a report that the parameter 0-15-3 (Vertically Integrated Liquid)
    >
    > which has units of kg/m in
    >
    >   http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/2011edition/WMO306_vI2_GRIB2_CodeFlag_en.pdf
    >
    > should have units of
    >
    >   kg/m2
    >
    > What do you think? Does everyone already use kg/m2 so we should just correct all version of the tables?


