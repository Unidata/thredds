/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.grib2.Grib2Parameter;

/**
 * Mrms local tables
 * @see "http://www.nssl.noaa.gov/projects/mrms/operational/tables.php"
 */

public class MrmsLocalTables extends LocalTables {
    private static MrmsLocalTables single;

    public static Grib2Customizer getCust(Grib2Table table) {
        if (single == null) single = new MrmsLocalTables(table);
        return single;
    }

    private MrmsLocalTables(Grib2Table grib2Table) {
        super(grib2Table);
        if (grib2Table.getPath() == null)
            grib2Table.setPath(this.getClass().getName());
        init();
    }

    @Override
    public String getTablePath(int discipline, int category, int number) {
        if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
        return this.getClass().getName();
    }

    /*
    taken from http://www.nssl.noaa.gov/projects/mrms/operational/tables.php
    */
    private void init() {
        add(209, 2, 0, "LightningDensityNLDN1min", "CG Lightning Density 1-min - NLDN", "flashes/km^2/min", -3, -1);
        add(209, 2, 1, "LightningDensityNLDN5min", "CG Lightning Density 5-min - NLDN", "flashes/km^2/min", -3, -1);
        add(209, 2, 2, "LightningDensityNLDN15min", "CG Lightning Density 15-min - NLDN", "flashes/km^2/min", -3, -1);
        add(209, 2, 3, "LightningDensityNLDN30min", "CG Lightning Density 30-min - NLDN", "flashes/km^2/min", -3, -1);
        add(209, 2, 4, "LightningProbabilityNext30min", "Lightning Probability 0-30 minutes - NLDN", "%", 0, 0);
        add(209, 3, 0, "MergedAzShear0to2kmAGL", "Azimuth Shear 0-2km AGL", "0.001/s", 0, 0);
        add(209, 3, 1, "MergedAzShear3to6kmAGL", "Azimuth Shear 3-6km AGL", "0.001/s", 0, 0);
        add(209, 3, 2, "RotationTrack30min", "Rotation Track 0-2km AGL 30-min", "0.001/s", 0, 0);
        add(209, 3, 3, "RotationTrack60min", "Rotation Track 0-2km AGL 60-min", "0.001/s", 0, 0);
        add(209, 3, 4, "RotationTrack120min", "Rotation Track 0-2km AGL 120-min", "0.001/s", 0, 0);
        add(209, 3, 5, "RotationTrack240min", "Rotation Track 0-2km AGL 240-min", "0.001/s", 0, 0);
        add(209, 3, 6, "RotationTrack360min", "Rotation Track 0-2km AGL 360-min", "0.001/s", 0, 0);
        add(209, 3, 7, "RotationTrack1440min", "Rotation Track 0-2km AGL 1440-min", "0.001/s", 0, 0);
        add(209, 3, 14, "RotationTrackML30min", "Rotation Track 0-3km AGL 30-min", "0.001/s", 0, 0);
        add(209, 3, 15, "RotationTrackML60min", "Rotation Track 0-3km AGL 60-min", "0.001/s", 0, 0);
        add(209, 3, 16, "RotationTrackML120min", "Rotation Track 0-3km AGL 120-min", "0.001/s", 0, 0);
        add(209, 3, 17, "RotationTrackML240min", "Rotation Track 0-3km AGL 240-min", "0.001/s", 0, 0);
        add(209, 3, 18, "RotationTrackML360min", "Rotation Track 0-3km AGL 360-min", "0.001/s", 0, 0);
        add(209, 3, 19, "RotationTrackML1440min", "Rotation Track 0-3km AGL 1440-min", "0.001/s", 0, 0);
        add(209, 3, 26, "SHI", "Severe Hail Index", "index", -3, -1);
        add(209, 3, 27, "POSH", "Prob of Severe Hail", "%", -3, -1);
        add(209, 3, 28, "MESH", "Maximum Estimated Size of Hail (MESH)", "mm", -3, -1);
        add(209, 3, 29, "MESHMax30min", "MESH Hail Swath 30-min", "mm", -3, -1);
        add(209, 3, 30, "MESHMax60min", "MESH Hail Swath 60-min", "mm", -3, -1);
        add(209, 3, 31, "MESHMax120min", "MESH Hail Swath 120-min", "mm", -3, -1);
        add(209, 3, 32, "MESHMax240min", "MESH Hail Swath 240-min", "mm", -3, -1);
        add(209, 3, 33, "MESHMax360min", "MESH Hail Swath 360-min", "mm", -3, -1);
        add(209, 3, 34, "MESHMax1440min", "MESH Hail Swath 1440-min", "mm", -3, -1);
        add(209, 3, 41, "VIL", "Vertically Integrated Liquid", "kg/m^2", -3, -1);
        add(209, 3, 42, "VILDensity", "Vertically Integrated Liquid Density", "g/m^3", -3, -1);
        add(209, 3, 43, "VII", "Vertically Integrated Ice", "kg/m^2", -3, -1);
        add(209, 3, 44, "EchoTop18", "Echo Top - 18 dBZ MSL", "km", -3, -1);
        add(209, 3, 45, "EchoTop30", "Echo Top - 30 dBZ MSL", "km", -3, -1);
        add(209, 3, 46, "EchoTop50", "Echo Top - 50 dBZ MSL", "km", -3, -1);
        add(209, 3, 47, "EchoTop60", "Echo Top - 60 dBZ MSL", "km", -3, -1);
        add(209, 3, 48, "H50AboveM20C", "Thickness [50 dBZ top - (-20C)]", "km", -999, -99);
        add(209, 3, 49, "H50Above0C", "Thickness [50 dBZ top - 0C]", "km", -999, -99);
        add(209, 3, 50, "H60AboveM20C", "Thickness [60 dBZ top - (-20C)]", "km", -999, -99);
        add(209, 3, 51, "H60Above0C", "Thickness [60 dBZ top - 0C]", "km", -999, -99);
        add(209, 3, 52, "Reflectivity0C", "Isothermal Reflectivity at 0C", "dBZ", -999, -99);
        add(209, 3, 53, "ReflectivityM5C", "Isothermal Reflectivity at -5C", "dBZ", -999, -99);
        add(209, 3, 54, "ReflectivityM10C", "Isothermal Reflectivity at -10C", "dBZ", -999, -99);
        add(209, 3, 55, "ReflectivityM15C", "Isothermal Reflectivity at -15C", "dBZ", -999, -99);
        add(209, 3, 56, "ReflectivityM20C", "Isothermal Reflectivity at -20C", "dBZ", -999, -99);
        add(209, 3, 57, "ReflectivityAtLowestAltitude", "ReflectivityAtLowestAltitude", "dBZ", -999, -99);
        add(209, 3, 58, "MergedReflectivityAtLowestAltitude", "Non Quality Controlled Reflectivity At Lowest Altitude", "dBZ", -999, -99);
        add(209, 4, 0, "IRband4", "Infrared (E/W blend)", "K", -999, -99);
        add(209, 4, 1, "Visible", "Visible (E/W blend)", "dimensionless", -3, -1);
        add(209, 4, 2, "WaterVapor", "Water Vapor (E/W blend)", "K", -999, -99);
        add(209, 4, 3, "CloudCover", "Cloud Cover", "K", -999, -99);
        add(209, 6, 0, "PrecipFlag", "Surface Precipitation Type (Convective, Stratiform, Tropical, Hail, Snow)", "dimensionless", -3, -1);
        add(209, 6, 1, "PrecipRate", "Radar Precipitation Rate", "mm/hr", -3, -1);
        add(209, 6, 2, "RadarOnlyQPE01H", "Radar precipitation accumulation 1-hour", "mm", -3, -1);
        add(209, 6, 3, "RadarOnlyQPE03H", "Radar precipitation accumulation 3-hour", "mm", -3, -1);
        add(209, 6, 4, "RadarOnlyQPE06H", "Radar precipitation accumulation 6-hour", "mm", -3, -1);
        add(209, 6, 5, "RadarOnlyQPE12H", "Radar precipitation accumulation 12-hour", "mm", -3, -1);
        add(209, 6, 6, "RadarOnlyQPE24H", "Radar precipitation accumulation 24-hour", "mm", -3, -1);
        add(209, 6, 7, "RadarOnlyQPE48H", "Radar precipitation accumulation 48-hour", "mm", -3, -1);
        add(209, 6, 8, "RadarOnlyQPE72H", "Radar precipitation accumulation 72-hour", "mm", -3, -1);
        add(209, 6, 9, "GaugeCorrQPE01H", "Local gauge bias corrected radar precipitation accumulation 1-hour", "mm", -3, -1);
        add(209, 6, 10, "GaugeCorrQPE03H", "Local gauge bias corrected radar precipitation accumulation 3-hour", "mm", -3, -1);
        add(209, 6, 11, "GaugeCorrQPE06H", "Local gauge bias corrected radar precipitation accumulation 6-hour", "mm", -3, -1);
        add(209, 6, 12, "GaugeCorrQPE12H", "Local gauge bias corrected radar precipitation accumulation 12-hour", "mm", -3, -1);
        add(209, 6, 13, "GaugeCorrQPE24H", "Local gauge bias corrected radar precipitation accumulation 24-hour", "mm", -3, -1);
        add(209, 6, 14, "GaugeCorrQPE48H", "Local gauge bias corrected radar precipitation accumulation 48-hour", "mm", -3, -1);
        add(209, 6, 15, "GaugeCorrQPE72H", "Local gauge bias corrected radar precipitation accumulation 72-hour", "mm", -3, -1);
        add(209, 6, 16, "GaugeOnlyQPE01H", "Gauge only precipitation accumulation 1-hour", "mm", -3, -1);
        add(209, 6, 17, "GaugeOnlyQPE03H", "Gauge only precipitation accumulation 3-hour", "mm", -3, -1);
        add(209, 6, 18, "GaugeOnlyQPE06H", "Gauge only precipitation accumulation 6-hour", "mm", -3, -1);
        add(209, 6, 19, "GaugeOnlyQPE12H", "Gauge only precipitation accumulation 12-hour", "mm", -3, -1);
        add(209, 6, 20, "GaugeOnlyQPE24H", "Gauge only precipitation accumulation 24-hour", "mm", -3, -1);
        add(209, 6, 21, "GaugeOnlyQPE48H", "Gauge only precipitation accumulation 48-hour", "mm", -3, -1);
        add(209, 6, 22, "GaugeOnlyQPE72H", "Gauge only precipitation accumulation 72-hour", "mm", -3, -1);
        add(209, 6, 23, "MountainMapperQPE01H", "Mountain Mapper precipitation accumulation 1-hour", "mm", -3, -1);
        add(209, 6, 24, "MountainMapperQPE03H", "Mountain Mapper precipitation accumulation 3-hour", "mm", -3, -1);
        add(209, 6, 25, "MountainMapperQPE06H", "Mountain Mapper precipitation accumulation 6-hour", "mm", -3, -1);
        add(209, 6, 26, "MountainMapperQPE12H", "Mountain Mapper precipitation accumulation 12-hour", "mm", -3, -1);
        add(209, 6, 27, "MountainMapperQPE24H", "Mountain Mapper precipitation accumulation 24-hour", "mm", -3, -1);
        add(209, 6, 28, "MountainMapperQPE48H", "Mountain Mapper precipitation accumulation 48-hour", "mm", -3, -1);
        add(209, 6, 29, "MountainMapperQPE72H", "Mountain Mapper precipitation accumulation 72-hour", "mm", -3, -1);
        add(209, 7, 0, "ModelSurfaceTemp", "Model Surface temperature [RAP 13km]", "C", -999, -99);
        add(209, 7, 1, "ModelWetBulbTemp", "Model Surface wet bulb temperature [RAP 13km]", "C", -999, -99);
        add(209, 7, 2, "WarmRainProbability", "Probability of warm rain [RAP 13km derived]", "%", -3, -1);
        add(209, 7, 3, "ModelHeight0C", "Model Freezing Level Height [RAP 13km] MSL", "m", -3, -1);
        add(209, 7, 4, "BrightBandTopHeight", "Brightband Top Radar [RAP 13km derived] AGL", "m", -3, -1);
        add(209, 7, 5, "BrightBandBottomHeight", "Brightband Bottom Radar [RAP 13km derived] AGL", "m", -3, -1);
        add(209, 8, 0, "RadarQualityIndex", "Radar Quality Index", "dimensionless", -3, -1);
        add(209, 8, 1, "GaugeInflIndex01H", "Gauge Influence Index for 1-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 2, "GaugeInflIndex03H", "Gauge Influence Index for 3-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 3, "GaugeInflIndex06H", "Gauge Influence Index for 6-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 4, "GaugeInflIndex12H", "Gauge Influence Index for 12-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 5, "GaugeInflIndex24H", "Gauge Influence Index for 24-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 6, "GaugeInflIndex48H", "Gauge Influence Index for 48-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 7, "GaugeInflIndex72H", "Gauge Influence Index for 72-hour QPE", "dimensionless", -3, -1);
        add(209, 8, 8, "SeamlessHSR", "Seamless Hybrid Scan Reflectivity with VPR correction", "dBZ", -999, -99);
        add(209, 8, 9, "SeamlessHSRHeight", "Height of Seamless Hybrid Scan Reflectivity AGL", "km", -3, -1);
        add(209, 9, 0, "ConusMergedReflectivityQC", "WSR-88D 3D Reflectivity Mosaic - 33 CAPPIS (500-19000m)", "dBZ", -999, -99);
        add(209, 9, 1, "ConusPlusMergedReflectivityQC", "All Radar 3D Reflectivity Mosaic - 33 CAPPIS (500-19000m)", "dBZ", -999, -99);
        add(209, 10, 0, "MergedReflectivityQCComposite", "Composite Reflectivity Mosaic (optimal method)", "dBZ", -999, -99);
        add(209, 10, 1, "HeightCompositeReflectivity", "Height of Composite Reflectivity Mosaic (optimal method) MSL", "m", -3, -1);
        add(209, 10, 2, "LowLevelCompositeReflectivity", "Low-Level Composite Reflectivity Mosaic (0-4km)", "dBZ", -999, -99);
        add(209, 10, 3, "HeightLowLevelCompositeReflectivity", "Height of Low-Level Composite Reflectivity Mosaic (0-4km) MSL", "m", -3, -1);
        add(209, 10, 4, "LayerCompositeReflectivity_Low", "Layer Composite Reflectivity Mosaic 0-24kft (low altitude)", "dBZ", -999, -99);
        add(209, 10, 5, "LayerCompositeReflectivity_High", "Layer Composite Reflectivity Mosaic 24-60 kft (highest altitude)", "dBZ", -999, -99);
        add(209, 10, 6, "LayerCompositeReflectivity_Super", "Layer Composite Reflectivity Mosaic 33-60 kft (super high altitude)", "dBZ", -999, -99);
        add(209, 10, 7, "ReflectivityCompositeHourlyMax", "Composite Reflectivity Hourly Maximum", "dBZ", -999, -99);
        add(209, 10, 8, "ReflectivityMaxAboveM10C", "Maximum Reflectivity at -10 deg C height and above", "dBZ", -999, -99);
        add(209, 11, 0, "MergedBaseReflectivityQC", "Mosaic Base Reflectivity (optimal method)", "dBZ", -999, -99);
        add(209, 11, 1, "MergedReflectivityComposite", "UnQc'd Composite Reflectivity Mosaic (max ref)", "dBZ", -999, -99);
        add(209, 11, 2, "MergedReflectivityQComposite", "Composite Reflectivity Mosaic (max ref)", "dBZ", -999, -99);
    }

    private void add(int discipline, int category, int number, String name, String desc, String unit, float fill, float missing) {
        local.put(makeParamId(discipline, category, number), new Grib2Parameter(discipline, category, number, name, unit, null, desc, fill, missing));
    }

}