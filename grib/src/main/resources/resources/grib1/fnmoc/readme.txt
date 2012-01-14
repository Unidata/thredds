notes for tables in fnmoc subdir

11/16/2011 tables sent by mary.clifford@navy.mil 11/15/2011
  - grib1 files are now in. very long description, can not use in names.

01/13/2012 also need to get the following tables for FNMOC:
  - TABLE A (Generating Process or Model from Originating Center)
    resources\grib1\fnmoc\US058MMTA-ALPdoc.pntabs-prodname-masterModelTableOrdered.GRIB1.TblA.xml

  - Table B (Master Geometry Table) is hopefully redundant to the GDS info

  - Table 3 (Master Level Type Table)
    resources\grib1\fnmoc\US058MMTA-ALPdoc.pntabs-prodname-masterLevelTypeTableOrdered.GRIB1.Tbl3.xml

01/14/2012 reading Table A
  - missing gentype = 78, eg E:/fnmoc/FNMOC_NCODA_Global_Ocean_20120109_1200.grib1
  - apparently "Navy Coupled Ocean Data Assimilation (NCODA) Model"

01/14/2012 check sample of all FNMOC models on  IDD
  - all parameters are found, using US058MMTA-ALPdoc.pntabs-prodname-masterParameterTableOrdered.GRIB1.Tbl2.xml
    as local table. can assume correct for current feed, problematic for archives.

    collection = E:/fnmoc/.*grib1
     E:/fnmoc/FNMOC_COAMPS_Central_America_20120109_0000.grib1
     E:/fnmoc/FNMOC_COAMPS_Eastern_Pacific_20120109_0000.grib1
     E:/fnmoc/FNMOC_COAMPS_Europe_20120109_0000.grib1
     E:/fnmoc/FNMOC_COAMPS_Western_Atlantic_20120109_0000.grib1
     E:/fnmoc/FNMOC_FAROP_Global_1p0deg_20120104_1800.grib1
     E:/fnmoc/FNMOC_FAROP_Global_1p0deg_20120109_0000.grib1
     E:/fnmoc/FNMOC_NCODA_Global_Ocean_20120109_1200.grib1
     E:/fnmoc/FNMOC_NOGAPS_Global_1p0deg_20120104_1800.grib1
     E:/fnmoc/FNMOC_NOGAPS_Global_1p0deg_20120109_0000.grib1
     E:/fnmoc/FNMOC_WW3_Global_1p0deg_20120109_0000.grib1
     E:/fnmoc/FNMOC_WW3_Mediterranean_20120109_0000.grib1

    E:/fnmoc/.*grib1 true showLocalParams
    top dir = E:/fnmoc
    Check Grib-1 Parameter Tables for local entries

     E:/fnmoc/FNMOC_COAMPS_Central_America_20120109_0000.grib1
      local parameter = terr_ht_surface (238) units=m
    total=17 local = 1 miss=0

     E:/fnmoc/FNMOC_COAMPS_Eastern_Pacific_20120109_0000.grib1
      local parameter = grnd_sea_temp_surface (133) units=deg_K
      local parameter = terr_ht_surface (238) units=m
    total=18 local = 2 miss=0

     E:/fnmoc/FNMOC_COAMPS_Europe_20120109_0000.grib1
      local parameter = terr_ht_surface (238) units=m
    total=19 local = 1 miss=0

     E:/fnmoc/FNMOC_COAMPS_Western_Atlantic_20120109_0000.grib1
      local parameter = grnd_sea_temp_surface (133) units=deg_K
      local parameter = terr_ht_surface (238) units=m
    total=18 local = 2 miss=0

     E:/fnmoc/FNMOC_FAROP_Global_1p0deg_20120104_1800.grib1
      local parameter = aero_optdep_surface (180) units=numeric
      local parameter = aero_optdep_sm_surface (198) units=numeric
      local parameter = aero_optdep_su_surface (199) units=numeric
      local parameter = aero_optdep_du_surface (201) units=numeric
    total=4 local = 4 miss=0

     E:/fnmoc/FNMOC_FAROP_Global_1p0deg_20120109_0000.grib1
      local parameter = aero_optdep_surface (180) units=numeric
      local parameter = aero_optdep_sm_surface (198) units=numeric
      local parameter = aero_optdep_su_surface (199) units=numeric
      local parameter = aero_optdep_du_surface (201) units=numeric
    total=4 local = 4 miss=0

     E:/fnmoc/FNMOC_NCODA_Global_Ocean_20120109_1200.grib1
    total=2 local = 0 miss=0

     E:/fnmoc/FNMOC_NOGAPS_Global_1p0deg_20120104_1800.grib1
      local parameter = grnd_sea_temp_surface (133) units=deg_K
      local parameter = grnd_wet_surface (218) units=fraction
      local parameter = snsb_ltnt_heat_flux_surface (221) units=W/m2
      local parameter = ttl_heat_flux_surface (222) units=W/m2
      local parameter = terr_ht_surface (238) units=m
      local parameter = peak_wnd_spd_height_above_ground (248) units=m/s
    total=59 local = 6 miss=0

     E:/fnmoc/FNMOC_NOGAPS_Global_1p0deg_20120109_0000.grib1
      local parameter = grnd_sea_temp_surface (133) units=deg_K
      local parameter = grnd_wet_surface (218) units=fraction
      local parameter = snsb_ltnt_heat_flux_surface (221) units=W/m2
      local parameter = ttl_heat_flux_surface (222) units=W/m2
      local parameter = terr_ht_surface (238) units=m
      local parameter = peak_wnd_spd_height_above_ground (248) units=m/s
    total=59 local = 6 miss=0

     E:/fnmoc/FNMOC_WW3_Global_1p0deg_20120109_0000.grib1
      local parameter = wcap_prbl_surface (155) units=%
      local parameter = peak_wav_dir_surface (207) units=deg
      local parameter = peak_wav_per_surface (208) units=s
      local parameter = max_wav_ht_surface (220) units=m
    total=15 local = 4 miss=0

     E:/fnmoc/FNMOC_WW3_Mediterranean_20120109_0000.grib1
      local parameter = wcap_prbl_surface (155) units=%
      local parameter = peak_wav_dir_surface (207) units=deg
      local parameter = peak_wav_per_surface (208) units=s
      local parameter = max_wav_ht_surface (220) units=m
    total=15 local = 4 miss=0

    Grand total=230 local = 34 missing = 0
