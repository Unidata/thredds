NOAA RFS (River Forecast Center) GRIB1 table notes

8/30/2011
  - David Blodgett sends me QPE.20101005.009.157
  "This is a grib-1 file from a river forecasting center (RFC) that contains hourly and 6-hour accumulated precipitation estimates."
  - apparently the 009.157 means center 9 (US NWS - other), subcenter 157 (the RFC that its from) 
  - looking at it from CDM 4.2, there are 2 parameters:

   float Total_precipitation(time=28, y=350, x=450);
     :units = "kg/m^2";
     :long_name = "Total_precipitation_Accumulation (Accumulation for  Mixed Intervals) @ surface";
     :cell_methods = "time: sum";
     :missing_value = -9999.0f; // float
     :grid_mapping = "Polar_Stereographic";
     :GRIB_param_short_name = "APCP";
     :GRIB_center_id = 9; // int
     :GRIB_table_id = 2; // int
     :GRIB_param_number = 61; // int
     :GRIB_param_id = 1, 9, 2, 61; // int

   float Total_column_ozone_concentration(time1=24, y=350, x=450);
     :units = "Dobson";
     :long_name = "Total_column_ozone_concentration_Accumulation (Accumulation for 1 hours Intervals) @ surface";
     :cell_methods = "time1: sum";
     :missing_value = -9999.0f; // float
     :grid_mapping = "Polar_Stereographic";
     :GRIB_param_name = "Total_column_ozone_concentration";
     :GRIB_param_short_name = "OZONE";
     :GRIB_center_id = 9; // int
     :GRIB_table_id = 128; // int
     :GRIB_param_number = 237; // int
     :GRIB_param_id = 1, 9, 128, 237; // int
     :GRIB_product_definition_type = "Initialized analysis product";
     :GRIB_level_type = 1; // int
     :GRIB_VectorComponentFlag = "gridRelative";

  - this second parameter looks rather bogus, whats ozone doing in a river forecast ??
  - whats actually happening is the usual misuse of tables - 237 is ozone in ncep tables.

  
8/30/2011 Correspond with David B., i said:

   Encoded in the files we have, we are seeing the following:
	center=57, subcenter=157, version=2, param= 61
	center=57, subcenter=157, version=128, param= 237

  - im not sure where i got center 57, the correct center is 9.


9/8/2011 from John Halquist <John.Halquist@noaa.gov>
	Development and Operations Officer
	National Operational Hydrologic Remote Sensing Center, NOAA/NWS

  "Attached is the Fortran BLOCK DATA sections that define the grib parameters that are used in the radar grib1 files (converted from the original xmrg's)"
  - presumbly for center=57, subcenter=157 (?) because thatst he example file i had
  - in the file:
	c-1:9:-1:A      TABLE A - GENERATING PROCESS OR MODEL - ORIGINATING CENTER 9
	c-1:9:-1:C      TABLE C - SUB-CENTERS FOR CENTER 9  US NWS FIELD STATIONS
	c-1:7:-1:2       TABLE 2 - PARAMETERS & UNITS FOR CENTER 7   US NWS NCEP
	c-1:9:-1:128     TABLE 128 - PARAMETERS & UNITS FOR CENTER 9  US NWS FIELD STATIONS

9/12/2011 reviewing:
  - CDM 4.3 is showing that that parameter is missing, because we dont have anything for center 9.
  - add center 9, version 128 table from John's fortran block: noaa_rfc/params9-128.tab
  - check their "version 2 entries" against standard: all ok (for once!)
  - things look good now. note that parameter 61 has mixed intervals, 237 has hourly intervals.
  
9/13/2011 from John:
You are correct, wgrib also misinterprets these parameters, but I was able to get the right decoding by using an external user defined table with wgrib.

The table you are including should work for any center 9/128 grib1.  However, the table is specifically from the program used at the NWS RFC's to
convert xmrg formatted data into grib1 for transmission.  Based on all of the grib1 tables that I looked through, these parameters are consistent
within the NWS field offices (i.e. Center 9 - US NWS FIELD STATIONS) and differ from those in use at NCEP, CPC/CDC and other Centers.

