Attributes {
    Absolute_vorticity {
        String long_name "Absolute_vorticity @ pressure";
        String units s-1;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name Absolute_vorticity;
        Int32 GRIB_param_id 2, 0, 2, 10;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Best_Lifted_Index {
        String long_name "Best_Lifted_Index @ pressure_difference_layer";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Best_lifted_index;
        Int32 GRIB_param_id 2, 0, 7, 193;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Categorical_Freezing_Rain {
        String long_name "Categorical_Freezing_Rain @ surface";
        String units ;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Categorical_Freezing_Rain;
        Int32 GRIB_param_id 2, 0, 1, 193;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Categorical_Ice_Pellets {
        String long_name "Categorical_Ice_Pellets @ surface";
        String units ;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Categorical_Ice_Pellets;
        Int32 GRIB_param_id 2, 0, 1, 194;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Categorical_Rain {
        String long_name "Categorical_Rain @ surface";
        String units ;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Categorical_Rain;
        Int32 GRIB_param_id 2, 0, 1, 192;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Categorical_Snow {
        String long_name "Categorical_Snow @ surface";
        String units ;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Categorical_Snow;
        Int32 GRIB_param_id 2, 0, 1, 195;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Convective_available_potential_energy {
        String long_name "Convective_available_potential_energy @ pressure_difference_layer";
        String units "J kg-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Convective_available_potential_energy;
        Int32 GRIB_param_id 2, 0, 7, 6;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Convective_available_potential_energy_surface {
        String long_name "Convective_available_potential_energy @ surface";
        String units "J kg-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Convective_available_potential_energy;
        Int32 GRIB_param_id 2, 0, 7, 6;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Convective_inhibition {
        String long_name "Convective_inhibition @ pressure_difference_layer";
        String units "J kg-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Convective_inhibition;
        Int32 GRIB_param_id 2, 0, 7, 7;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Convective_inhibition_surface {
        String long_name "Convective_inhibition @ surface";
        String units "J kg-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Convective_inhibition;
        Int32 GRIB_param_id 2, 0, 7, 7;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Convective_precipitation {
        String long_name "Convective_precipitation @ surface";
        String units "kg m-2";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Convective_precipitation;
        Int32 GRIB_param_id 2, 0, 1, 10;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Derived_radar_reflectivity {
        String long_name "Derived_radar_reflectivity @ height_above_ground";
        String units dB;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Forecast_Radar_Imagery;
        String GRIB_param_name Derived_radar_reflectivity;
        Int32 GRIB_param_id 2, 0, 16, 195;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Derived_radar_reflectivity_hybrid {
        String long_name "Derived_radar_reflectivity @ hybrid";
        String units dB;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Forecast_Radar_Imagery;
        String GRIB_param_name Derived_radar_reflectivity;
        Int32 GRIB_param_id 2, 0, 16, 195;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 105;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Dew_point_temperature {
        String long_name "Dew_point_temperature @ height_above_ground";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Dew_point_temperature;
        Int32 GRIB_param_id 2, 0, 0, 6;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Geopotential_height {
        String long_name "Geopotential_height @ pressure";
        String units gpm;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Geopotential_height;
        Int32 GRIB_param_id 2, 0, 3, 5;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Geopotential_height_zeroDegC_isotherm {
        String long_name "Geopotential_height @ zeroDegC_isotherm";
        String units gpm;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Geopotential_height;
        Int32 GRIB_param_id 2, 0, 3, 5;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 4;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Maximum_temperature {
        String long_name "Maximum_temperature @ height_above_ground";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Maximum_temperature;
        Int32 GRIB_param_id 2, 0, 0, 4;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    MaximumComposite_radar_reflectivity {
        String long_name "MaximumComposite_radar_reflectivity @ entire_atmosphere";
        String units dB;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Forecast_Radar_Imagery;
        String GRIB_param_name Maximum_Composite_radar_reflectivity;
        Int32 GRIB_param_id 2, 0, 16, 196;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 200;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Minimum_temperature {
        String long_name "Minimum_temperature @ height_above_ground";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Minimum_temperature;
        Int32 GRIB_param_id 2, 0, 0, 5;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    MSLP_Eta_Reduction {
        String long_name "MSLP_Eta_Reduction @ msl";
        String units Pa;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name MSLP_Eta_Reduction;
        Int32 GRIB_param_id 2, 0, 3, 192;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 101;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Parcel_lifted_index_to_500_hPa {
        String long_name "Parcel_lifted_index_to_500_hPa @ pressure_difference_layer";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Parcel_lifted_index;
        Int32 GRIB_param_id 2, 0, 7, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Precipitable_water {
        String long_name "Precipitable_water @ entire_atmosphere";
        String units "kg m-2";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Precipitable_water;
        Int32 GRIB_param_id 2, 0, 1, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 200;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Pressure_reduced_to_MSL {
        String long_name "Pressure_reduced_to_MSL @ msl";
        String units Pa;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Pressure_reduced_to_MSL;
        Int32 GRIB_param_id 2, 0, 3, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 101;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Pressure {
        String long_name "Pressure @ maximum_wind";
        String units Pa;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Pressure;
        Int32 GRIB_param_id 2, 0, 3, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 6;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Pressure_surface {
        String long_name "Pressure @ surface";
        String units Pa;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Pressure;
        Int32 GRIB_param_id 2, 0, 3, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Pressure_tropopause {
        String long_name "Pressure @ tropopause";
        String units Pa;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Mass;
        String GRIB_param_name Pressure;
        Int32 GRIB_param_id 2, 0, 3, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 7;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Probability_of_0p01_inch_Precipitation {
        String long_name "Probability_of_0p01_inch_Precipitation @ surface";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Hydrological_products;
        String GRIB_param_category Hydrology_probabilities;
        String GRIB_param_name Probability_of_p01_inch_precipitation;
        Int32 GRIB_param_id 2, 1, 1, 2;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Probability_of_freezing_Precipitation {
        String long_name "Probability_of_freezing_Precipitation @ surface";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Hydrological_products;
        String GRIB_param_category Hydrology_probabilities;
        String GRIB_param_name Probability_of_freezing_precipitation;
        Int32 GRIB_param_id 2, 1, 1, 192;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Probability_of_frozen_Precipitation {
        String long_name "Probability_of_frozen_Precipitation @ surface";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Hydrological_products;
        String GRIB_param_category Hydrology_probabilities;
        String GRIB_param_name Probability_of_frozen_precipitation;
        Int32 GRIB_param_id 2, 1, 1, 193;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Relative_humidity {
        String long_name "Relative_humidity @ pressure";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Relative_humidity;
        Int32 GRIB_param_id 2, 0, 1, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Relative_humidity_pressure_difference_layer {
        String long_name "Relative_humidity @ pressure_difference_layer";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Relative_humidity;
        Int32 GRIB_param_id 2, 0, 1, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Relative_humidity_height_above_ground {
        String long_name "Relative_humidity @ height_above_ground";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Relative_humidity;
        Int32 GRIB_param_id 2, 0, 1, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Relative_humidity_sigma_layer {
        String long_name "Relative_humidity @ sigma_layer";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Relative_humidity;
        Int32 GRIB_param_id 2, 0, 1, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 104;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Relative_humidity_zeroDegC_isotherm {
        String long_name "Relative_humidity @ zeroDegC_isotherm";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Relative_humidity;
        Int32 GRIB_param_id 2, 0, 1, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 4;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Snow_depth {
        String long_name "Snow_depth @ surface";
        String units m;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Snow_depth;
        Int32 GRIB_param_id 2, 0, 1, 11;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Storm_relative_helicity {
        String long_name "Storm_relative_helicity @ height_above_ground_layer";
        String units "J kg-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Storm_relative_helicity;
        Int32 GRIB_param_id 2, 0, 7, 8;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Surface_Lifted_Index {
        String long_name "Surface_Lifted_Index @ pressure_layer";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Thermodynamic_Stability_indices;
        String GRIB_param_name Surface_Lifted_Index;
        Int32 GRIB_param_id 2, 0, 7, 192;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Temperature {
        String long_name "Temperature @ pressure";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Temperature;
        Int32 GRIB_param_id 2, 0, 0, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Temperature_pressure_difference_layer {
        String long_name "Temperature @ pressure_difference_layer";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Temperature;
        Int32 GRIB_param_id 2, 0, 0, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Temperature_height_above_ground {
        String long_name "Temperature @ height_above_ground";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Temperature;
        Int32 GRIB_param_id 2, 0, 0, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Temperature_tropopause {
        String long_name "Temperature @ tropopause";
        String units K;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Temperature;
        String GRIB_param_name Temperature;
        Int32 GRIB_param_id 2, 0, 0, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 7;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Thunderstorm_probability {
        String long_name "Thunderstorm_probability @ surface";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Physical_atmospheric_properties;
        String GRIB_param_name Thunderstorm_probability;
        Int32 GRIB_param_id 2, 0, 19, 2;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Total_cloud_cover {
        String long_name "Total_cloud_cover @ entire_atmosphere";
        String units percent;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Cloud;
        String GRIB_param_name Total_cloud_cover;
        Int32 GRIB_param_id 2, 0, 6, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 200;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Total_precipitation {
        String long_name "Total_precipitation @ surface";
        String units "kg m-2";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Total_precipitation;
        Int32 GRIB_param_id 2, 0, 1, 8;
        String GRIB_product_definition_type "Average, accumulation, extreme values or other statistically processed value at a horizontal level";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    U-component_of_wind {
        String long_name "U-component_of_wind @ pressure";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name U-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 2;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    U-component_of_wind_pressure_difference_layer {
        String long_name "U-component_of_wind @ pressure_difference_layer";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name U-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 2;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    U-component_of_wind_height_above_ground {
        String long_name "U-component_of_wind @ height_above_ground";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name U-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 2;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    U-component_of_wind_maximum_wind {
        String long_name "U-component_of_wind @ maximum_wind";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name U-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 2;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 6;
        String GRIB_VectorComponentFlag gridRelative;
    }
    U-component_of_wind_tropopause {
        String long_name "U-component_of_wind @ tropopause";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name U-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 2;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 7;
        String GRIB_VectorComponentFlag gridRelative;
    }
    V-component_of_wind {
        String long_name "V-component_of_wind @ pressure";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name V-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    V-component_of_wind_pressure_difference_layer {
        String long_name "V-component_of_wind @ pressure_difference_layer";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name V-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 108;
        String GRIB_VectorComponentFlag gridRelative;
    }
    V-component_of_wind_height_above_ground {
        String long_name "V-component_of_wind @ height_above_ground";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name V-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    V-component_of_wind_maximum_wind {
        String long_name "V-component_of_wind @ maximum_wind";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name V-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 6;
        String GRIB_VectorComponentFlag gridRelative;
    }
    V-component_of_wind_tropopause {
        String long_name "V-component_of_wind @ tropopause";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name V-component_of_wind;
        Int32 GRIB_param_id 2, 0, 2, 3;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 7;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Vertical_velocity_pressure {
        String long_name "Vertical_velocity_pressure @ pressure";
        String units "Pa s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name Vertical_velocity;
        Int32 GRIB_param_id 2, 0, 2, 8;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 100;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Visibility {
        String long_name "Visibility @ surface";
        String units m;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Physical_atmospheric_properties;
        String GRIB_param_name Visibility;
        Int32 GRIB_param_id 2, 0, 19, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Water_equivalent_of_accumulated_snow_depth {
        String long_name "Water_equivalent_of_accumulated_snow_depth @ surface";
        String units "kg m-2";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Moisture;
        String GRIB_param_name Water_equivalent_of_accumulated_snow_depth;
        Int32 GRIB_param_id 2, 0, 1, 13;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 1;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Wind_direction_from_which_blowing {
        String long_name "Wind_direction_from_which_blowing @ height_above_ground";
        String units degrees;
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name Wind_direction;
        Int32 GRIB_param_id 2, 0, 2, 0;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    Wind_speed {
        String long_name "Wind_speed @ height_above_ground";
        String units "m s-1";
        Float32 missing_value NaN;
        String grid_mapping Lambert_Conformal;
        String GRIB_param_discipline Meteorological_products;
        String GRIB_param_category Momentum;
        String GRIB_param_name Wind_speed;
        Int32 GRIB_param_id 2, 0, 2, 1;
        String GRIB_product_definition_type "Analysis/forecast at horizontal level/layer";
        Int32 GRIB_level_type 103;
        String GRIB_VectorComponentFlag gridRelative;
    }
    time {
        String long_name "forecast time";
        String units "hours since 2010-06-28T12:00:00Z";
        String GRIB_orgReferenceTime 2010-06-28T12:00:00Z;
        String GRIB2_significanceOfRTName "Start of forecast";
        String _CoordinateAxisType Time;
    }
    time1 {
        String long_name "forecast time";
        String units "hours since 2010-06-28T12:00:00Z";
        String GRIB_orgReferenceTime 2010-06-28T12:00:00Z;
        String GRIB2_significanceOfRTName "Start of forecast";
        String _CoordinateAxisType Time;
    }
    Lambert_Conformal {
        String grid_mapping_name lambert_conformal_conic;
        Float64 standard_parallel 25.0;
        Float64 longitude_of_central_meridian 265.0;
        Float64 latitude_of_projection_origin 25.0;
        String earth_shape "Earth spherical with radius of 6,371,229.0 m";
        Float64 earth_radius 6371229.0;
        Float64 GRIB_param_Dx 12191.0;
        Float64 GRIB_param_Dy 12191.0;
        Int32 GRIB_param_GDSkey -2016599651;
        Float64 GRIB_param_La1 12.19;
        Float64 GRIB_param_LaD 25.0;
        Float64 GRIB_param_Latin1 25.0;
        Float64 GRIB_param_Latin2 25.0;
        Float64 GRIB_param_Lo1 226.54099;
        Float64 GRIB_param_LoV 265.0;
        String GRIB_param_NpProj true;
        Int32 GRIB_param_Nx 614;
        Int32 GRIB_param_Ny 428;
        Int32 GRIB_param_ProjFlag 0;
        String GRIB_param_Quasi false;
        Int32 GRIB_param_ResCompFlag 8;
        Float64 GRIB_param_SpLat 0.0;
        Float64 GRIB_param_SpLon 0.0;
        String GRIB_param_VectorComponentFlag gridRelative;
        String GRIB_param_Winds Relative;
        String GRIB_param_grid_name Lambert_Conformal;
        Float64 GRIB_param_grid_radius_spherical_earth 6371229.0;
        String GRIB_param_grid_shape "Earth spherical with radius of 6,371,229.0 m";
        Int32 GRIB_param_grid_shape_code 6;
        Int32 GRIB_param_grid_type 30;
        String GRIB_param_grid_units m;
        Int32 GRIB_param_scanning_mode 64;
        DODS {
            Int32 strlen 0;
        }
    }
    y {
        String units km;
        String long_name "y coordinate of projection";
        String standard_name projection_y_coordinate;
        String grid_spacing "12.191 km";
        String _CoordinateAxisType GeoY;
    }
    x {
        String units km;
        String long_name "x coordinate of projection";
        String standard_name projection_x_coordinate;
        String grid_spacing "12.191 km";
        String _CoordinateAxisType GeoX;
    }
    height_above_ground {
        String long_name "Specified height level above ground";
        String units m;
        String positive up;
        String GRIB_level_type 103;
        String _CoordinateAxisType Height;
    }
    height_above_ground1 {
        String long_name "Specified height level above ground";
        String units m;
        String positive up;
        String GRIB_level_type 103;
        String _CoordinateAxisType Height;
    }
    height_above_ground2 {
        String long_name "Specified height level above ground";
        String units m;
        String positive up;
        String GRIB_level_type 103;
        String _CoordinateAxisType Height;
    }
    height_above_ground_layer {
        String long_name "Layer between Specified height level above ground";
        String units m;
        String positive up;
        String GRIB_level_type 103;
        String _CoordinateAxisType Height;
        String bounds height_above_ground_layer_bounds;
        String _CoordinateZisLayer true;
    }
    height_above_ground_layer_bounds {
        String long_name "bounds for height_above_ground_layer";
        String units m;
    }
    hybrid {
        String long_name "Hybrid level";
        String units ;
        String positive up;
        String GRIB_level_type 105;
        String _CoordinateAxisType GeoZ;
    }
    pressure {
        String long_name "Isobaric surface";
        String units Pa;
        String positive down;
        String GRIB_level_type 100;
        String _CoordinateAxisType Pressure;
    }
    pressure1 {
        String long_name "Isobaric surface";
        String units Pa;
        String positive down;
        String GRIB_level_type 100;
        String _CoordinateAxisType Pressure;
    }
    pressure2 {
        String long_name "Isobaric surface";
        String units Pa;
        String positive down;
        String GRIB_level_type 100;
        String _CoordinateAxisType Pressure;
    }
    pressure_difference_layer {
        String long_name "Layer between Level at specified pressure difference from ground to level";
        String units Pa;
        String positive up;
        String GRIB_level_type 108;
        String _CoordinateAxisType Pressure;
        String bounds pressure_difference_layer_bounds;
        String _CoordinateZisLayer true;
    }
    pressure_difference_layer_bounds {
        String long_name "bounds for pressure_difference_layer";
        String units Pa;
    }
    pressure_difference_layer1 {
        String long_name "Layer between Level at specified pressure difference from ground to level";
        String units Pa;
        String positive up;
        String GRIB_level_type 108;
        String _CoordinateAxisType Pressure;
        String bounds pressure_difference_layer1_bounds;
        String _CoordinateZisLayer true;
    }
    pressure_difference_layer1_bounds {
        String long_name "bounds for pressure_difference_layer1";
        String units Pa;
    }
    pressure_difference_layer2 {
        String long_name "Layer between Level at specified pressure difference from ground to level";
        String units Pa;
        String positive up;
        String GRIB_level_type 108;
        String _CoordinateAxisType Pressure;
        String bounds pressure_difference_layer2_bounds;
        String _CoordinateZisLayer true;
    }
    pressure_difference_layer2_bounds {
        String long_name "bounds for pressure_difference_layer2";
        String units Pa;
    }
    pressure_layer {
        String long_name "Layer between Isobaric surface";
        String units Pa;
        String positive down;
        String GRIB_level_type 100;
        String _CoordinateAxisType Pressure;
        String bounds pressure_layer_bounds;
        String _CoordinateZisLayer true;
    }
    pressure_layer_bounds {
        String long_name "bounds for pressure_layer";
        String units Pa;
    }
    NC_GLOBAL {
        String Conventions CF-1.0;
        String Originating_center "US National Weather Service (NCEP) subcenter = 0";
        String Generating_Process_or_Model Forecast;
        String Product_Status "Operational products";
        String Product_Type "Forecast products";
        String cdm_data_type GRID;
        String creator_name "US National Weather Service (NCEP) subcenter = 0";
        String file_format GRIB-2;
        String location /data/ldm/pub/native/grid/NCEP/NAM/CONUS_12km/NAM_CONUS_12km_20100628_1200.grib2;
        String history "Direct read of GRIB-2 into NetCDF-Java 4.0 API";
        String _CoordinateModelRunDate 2010-06-28T12:00:00Z;
    }
}

DEBUG: testdata: NAM_CONUS_12km_20100628_1200.grib2.DAS:
null
java.lang.NullPointerException
	at java.io.StringReader.<init>(StringReader.java:33)
	at opendap.dap.test.Diff.doDiff(Diff.java:266)
	at opendap.dap.test.Diff.doDiff(Diff.java:271)
	at opendap.dap.test.TestDConnect2.testpart(TestDConnect2.java:135)
	at opendap.dap.test.TestDConnect2.test1(TestDConnect2.java:82)
	at opendap.dap.test.TestDConnect2.test(TestDConnect2.java:163)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at junit.framework.TestCase.runTest(TestCase.java:168)
	at junit.framework.TestCase.runBare(TestCase.java:134)
	at junit.framework.TestResult$1.protect(TestResult.java:110)
	at junit.framework.TestResult.runProtected(TestResult.java:128)
	at junit.framework.TestResult.run(TestResult.java:113)
	at junit.framework.TestCase.run(TestCase.java:124)
	at junit.framework.TestSuite.runTest(TestSuite.java:232)
	at junit.framework.TestSuite.run(TestSuite.java:227)
	at junit.textui.TestRunner.doRun(TestRunner.java:116)
	at com.intellij.junit3.JUnit3IdeaTestRunner.doRun(JUnit3IdeaTestRunner.java:108)
	at junit.textui.TestRunner.doRun(TestRunner.java:109)
	at com.intellij.junit3.JUnit3IdeaTestRunner.startRunnerWithArgs(JUnit3IdeaTestRunner.java:42)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:192)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:110)
DEBUG: result: NAM_CONUS_12km_20100628_1200.grib2.DDS:
Dataset {
    Grid {
     ARRAY:
        Float32 Wind_speed[time = 1][height_above_ground = 1][y = 1][x = 1];
     MAPS:
        Int32 time[time = 1];
        Float64 height_above_ground[height_above_ground = 1];
        Float64 y[y = 1];
        Float64 x[x = 1];
    } Wind_speed;
} fmrc/NCEP/NAM/CONUS_12km/files/NAM_CONUS_12km_20100628_1200.grib2;
