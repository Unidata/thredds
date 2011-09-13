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

