WRF GRIB1 table notes

9/7/2011 sent to use from wrfhelp
  "see attached. Here gribmap.txt is for grib1 output"

9/12/2011 download wrf version 3.3 from http://www.mmm.ucar.edu/wrf/users/download/get_sources.html
  - found gribmap.txt in the run directory
  - also grb2map.tbl, apparenty for grib2
  - in external directoy, there are io_grib1, io_grib2, io_grib_share. also io_netcdf etc
  - io_grib1 has wgrib and mel_grib1

9/12/2011 took apart gribmap.txt. In the NCEP "tab" format, so broke into files:
  - wrfN.tab, seems to be 255:255:N
  - wrf7-129.tab for 7:-1:129
  - wrf7-130.tab for 7:-1:130

9/12/2011
 - wrf7-129 agrees mostly with ncl/ncep_129_gtb.h, execpt latter has more entries. entry 142 seems to be an exception
 - wrf2 agrees mostly with local/wrf_amps.wrf (from amps group), except:
     248 udunits
        kg/m2/s
        m
      249 desc
        Ground Reservoir Temperature
        Inversion height
      249 udunits
        K
        m
      253 desc
        Cumulative Large scale precipitation
        Column integrated cloud ice
      254 desc
        Cumulative Convective precipitation
        Column integrated cloud liquid water

9/12/2011 from WRFV3/external/io_grib1/README.io.grib1:
 Examining GRIB output with wgrib: Define the GRIBTAB environment variable:
           export GRIBTAB=<your WRF dir>/run/gribmap.txt
 - so apparently best practice is for the user to find the grib tables that were used in writing, and pass them to wgrib
 - that implies that GRIB1 output is using then center ids and table versions (taken from gribmap.txt):
  # resources\grib1\wrf\lookupTables.txt
    7:     -1:   129: wrf7-129.tab
    7:     -1:   130: wrf7-130.tab
    255:   255:   2:  wrf2.tab
    255:   255:   3:  wrf3.tab
    255:   255:   4:  wrf4.tab
    255:   255:   5:  wrf5.tab
    255:   255:   6:  wrf6.tab