12/14/2011 work with jordan walker on noaa regional QPE data ftp://ftp.hpc.ncep.noaa.gov/npvu/rfcqpe/
  1) seeing lots of duplicate records - different WMO headers
   wmo headers look like ZETA98 KTIR 051320
                         123456 CCCC DDHHMM
    "13-14 Two numeric characters providing the reference day of the month (01-31) of the data.
     15-18 Four numeric characters providing the reference hour and minute of the data."
     http://www.nco.ncep.noaa.gov/pmb/docs/on388/appendixa.html

     however, these do not show reference date, but possibly date written.
     assume that we should take the last record seen.

  2) mixed intervals - should be seperate variables (?)  how uniform ??
     maybe:
        http://www.hpc.ncep.noaa.gov/npvu/confpres/hydro15/preprint_adj.pdf
        http://origin.hpc.ncep.noaa.gov/npvu/help/

    eg: E:/work/jordan/qpe/QPE.20111204.009.160

    ************************

    there are 3 problems to check with the data producers:

1) lots of duplicate records. I will assume that these are updated values, and that the last one seen should be used.

2) mixed intervals: eg QPE.20111204.009.160 has 2 variables,

float Total_precipitation_surface_Accumulation(time1=14, y=260, x=250);
     :long_name = "Total precipitation (Accumulation) @ surface";
     :units = "kg.m-2";
     :missing_value = NaNf; // float
     :grid_mapping = "PolarStereographic_Projection";
     :Grib_Parameter = 61; // int
     :Grib_Level_Type = 1; // int
     :Grib_Statistical_Interval_Type = 4; // int
     :cell_methods = "time1: sum";

has "mixed interval" times (in hours since some base date):

time1 =

(0.000000,1.000000) = 1.000000
(1.000000,2.000000) = 1.000000
(2.000000,3.000000) = 1.000000
(3.000000,4.000000) = 1.000000
(4.000000,5.000000) = 1.000000
(5.000000,6.000000) = 1.000000
(6.000000,7.000000) = 1.000000
(7.000000,8.000000) = 1.000000
(8.000000,9.000000) = 1.000000
(9.000000,10.000000) = 1.000000
(12.000000,18.000000) = 6.000000
(18.000000,24.000000) = 6.000000
(24.000000,30.000000) = 6.000000
(30.000000,36.000000) = 6.000000

but the other variable just has 1 hour intervals out 24 hours:

(0.000000,1.000000) = 1.000000
(1.000000,2.000000) = 1.000000
(2.000000,3.000000) = 1.000000
(3.000000,4.000000) = 1.000000
(4.000000,5.000000) = 1.000000
(5.000000,6.000000) = 1.000000
(6.000000,7.000000) = 1.000000
(7.000000,8.000000) = 1.000000
(8.000000,9.000000) = 1.000000
(9.000000,10.000000) = 1.000000
(10.000000,11.000000) = 1.000000
(11.000000,12.000000) = 1.000000
(12.000000,13.000000) = 1.000000
(13.000000,14.000000) = 1.000000
(14.000000,15.000000) = 1.000000
(15.000000,16.000000) = 1.000000
(16.000000,17.000000) = 1.000000
(17.000000,18.000000) = 1.000000
(18.000000,19.000000) = 1.000000
(19.000000,20.000000) = 1.000000
(20.000000,21.000000) = 1.000000
(21.000000,22.000000) = 1.000000
(22.000000,23.000000) = 1.000000
(23.000000,24.000000) = 1.000000

3) They also have mixed reference dates. perhaps they are misusing this?

12/14/2011 checking IDD pqact:
    # Other ZETA assorted binary products
                   1      2           3               4
    HRS     ZETA98 (....) ([0-3][0-9])([0-2][0-9]).*/m(.......)
            FILE    -metadata
    data/pub/native/grid/RFC/zeta/\1/ZETA_\1_\4_(\2:yyyy)(\2:mm)\2_\300.grib1

     example:
            1    2 3            1
     ZETA98 KALR 310700 -> ZETA_KALR_NWS_152_20111031_0700.grib1
     123456 CCCC DDHHMM

     ZETA98 KALR 300035 -> ZETA_KALR_NWS_152_20111030_0000.grib1


    should be:

     data/pub/native/grid/RFC/zeta/\1/ZETA_\1_\4_(\2:yyyy)(\2:mm)\2_\300.grib1
     data/pub/native/grid/RFC/\1/RFC_\1_(\2:yyyy)(\2:mm)\2.grib1

     NPVU_RFC_KALR_20111030.grib1

