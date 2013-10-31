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

10/31/2013 pull request from Ian Will fixing issues with FNMOC grib1 table
/resources/grib1/fnmoc/US058MMTA-ALPdoc.pntabs-prodname-masterParameterTableOrdered.GRIB1.Tbl2.xml
merged into 4.3.20. also add to 4.4.0

explanation from Ian:

"These fixes don't change any parameter mappings. They remove duplicate grib1Id values in the XML, which appear to be clear mistakes in the original XML.
Duplicate names were listed subsequent to the unqualified name, but the Grib1 logic doesn't appear to expect duplicates and uses the last occurring XML
entry for a given grib1Id. The removed names were all clearly derived from the first listing (e.g. ttl_prcp, ttl_prcp_01, ttl_prcp_03, ttl_prcp_06,
ttl_prcp_12 were all listed as unique entries for the same id 61, and ttl_prcp was kept). It appears that the original XML was trying to encode
certain types of information (like deprecated naming, or nuanced-interpretation) that do not belong in the GRIB parameter table.

The only change that slightly deviates from this pattern was for grib1Id 2 which was listed as "pres," causing conflict with grib1Id 1 which is also
listed as "pres." The description provided for id 2 indicates that this should be interpreted as pressure reduced to MSL. Thus a variation of the name "pres"
 was used to differentiate it from parameter 1, avoiding errors when reading GRIB files contain records with both 1 and 2. This interpretation of id 2 is
 consistent with the old parameter table definitions used in netcdf 4.2.

Summary: These are not new mappings, just fixes to the XML file. "