9/12/2011 examine cdmUnitTest/formats/grib1/07111906_nmm.GrbF00000, from Chiz' WRF runs:
  tables used:

  CHECK TABLES

  table
        7-0-129: count = 56
        7-0-130: count = 11
          7-0-2: count = 725

  local
        7-0-129: count = 56
        7-0-130: count = 11
          7-0-2: count = 219

  missing

  The tables are from dss or ncl though, not WRF. There a lot of differences in the 7-0-2 vs 255-255-2, assuming thats the default.
  They are pretty much different above 127. (

  Compare
 Grib1ParamTable{center_id=7, subcenter_id=-1, version=2, name='WMO_GRIB1.7-0.2.xml', path='resources/grib1/dss/WMO_GRIB1.7-0.2.xml'}
 Grib1ParamTable{center_id=255, subcenter_id=255, version=2, name='wrf2.tab', path='resources/grib1/wrf/wrf2.tab'}

 Compare
  Grib1ParamTable{center_id=7, subcenter_id=-1, version=2, name='WMO_GRIB1.7-0.2.xml', path='resources/grib1/dss/WMO_GRIB1.7-0.2.xml'}
  Grib1ParamTable{center_id=0, subcenter_id=0, version=0, name='nceptab_2.tab', path='C:/dev/github/thredds/grib/src/main/resources/resources/grib1/tablesOld/nceptab_2.tab'}
  4 udunits
    K.m2.kg-1.s-1
    km2/kg/s
  5 udunits
    m
    M
  58 udunits
    kg.m-2
    kg/kg
  76 udunits
    kg.m-2
    kg/kg
  119 udunits
    W.srm-2
    W/m/sr
  120 udunits
    W.srm-2
    W/m3/sr
  180 udunits

    m/s
  190 udunits
    m.s-1.Pa.s-1
    m2/s2
  191 udunits
    m.s-1.Pa.s-1
    non-dim
  192 udunits
    m.s-1.gm.gm-1
    non-dim
  193 udunits
    m.s-1.gm.gm-1
    %
  194 udunits
    K.Pa.s-1
    %
  195 udunits
    gm.gm-1.Pa.s-1
    %
  196 udunits
    m2.s-2
    m/s
  197 udunits
    K.m.s-1
    m/s
  198 udunits
    K.m.s-1

  199 udunits

    W/m2
  200 udunits

    W/m2
  209 udunits

    integer
  210 udunits

    W/m2
  230 udunits

    gpm
  240 udunits

    K.m/s

 9/12/2011
   GRib1 Report (localTables) on F:/data/cdmUnitTest/formats/grib1/07111906_nmm.GrbF00000
   GRIB table = "7-0-2" == resources/grib1/dss/WMO_GRIB1.7-0.2.xml
   local parameter = MSLET_msl (130) units=Pa
   local parameter = LFTX_layer_between_two_isobariclayer (131) units=K
   local parameter = 4LFTX_layer_between_two_pressure_difference_from_groundlayer (132) units=K
   local parameter = CUEFI_entire_atmosphere (134) units=non-dim
   local parameter = MCONV_isobaric (135) units=kg.kg-1.s-1
   local parameter = VSSH_layer_between_two_heights_above_groundlayer (136) units=s-1
   local parameter = TCOLW_entire_atmosphere (136) units=kg/m2
   local parameter = TCOLI_entire_atmosphere (137) units=kg/m2
   local parameter = TCOLR_entire_atmosphere (138) units=kg/m2
   local parameter = TCOLS_entire_atmosphere (139) units=kg/m2
   local parameter = CRAIN_surface (140) units=
   local parameter = TCOLC_entire_atmosphere (140) units=kg/m2
   local parameter = CRFZR_surface (141) units=
   local parameter = PLPL_layer_between_two_pressure_difference_from_groundlayer (141) units=Pa
   local parameter = CICEP_surface (142) units=
   local parameter = CSNOW_surface (143) units=
   local parameter = SOILW_layer_between_two_depths_below_surfacelayer (144) units=fraction
   local parameter = PEVPR_surface (145) units=W.m-2
   local parameter = CLWMR_isobaric (153) units=kg.kg-1
   local parameter = GFLUX_surface (155) units=W.m-2
   local parameter = CIN_surface (156) units=J.kg-1
   local parameter = CIN_layer_between_two_pressure_difference_from_groundlayer (156) units=J.kg-1
   local parameter = CAPE_surface (157) units=J.kg-1
   local parameter = CAPE_layer_between_two_pressure_difference_from_groundlayer (157) units=J.kg-1
   local parameter = TKE_isobaric (158) units=J.kg-1
   local parameter = CSDSF_surface (161) units=W.m-2
   local parameter = RWMR_isobaric (170) units=
   local parameter = RLYRS_surface (171) units=non-dim
   local parameter = SNMR_isobaric (171) units=
   local parameter = GUST_surface (180) units=
   local parameter = CCOND_surface (181) units=m/s
   local parameter = CBUW_layer_between_two_heights_above_groundlayer (190) units=m.s-1.Pa.s-1
   local parameter = CBMZW_layer_between_two_heights_above_groundlayer (196) units=m2.s-2
   local parameter = CBTZW_layer_between_two_heights_above_groundlayer (197) units=K.m.s-1
   local parameter = RSMIN_surface (203) units=s/m
   local parameter = UVAR_surface (203) units=m2/s2
   local parameter = DSWRF_surface (204) units=W.m-2
   local parameter = VVAR_surface (204) units=m2/s2
   local parameter = DLWRF_surface (205) units=W.m-2
   local parameter = UVVCC_surface (205) units=m2/s2
   local parameter = MCLS_surface (206) units=m
   local parameter = MSTAV_layer_between_two_depths_below_surfacelayer (207) units=%
   local parameter = SFEXC_surface (208) units=(kg.m-3)(m.s-1)
   local parameter = USWRF_surface (211) units=W.m-2
   local parameter = REFD_isobaric (211) units=dbZ
   local parameter = REFD_height_above_ground (211) units=dbZ
   local parameter = REFD_hybrid (211) units=dbZ
   local parameter = ULWRF_surface (212) units=W.m-2
   local parameter = REFC_entire_atmosphere (212) units=dbZ
   local parameter = CPRAT_surface (214) units=kg.m-2.s-1
   local parameter = TTRAD_hybrid (216) units=K.s-1
   local parameter = WILT_surface (219) units=fraction
   local parameter = HPBL_surface (221) units=m
   local parameter = CNWAT_surface (223) units=kg.m-2
   local parameter = SOTYP_surface (224) units=
   local parameter = VGTYP_surface (225) units=
   local parameter = BMIXL_hybrid (226) units=m
   local parameter = SMREF_surface (230) units=fraction
   local parameter = SMDRY_surface (231) units=fraction
   local parameter = SNOWC_surface (238) units=%
   local parameter = POROS_surface (240) units=fraction
   local parameter = RCS_surface (246) units=fraction
   local parameter = RCT_surface (247) units=fraction
   local parameter = RCQ_surface (248) units=fraction
   local parameter = RCSOL_surface (249) units=fraction
   local parameter = SWHR_hybrid (250) units=K.s-1
   local parameter = LWHR_hybrid (251) units=K.s-1
   local parameter = CD_surface (252) units=
   local parameter = FRICV_surface (253) units=m.s-1
 total=135 local = 69 miss=0

 revert to old tables:
F:/data/cdmUnitTest/formats/grib1/07111906_nmm.GrbF00000
  GRIB table = "7-0-2" == resources/grib1/tablesOld/nceptab_2.tab
  local parameter = MSLET_msl (130) units=Pa
  local parameter = LFTX_layer_between_two_isobariclayer (131) units=K
  local parameter = 4LFTX_layer_between_two_pressure_difference_from_groundlayer (132) units=K
  local parameter = CUEFI_entire_atmosphere (134) units=
  local parameter = MCONV_isobaric (135) units=kg/kg/s
  local parameter = VWSH_layer_between_two_heights_above_groundlayer (136) units=1/s
  local parameter = TCOLW_entire_atmosphere (136) units=kg/m/m
  local parameter = TCOLI_entire_atmosphere (137) units=kg/m/m
  local parameter = TCOLR_entire_atmosphere (138) units=kg/m/m
  local parameter = TCOLS_entire_atmosphere (139) units=kg/m/m
  local parameter = CRAIN_surface (140) units=yes=1;no=0
  local parameter = TCOLC_entire_atmosphere (140) units=kg/m/m
  local parameter = CFRZR_surface (141) units=yes=1;no=0
  local parameter = PLPL_layer_between_two_pressure_difference_from_groundlayer (141) units=Pa
  local parameter = CICEP_surface (142) units=yes=1;no=0
  local parameter = CSNOW_surface (143) units=yes=1;no=0
  local parameter = SOILW_layer_between_two_depths_below_surfacelayer (144) units=fraction
  local parameter = PEVPR_surface (145) units=W/m2
  local parameter = CLWMR_isobaric (153) units=kg/kg
  local parameter = GFLUX_surface (155) units=W/m2
  local parameter = CIN_surface (156) units=J/kg
  local parameter = CIN_layer_between_two_pressure_difference_from_groundlayer (156) units=J/kg
  local parameter = CAPE_surface (157) units=J/kg
  local parameter = CAPE_layer_between_two_pressure_difference_from_groundlayer (157) units=J/kg
  local parameter = TKE_isobaric (158) units=J/kg
  local parameter = CSDSF_surface (161) units=W/m2
  local parameter = RWMR_isobaric (170) units=kg/kg
  local parameter = RLYRS_surface (171) units=non-dim
  local parameter = SNMR_isobaric (171) units=kg/kg
  local parameter = GUST_surface (180) units=m/s
  local parameter = CCOND_surface (181) units=m/s
  local parameter = HLCY_layer_between_two_heights_above_groundlayer (190) units=m2/s2
  local parameter = USTM_layer_between_two_heights_above_groundlayer (196) units=m/s
  local parameter = VSTM_layer_between_two_heights_above_groundlayer (197) units=m/s
  local parameter = RSMIN_surface (203) units=s/m
  local parameter = UVAR_surface (203) units=m2/s2
  local parameter = DSWRF_surface (204) units=W/m2
  local parameter = VVAR_surface (204) units=m2/s2
  local parameter = DLWRF_surface (205) units=W/m2
  local parameter = UVVCC_surface (205) units=m2/s2
  local parameter = MCLS_surface (206) units=m
  local parameter = MSTAV_layer_between_two_depths_below_surfacelayer (207) units=%
  local parameter = SFEXC_surface (208) units=(kg/m3)(m/s)
  local parameter = USWRF_surface (211) units=W/m2
  local parameter = REFD_isobaric (211) units=dbZ
  local parameter = REFD_height_above_ground (211) units=dbZ
  local parameter = REFD_hybrid (211) units=dbZ
  local parameter = ULWRF_surface (212) units=W/m2
  local parameter = REFC_entire_atmosphere (212) units=dbZ
  local parameter = CPRAT_surface (214) units=kg/m2/s
  local parameter = TTRAD_hybrid (216) units=K/s
  local parameter = WILT_surface (219) units=fraction
  local parameter = HPBL_surface (221) units=m
  local parameter = CNWAT_surface (223) units=kg/m2
  local parameter = SOTYP_surface (224) units=0..9
  local parameter = VGTYP_surface (225) units=0..13
  local parameter = BMIXL_hybrid (226) units=m
  local parameter = SMREF_surface (230) units=fraction
  local parameter = SMDRY_surface (231) units=fraction
  local parameter = SNOWC_surface (238) units=%
  local parameter = POROS_surface (240) units=fraction
  local parameter = RCS_surface (246) units=fraction
  local parameter = RCT_surface (247) units=fraction
  local parameter = RCQ_surface (248) units=fraction
  local parameter = RCSOL_surface (249) units=fraction
  local parameter = SWHR_hybrid (250) units=K/s
  local parameter = LWHR_hybrid (251) units=K/s
  local parameter = CD_surface (252) units=non-dim
  local parameter = FRICV_surface (253) units=m/s
total=135 local = 69 miss=0

Grand total=135 local = 69 missing = 0

differ:
  local parameter = HLCY_layer_between_two_heights_above_groundlayer (190) units=m2/s2
  local parameter = USTM_layer_between_two_heights_above_groundlayer (196) units=m/s
  local parameter = VSTM_layer_between_two_heights_above_groundlayer (197) units=m/s

  Conclusion: if real table is 255-255-2 from wrf, theres a whole lot of incorrect parameters.

9/13/2011
  -see http://strc.comet.ucar.edu/wrf/
  -see http://www.emc.ncep.noaa.gov/mmb/papers/chuang/2/wrfpost.txt

  from chiz runs on daffy (wrf_2.1.2.2)
  post_grib.conf


#     OCNTR is the originating center of the GRIB file. By default the
#     value is set to 7 which is used to designate NCEP; however, if you
#     plan on exchanging grib files between WFOs then it is recommended
#     that you use 9, which identifies the originating center as a NWS
#     field station. You can find a list of the current originating
#     centers in the wrf/docs directory (grib_origcenters.htm) and
#     on the SOO/STRC WRF EMS website.
#
#     http://strc.comet.ucar.edu/wrf/docs/grib_origcenters.htm
#
OCNTR = 07

#     SCNTR is the originating sub center of the GRIB file. By default the
#     value is set to 0 and should remain 0 if OCNTR = 7; however, if you
#     decide to change OCNTR to 9 to identify a NWS field office, then
#     SCNTR identifies WHICH office. If OCNTR is 7 then the list
#     of currently defined subcenters is provided in the wrf/docs
#     directory (grib_subcenters.htm). If you have OCNTR = 9 then look
#     at the wrf/docs/nwssubcenters.tbl file or on the SOO/STRC WRF EMS
#     website for the appropriate office ID.
#
#     http://strc.comet.ucar.edu/wrf/docs/nwssubcenters.tbl
#
SCNTR = 0

 - so its clear that WRF is generating files with center=7, subcenter=0 by default. but what tables do they use ?
 - must be gribmap.txt, but im trying to be sure

 [root@daffy wrf]# pwd
 /daffy/chiz/wrf/wrf
 [root@daffy wrf]# find . -name gribmap.txt -print
 ./data/conf/tables/gribmap.txt
 ./runs/2010011912_12km/gribmap.txt
 ./runs/2011091312_12km_alt1/gribmap.txt
 ./runs/2010090618_12km_alt1/gribmap.txt

  [root@daffy tables]# pwd
 /daffy/chiz/wrf/wrf/data/conf/tables

 -rwxr-xr-x 1 chiz ustaff  100745 2006-01-30 07:29 bufr_stations.parm
 -rwxr-xr-x 1 chiz ustaff   30200 2006-01-30 07:29 eta_micro_lookup.dat
 -rwxr-xr-x 1 chiz ustaff   30200 2006-01-30 07:29 ETAMPNEW_DATA
 -rwxr-xr-x 1 chiz ustaff     245 2006-01-30 07:29 GENPARM.TBL
 -rwxr-xr-x 1 chiz ustaff   27129 2006-01-30 07:29 gribmap.txt
 ....

 hasnt changed since 2006 ?

09/13/2011
  - compare gribmap.txt i got from chiz' wrf run (v 2.1.2.2) to the one sent to me (presumably from latest wrf 3.1)

  - in 2.1.2.2 the following headers are found:
    -1:250:2:200
    -1:7:-1:129
    -1:7:-1:130

  - in 3.1 we have:
   -1:255:255:2
   -1:255:255:3
   -1:255:255:4
   -1:255:255:5
   -1:255:255:6
   -1:7:-1:129
   -1:7:-1:130

   129 and 128 match.
   dunno what -1:250:2:200 means, but on the surface it means center=250, subcenter=2,version=200. this mostly matches -1:255:255:2 below <128,
     but then differes significantly.

   looking in an output file from chiz:

   table
         7-0-129: count = 52
         7-0-130: count = 10
           7-0-2: count = 737

   local
         7-0-129: count = 52
         7-0-130: count = 10
           7-0-2: count = 225

   probably 7-0-2 is taken from wgrib (?). but what is the role of -1:250:2:200 ??
   129 and 130 match. so probably if these tables match ncep canonical tables, then we are ok.
   OTOH, if users change the center/subcenter then WTF?

09/13/2011
 - somewhere they are mapping the WRF variables to these GRIB tables. In first table in gribmap.txt, there are some extra columns

    1:PRES:Pressure [Pa]:PRES,P,PSFC:2
    2:PRMSL:Pressure reduced to MSL [Pa]:PMSL:2
    3:PTEND:Pressure tendency [Pa/s]::
    4:PVORT:Pot. vorticity [km^2/kg/s]::
    5:ICAHT:ICAO Standard Atmosphere Reference Height [M]::
    6:GP:Geopotential [m^2/s^2]:PHP:3
    7:HGT:Geopotential height [gpm]:GHT,SOILHGT:2
    8:DIST:Geometric height [m]:HGT:4
    9:HSTDV:Std dev of height [m]::
    10:TOZNE:Total ozone [Dobson]::
    11:TMP:Temp. [K]:TT,T2,TSK,SKINTEMP:2

eg PRES,P,PSFC:2, perhaps these refer to the wWRF internal vars. note not all rows have the extra column.

E:/datasets/wrf/11091212_nmm.GrbF02800
  GRIB table = "7-0-2" == resources/grib1/dss/WMO_GRIB1.7-0.2.xml
  local parameter = MSLET_msl (130) units=Pa
  local parameter = LFTX_layer_between_two_isobariclayer (131) units=K
  local parameter = 4LFTX_layer_between_two_pressure_difference_from_groundlayer (132) units=K
  local parameter = CUEFI_entire_atmosphere (134) units=non-dim
  local parameter = MCONV_isobaric (135) units=kg.kg-1.s-1
  local parameter = TCOLW_entire_atmosphere (136) units=kg/m2
  local parameter = TCOLI_entire_atmosphere (137) units=kg/m2
  local parameter = TCOLR_entire_atmosphere (138) units=kg/m2
  local parameter = TCOLS_entire_atmosphere (139) units=kg/m2
  local parameter = CRAIN_surface (140) units=
  local parameter = TCOLC_entire_atmosphere (140) units=kg/m2
  local parameter = CRFZR_surface (141) units=
  local parameter = PLPL_layer_between_two_pressure_difference_from_groundlayer (141) units=Pa
  local parameter = CICEP_surface (142) units=
  local parameter = CSNOW_surface (143) units=
  local parameter = SOILW_layer_between_two_depths_below_surfacelayer (144) units=fraction
  local parameter = PEVPR_surface (145) units=W.m-2
  local parameter = CLWMR_isobaric (153) units=kg.kg-1
  local parameter = GFLUX_surface (155) units=W.m-2
  local parameter = GFLUX_surface_Average (155) units=W.m-2
  local parameter = CIN_surface (156) units=J.kg-1
  local parameter = CIN_layer_between_two_pressure_difference_from_groundlayer (156) units=J.kg-1
  local parameter = CAPE_surface (157) units=J.kg-1
  local parameter = CAPE_layer_between_two_pressure_difference_from_groundlayer (157) units=J.kg-1
  local parameter = TKE_isobaric (158) units=J.kg-1
  local parameter = RWMR_isobaric (170) units=
  local parameter = RLYRS_surface (171) units=non-dim
  local parameter = SNMR_isobaric (171) units=
  local parameter = GUST_surface (180) units=
  local parameter = CCOND_surface (181) units=m/s
  local parameter = CBUW_layer_between_two_heights_above_groundlayer (190) units=m.s-1.Pa.s-1
  local parameter = CBMZW_layer_between_two_heights_above_groundlayer (196) units=m2.s-2
  local parameter = CBTZW_layer_between_two_heights_above_groundlayer (197) units=K.m.s-1
  local parameter = MSTAV_layer_between_two_depths_below_surfacelayer (207) units=%
  local parameter = SFEXC_surface (208) units=(kg.m-3)(m.s-1)
  local parameter = USWRF_surface (211) units=W.m-2
  local parameter = USWRF_surface_Average (211) units=W.m-2
  local parameter = USWRF_atmosphere_top_Average (211) units=W.m-2
  local parameter = REFD_isobaric (211) units=dbZ
  local parameter = REFD_height_above_ground (211) units=dbZ
  local parameter = REFD_hybrid (211) units=dbZ
  local parameter = ULWRF_surface (212) units=W.m-2
  local parameter = ULWRF_surface_Average (212) units=W.m-2
  local parameter = ULWRF_atmosphere_top_Average (212) units=W.m-2
  local parameter = REFC_entire_atmosphere (212) units=dbZ
  local parameter = CDLYR_entire_atmosphere_Average (213) units=%
  local parameter = CPRAT_surface (214) units=kg.m-2.s-1
  local parameter = TTRAD_hybrid (216) units=K.s-1
  local parameter = WILT_surface (219) units=fraction
  local parameter = HPBL_surface (221) units=m
  local parameter = CNWAT_surface (223) units=kg.m-2
  local parameter = SOTYP_surface (224) units=
  local parameter = VGTYP_surface (225) units=
  local parameter = BMIXL_hybrid (226) units=m
  local parameter = SMREF_surface (230) units=fraction
  local parameter = SMDRY_surface (231) units=fraction
  local parameter = SNOWC_surface (238) units=%
  local parameter = POROS_surface (240) units=fraction
  local parameter = LRGHR_hybrid_Average (241) units=K.s-1
  local parameter = CNVHR_hybrid_Average (242) units=K.s-1
  local parameter = RCS_surface (246) units=fraction
  local parameter = RCT_surface (247) units=fraction
  local parameter = RCQ_surface (248) units=fraction
  local parameter = RCSOL_surface (249) units=fraction
  local parameter = SWHR_hybrid (250) units=K.s-1
  local parameter = LWHR_hybrid (251) units=K.s-1
  local parameter = CD_surface (252) units=
  local parameter = FRICV_surface (253) units=m.s-1
total=140 local = 68 miss=0

just looking at the first one:
  local parameter = MSLET_msl (130) units=Pa

2.1.2.2 has MSLET:Mean sea level pressure (ETA model) [Pa]::
3.1 has 130:LU_INDEX:Land Use Category:LU_INDEX:1

  looking at data, it appears to be MSLET.
  naively, i would have assigned -1:255:255:2 to it (any center.subcenter, version 2) ??


9/13/2011
 - see ftp://ftp.cpc.ncep.noaa.gov/wd51we/wgrib/usertables.txt

 -1:CENTER:SUBCENTER:PARAMETER_TABLE
0:name:comment
1:name:comment
...
255:name:comment

-1:CENTER:SUBCENTER:PARAMETER_TABLE
0:name:comment
1:name:comment
...
255:name:comment

  (etc.)
-----------------------------------------------------------------------

"CENTER" is number between 0 and 255 with -1 being a wildcard.
"SUBCENTER" is number between 0 and 255 with -1 being a wildcard.
"PARAMETER_TABLE" is number between 0 and 255 with -1 being a wildcard.
"name" is a string with the variable name "TMP".
"comment" is a string such describing the variable such as "temperature [K]"
Any missing parameter lines will be given a generic name and comment.

Each GRIB record contains the center (PDS octet 5), subcenter (PDS octet 26)
and parameter_table (PDS octet 4).  The names and comments from the
first section to match the center-subcenter-parameter_name in the PDS
are used.  Currently the subcenter are not used in matching the
built-in tables.  This will probably change when sub-centers start using
their own parameter tables.

 - so probably wrong in interpreting 255 as -1 (match any)

12/05/2011 run strict=true scan on Q:/cdmUnitTest/formats/grib1
 - fail:
  Q:\cdmUnitTest\formats\grib1\wrf.grib  ERR: Could not find a table for GRIB file with center: 60 subCenter: 0 version: 2
  center 60 = NCAR.
  real WRF output uses center = 7 (!)
  so WTF?





