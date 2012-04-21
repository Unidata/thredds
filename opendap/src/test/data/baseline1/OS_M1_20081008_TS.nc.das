Attributes {
    LONGITUDE {
        String point_spacing "even";
        String axis "X";
        String units "degrees_east";
        String long_name "Longitude";
        String standard_name "longitude";
        String QC_procedure "5";
        String uncertainty "0.0001";
        String valid_min "-180";
        String valid_max "180";
        String modulo " ";
    }
    LATITUDE {
        String point_spacing "even";
        String axis "Y";
        String units "degrees_north";
        String long_name "Latitude";
        String standard_name "latitude";
        String QC_procedure "5";
        String uncertainty "0.0001";
        String valid_min "-90";
        String valid_max "90";
    }
    DEPTH {
        String units "METERS";
        String point_spacing "uneven";
        String axis "Z";
        String bounds "DEPTH_bnds";
        String positive "down";
        String long_name "Depth";
        String standard_name "depth";
        String QC_procedure "5";
        String uncertainty "0.1";
        String valid_min "0";
        String valid_max "5000";
    }
    DEPTH_bnds {
    }
    TIME {
        String units "DAYS since 1950-01-01 00:00:00";
        String axis "T";
        String time_origin "01-JAN-1950 00:00:00";
        String long_name "Time";
        String standard_name "time";
        String QC_procedure "1";
        String uncertainty "0.5";
        String valid_min "1223496115";
        String valid_max "1256054123";
    }
    PSAL {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "Hourly sea_water_salinity";
        String units " ";
        String standard_name "sea_water_salinity";
        String uncertainty "0.02";
        String valid_min "30";
        String valid_max "36";
        String cell_methods "time: mean";
        String history "From m1_ctd0001_20081008_original";
    }
    PSAL_QC {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "quality flag";
        String units " ";
        String conventions "OceanSITES reference table 2";
        String history "From m1_ctd0001_20081008_original";
    }
    TEMP {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "Hourly sea_water_temperature";
        String units "celsius";
        String standard_name "sea_water_temperature";
        String uncertainty "0.002";
        String valid_min "0";
        String valid_max "35";
        String cell_methods "time: mean";
        String history "From m1_ctd0001_20081008_original";
    }
    TEMP_QC {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "quality flag";
        String units " ";
        String conventions "OceanSITES reference table 2";
        String history "From m1_ctd0001_20081008_original";
    }
    TIME_QC {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "Quality flag for time axis, 1: Good data";
    }
    POSITION_QC {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "Quality flag for Latitude and Longitude axes, 1: Good data";
    }
    DEPTH_QC {
        Float32 missing_value -9.999999790e+33;
        Float32 _FillValue -9.999999790e+33;
        String long_name "Quality flag for depth axis, 1: Good data";
    }
    NC_GLOBAL {
        String title "Hourly Gridded MBARI Mooring M1 Sea Water Temperature and Salinity Observations";
        String summary "These data have been processed from the original instrument netCDF files which have individual irregular time axes.  The data in this file have been combined on a common hourly time axes and a common nominal depth axis.  Data values outside of reasonable ranges have been flagged as bad.";
        String keywords "CTD, Sea Water Temperature, Sea Water Salinity, Monterey Bay, OASIS, CIMT, MSE, MOOS";
        String standard_name_vocabulary "CF-1.1";
        String id "/mbari/ssdsdata/deployments/m1/200810/OS_M1_20081008_TS.nc";
        String naming_authority "OceanSITES";
        String keywords_vocabulary "GCMD Science Keywords";
        String cdm_data_type "Station";
        String creator_name "ssdsadmin";
        String project "SSDS";
        String data_type "OceanSITES time-series data";
        String format_version "1.1";
        String platform_code "M1";
        String date_created "2010-02-18T00:20:06Z";
        String date_update "2010-02-18T00:20:06Z";
        String site_code "MB";
        String institution "Monterey Bay Aquarium Research Institute";
        String data_mode "D";
        String quality_control_indicator "1";
        String quality_index "B";
        String netcdf_version "3.5";
        String area "Eastern Pacific Ocean";
        String references "http://www.mbari.org";
        String geospatial_lat_min "36.764";
        String geospatial_lat_max "36.764";
        String geospatial_lon_min "-122.046";
        String geospatial_lon_max "-122.046";
        String geospatial_vertical_min "1";
        String geospatial_vertical_max "300";
        String geospatial_vertical_positive "down";
        String time_coverage_start "2008-10-08T20:00:00Z";
        String time_coverage_end "2009-10-20T15:00:00Z";
        String source "Mooring Observation";
        String program "combineTS.pl -mooring M1 -ssdsServer new-ssds.mbari.org -deployments 200810 -ssdsDataServer new-ssds.mbari.org -inputDir /mbari/ssdsdata/deployments";
        String version "1.52";
        String user "ssdsadmin";
        String host "elvis.shore.mbari.org";
        String created "Wed Feb 17 16:20:06 2010";
        String history "FERRET V6.2   17-Feb-10";
        String Conventions "CF-1.0";
    }
    DODS_EXTRA {
        String Unlimited_Dimension "TIME";
    }
}
