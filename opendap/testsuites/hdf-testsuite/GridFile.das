Attributes {
    MRGFLD_Temperature_dim_0 {
        String name "MRGDIM:PolarGrid_2";
    }
    MRGFLD_Temperature_dim_1 {
        String name "YDim:PolarGrid";
    }
    Pollution {
        Float64 _FillValue -7;
    }
    HDF_GLOBAL {
        String HDFEOSVersion "HDFEOS_V2.0";
    }
    Vegetation_dim_0 {
        String name "YDim:UTMGrid";
    }
    Vegetation_dim_1 {
        String name "XDim:UTMGrid";
    }
    Soil%20Dryness_dim_0 {
        String name "YDim:PolarGrid";
    }
    Soil%20Dryness_dim_1 {
        String name "XDim:PolarGrid";
    }
    Spectra_dim_2 {
        String name "XDim:PolarGrid";
    }
    MRGFLD_Temperature_dim_2 {
        String name "XDim:PolarGrid";
    }
    Pollution_dim_0 {
        String name "Time:UTMGrid";
    }
    Pollution_dim_1 {
        String name "YDim:UTMGrid";
    }
    Pollution_dim_2 {
        String name "XDim:UTMGrid";
    }
    StructMetadata {
        SwathStructure {
        }
        GridStructure {
            GRID_1 {
                String GridName "UTMGrid";
                Int32 XDim 120;
                Int32 YDim 200;
                Float64 UpperLeftPointMtrs 210584.500410, 3322395.954450;
                Float64 LowerRightMtrs 813931.109590, 2214162.532780;
                String Projection GCTP_UTM;
                Int32 ZoneCode 40;
                Int32 SphereCode 0;
                Dimension {
                    Dimension_1 {
                        String DimensionName "Time";
                        Int32 Size 10;
                    }
                }
                DataField {
                    DataField_1 {
                        String DataFieldName "Pollution";
                        String DataType DFNT_FLOAT32;
                        String DimList "Time", "YDim", "XDim";
                    }
                    DataField_2 {
                        String DataFieldName "Vegetation";
                        String DataType DFNT_FLOAT32;
                        String DimList "YDim", "XDim";
                    }
                    DataField_3 {
                        String DataFieldName "Extern";
                        String DataType DFNT_FLOAT32;
                        String DimList "YDim", "XDim";
                    }
                }
                MergedFields {
                }
            }
            Alias UTMGrid GRID_1;
            GRID_2 {
                String GridName "PolarGrid";
                Int32 XDim 100;
                Int32 YDim 100;
                String UpperLeftPointMtrs DEFAULT;
                String LowerRightMtrs DEFAULT;
                String Projection GCTP_PS;
                Float64 ProjParams 0, 0, 0, 0, 0, 90000000, 0, 0, 0, 0, 0, 0, 0;
                Int32 SphereCode 3;
                String GridOrigin HDFE_GD_LR;
                Dimension {
                    Dimension_1 {
                        String DimensionName "Bands";
                        Int32 Size 3;
                    }
                }
                DataField {
                    DataField_1 {
                        String DataFieldName "Temperature";
                        String DataType DFNT_FLOAT32;
                        String DimList "YDim", "XDim";
                    }
                    DataField_2 {
                        String DataFieldName "Pressure";
                        String DataType DFNT_FLOAT32;
                        String DimList "YDim", "XDim";
                    }
                    DataField_3 {
                        String DataFieldName "Soil Dryness";
                        String DataType DFNT_FLOAT32;
                        String DimList "YDim", "XDim";
                    }
                    DataField_4 {
                        String DataFieldName "Spectra";
                        String DataType DFNT_FLOAT64;
                        String DimList "Bands", "YDim", "XDim";
                    }
                }
                MergedFields {
                    MergedFields_1 {
                        String MergedFieldName "MRGFLD_Temperature";
                        String FieldList "Temperature", "Pressure";
                    }
                }
            }
            Alias PolarGrid GRID_2;
            GRID_3 {
                String GridName "GEOGrid";
                Int32 XDim 60;
                Int32 YDim 40;
                Float64 UpperLeftPointMtrs 0.000000, 30000000.000000;
                Float64 LowerRightMtrs 15000000.000000, 20000000.000000;
                String Projection GCTP_GEO;
                Dimension {
                }
                DataField {
                }
                MergedFields {
                }
            }
            Alias GEOGrid GRID_3;
        }
        PointStructure {
        }
    }
    Spectra_dim_0 {
        String name "Bands:PolarGrid";
    }
    Spectra_dim_1 {
        String name "YDim:PolarGrid";
    }
    MRGFLD_Temperature {
        Int32 Field%20Dims 1, 1;
        Int32 Field%20Offsets 0, 1;
    }
}
