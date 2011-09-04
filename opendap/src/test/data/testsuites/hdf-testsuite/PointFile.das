Attributes {
    HDF_GLOBAL {
        String HDFEOSVersion "HDFEOS_V2.0";
    }
    StructMetadata {
        SwathStructure {
        }
        GridStructure {
        }
        PointStructure {
            POINT_1 {
                String PointName "Simple Point";
                Level {
                    Level_0 {
                        String LevelName "Sensor";
                        PointField_1 {
                            String PointFieldName "Time";
                            String DataType DFNT_FLOAT64;
                            Int32 Order 1;
                        }
                        PointField_2 {
                            String PointFieldName "Concentration";
                            String DataType DFNT_FLOAT32;
                            Int32 Order 4;
                        }
                        PointField_3 {
                            String PointFieldName "Species";
                            String DataType DFNT_CHAR8;
                            Int32 Order 4;
                        }
                    }
                }
                LevelLink {
                }
            }
            Alias Simple_Point POINT_1;
            POINT_2 {
                String PointName "FixedBuoy Point";
                Level {
                }
                LevelLink {
                }
            }
            Alias FixedBuoy_Point POINT_2;
            POINT_3 {
                String PointName "FloatBuoy Point";
                Level {
                }
                LevelLink {
                }
            }
            Alias FloatBuoy_Point POINT_3;
        }
    }
}
