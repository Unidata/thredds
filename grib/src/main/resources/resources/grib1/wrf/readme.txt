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
 - wrf2 agrees mostly with local/wrf_amps.wrf (from amps group), execpt:
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

