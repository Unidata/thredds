Attributes {
    MRGFLD_Longitude {
        Int32 Field%20Dims 1, 1;
        Int32 Field%20Offsets 0, 1;
    }
    HDF_GLOBAL {
        String HDFEOSVersion "HDFEOS_V2.0";
    }
    Spectra_dim_0 {
        String name "Bands:Swath1";
    }
    Spectra_dim_1 {
        String name "Res2tr:Swath1";
    }
    Spectra_dim_2 {
        String name "Res2xtr:Swath1";
    }
    StructMetadata {
        SwathStructure {
            SWATH_1 {
                String SwathName "Swath1";
                Dimension {
                    Dimension_1 {
                        String DimensionName "GeoTrack";
                        Int32 Size 20;
                    }
                    Dimension_2 {
                        String DimensionName "GeoXtrack";
                        Int32 Size 10;
                    }
                    Dimension_3 {
                        String DimensionName "Res2tr";
                        Int32 Size 40;
                    }
                    Dimension_4 {
                        String DimensionName "Res2xtr";
                        Int32 Size 20;
                    }
                    Dimension_5 {
                        String DimensionName "Bands";
                        Int32 Size 15;
                    }
                    Dimension_6 {
                        String DimensionName "IndxTrack";
                        Int32 Size 12;
                    }
                    Dimension_7 {
                        String DimensionName "Unlim";
                        Int32 Size 0;
                    }
                }
                DimensionMap {
                    DimensionMap_1 {
                        String GeoDimension "GeoTrack";
                        String DataDimension "Res2tr";
                        Int32 Offset 0;
                        Int32 Increment 2;
                    }
                    DimensionMap_2 {
                        String GeoDimension "GeoXtrack";
                        String DataDimension "Res2xtr";
                        Int32 Offset 1;
                        Int32 Increment 2;
                    }
                }
                IndexDimensionMap {
                    IndexDimensionMap_1 {
                        String GeoDimension "IndxTrack";
                        String DataDimension "Res2tr";
                    }
                }
                GeoField {
                    GeoField_1 {
                        String GeoFieldName "Time";
                        String DataType DFNT_FLOAT64;
                        String DimList "GeoTrack";
                    }
                    GeoField_2 {
                        String GeoFieldName "Longitude";
                        String DataType DFNT_FLOAT32;
                        String DimList "GeoTrack", "GeoXtrack";
                    }
                    GeoField_3 {
                        String GeoFieldName "Latitude";
                        String DataType DFNT_FLOAT32;
                        String DimList "GeoTrack", "GeoXtrack";
                    }
                }
                DataField {
                    DataField_1 {
                        String DataFieldName "Density";
                        String DataType DFNT_FLOAT32;
                        String DimList "GeoTrack";
                    }
                    DataField_2 {
                        String DataFieldName "Temperature";
                        String DataType DFNT_FLOAT32;
                        String DimList "GeoTrack", "GeoXtrack";
                    }
                    DataField_3 {
                        String DataFieldName "Pressure";
                        String DataType DFNT_FLOAT64;
                        String DimList "Res2tr", "Res2xtr";
                    }
                    DataField_4 {
                        String DataFieldName "Spectra";
                        String DataType DFNT_FLOAT64;
                        String DimList "Bands", "Res2tr", "Res2xtr";
                    }
                    DataField_5 {
                        String DataFieldName "Count";
                        String DataType DFNT_INT16;
                        String DimList "Unlim";
                    }
                }
                MergedFields {
                    MergedFields_1 {
                        String MergedFieldName "MRGFLD_Longitude";
                        String FieldList "Longitude", "Latitude";
                    }
                }
            }
            Alias Swath1 SWATH_1;
        }
        GridStructure {
        }
        PointStructure {
        }
    }
    Temperature_dim_0 {
        String name "GeoTrack:Swath1";
    }
    Temperature_dim_1 {
        String name "GeoXtrack:Swath1";
    }
    Pressure_dim_0 {
        String name "Res2tr:Swath1";
    }
    Pressure_dim_1 {
        String name "Res2xtr:Swath1";
    }
    MRGFLD_Longitude_dim_0 {
        String name "MRGDIM:Swath1_2";
    }
    MRGFLD_Longitude_dim_1 {
        String name "GeoTrack:Swath1";
    }
    MRGFLD_Longitude_dim_2 {
        String name "GeoXtrack:Swath1";
    }
}