12/16/2011
  - wasnt getting all of the center 9 subcenters from bldparm.f read correctly

12/23/2011 email from jordan:
> what are the 3? stations you are interested in?
I'm not absolutely sure, but I believe we are interested both "total precip" and "1-hr qpe" if you are able to put them up.
As far as the stations, we are interested the North Central, Northeast, and Ohio River Forecasting Centers.
The numbers on those are 157, 158, and 160.

# from bdgparm.f John Halquist <John.Halquist@noaa.gov> 9/12/2011
# thredds\grib\src\main\resources\resources\grib1\noaa_rfc\tableC.txt

     +'150:KTUA:Arkansas-Red River RFC Tulsa OK                      ',
     +'151:PACR:Alaska-Pacific RFC Anchorage AK                      ',
     +'152:KSTR:Colorado Basin RFC Salt Lake City UT                 ',
     +'153:KRSA:California-Nevada RFC Sacramento CA                  ',
     +'154:KORN:Lower Mississippi RFC Slidel LA                      ',
     +'155:KRHA:Middle Atlantic RFC State College PA                 ',
     +'156:KKRF:Missouri Basin RFC Pleasant Hill MO                  ',
     +'157:KMSR:North Central RFC  Chanhassen MN                     ',
     +'158:KTAR:Northeast RFC Taunton MA                             ',
     +'159:KPTR:Northwest RFC Portland OR                            ',
     +'160:KTIR:Ohio Basin RFC Wilmington OH                         ',
     +'161:KALR:Southeast RFC Peachtree GA                           ',
     +'162:KFWR:West Gulf RFC Fort Worth TX                          ',
     +'163:NOHR:Chanhassen MN                                        ',
     +'170:KNES:Satellite Analysis Branch                            ',


01/11/2012 got RFC files from wcfields.
 - pqact:

 # Other ZETA assorted binary products
 HRS     ZETA98 (....) ([0-3][0-9])([0-2][0-9]).*/m(.......)
        FILE    -metadata
  data/pub/native/grid/RFC/zeta/\1/ZETA_\1_\4_(\2:yyyy)(\2:mm)\2_\300.grib1

 - example records are:
 1) header = ZETA98 KNES 111616 /mNWS_190 !grib/nws/NWS_190/#255/201201111500/F001/APCP/sfc/
 - file = 14 E:/ncep/RFC/ZETA_KNES_NWS_190_20120111.grib1
            Originating Center : (9) US National Weather Service - Other
         Originating SubCenter : (170) KNES: Satellite Analysis Branch
                 Table Version : 2
               Parameter Table : (9-170-2) resources/grib1/dss/WMO_GRIB1.xml
                Parameter Name : (61) null
                Parameter Desc : Total precipitation
               Parameter Units : kg.m-2
       Generating Process Type : (190) Satellite Autoestimator Precipitation

  so the subcenter is being correctly encoded

compare to:
 2) header = ZETA98 KWNE 111200 /mRFC_QPE !grib/ncep/RFC_QPE/#240/201201111200/F006/APCP/sfc/ .

 file = 39 E:/ncep/RFC/ZETA_KWNE_RFC_QPE_20120110.grib1
            Originating Center : (7) US National Weather Service - National Centres for Environmental Prediction (NCEP)
         Originating SubCenter : (4) Environmental Modeling Center
                 Table Version : 2
               Parameter Table : (7-4-2) resources/grib1/dss/WMO_GRIB1.7-4.2.xml
                Parameter Name : (61) A_PCP
                Parameter Desc : Total precipitation
               Parameter Units : kg.m-2
       Generating Process Type : (182) River Forecast Center Quantitative Precipitation estimate mosaic generated by NCEP

file = 40 E:/ncep/RFC/ZETA_KWNE_RFC_QPE_20120111.grib1
            Originating Center : (7) US National Weather Service - National Centres for Environmental Prediction (NCEP)
         Originating SubCenter : (4) Environmental Modeling Center
                 Table Version : 2
               Parameter Table : (7-4-2) resources/grib1/dss/WMO_GRIB1.7-4.2.xml
                Parameter Name : (61) A_PCP
                Parameter Desc : Total precipitation
               Parameter Units : kg.m-2
       Generating Process Type : (182) River Forecast Center Quantitative Precipitation estimate mosaic generated by NCEP


