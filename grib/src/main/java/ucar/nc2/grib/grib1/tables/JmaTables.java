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
package ucar.nc2.grib.grib1.tables;

import ucar.nc2.grib.GribLevelType;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribStatType;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Local tables for JMA (center 34)
 *
 * @author caron
 * @see "http://rda.ucar.edu/datasets/ds628.0/docs/JRA-55.grib_20130319.xlsx"
 * @since 1/27/2015
 */
public class JmaTables extends Grib1Customizer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JmaTables.class);

  private static Map<Integer, GribLevelType> levelTypesMap;  // shared by all instances

  JmaTables(Grib1ParamTables tables) {
    super(34, tables);
  }

  /////////////////// local time types
  @Override
  public Grib1ParamTime getParamTime(Grib1SectionProductDefinition pds) {
    int p1 = pds.getTimeValue1();  // octet 19
    int p2 = pds.getTimeValue2();  // octet 20
    int timeRangeIndicator = pds.getTimeRangeIndicator(); // octet 21
    int n = pds.getNincluded();

    int start = 0;
    int end = 0;
    int forecastTime = 0;
    boolean isInterval = false;

    switch (timeRangeIndicator) {
      /*
      Monthly-diurnal (ds628.5/fcst_phy2m125_diurnal)
      Average over the days of the month.
      128: Average of N forecast products with a valid time ranging between reference time + P1 and reference time + P2;
        products have reference times at Intervals of 24 hours, beginning at the given reference time.
       */
      case 128:
        isInterval = true;
        start = p1;
        end = p2;
        break;

      /* 129: Temporal variance of N forecasts; each product has valid time ranging between reference time + P1 and reference time + P2;
         products have reference times at intervals of 24 hours, beginning at the given reference time;
         unit of measurement is square of that in Code Table 2 */  // LOOK

      /* 131: Temporal variance of N forecasts; valid time of the first product ranges between R + P1 and R + P2,
          where R is reference time given in octets 13 to 17, then subsequent products have valid time range at interval of P2 - P1;
          thus all N products cover continuous time span; products have reference times at intervals of P2 - P1, beginning at the given reference time;
          unit of measurement is square of that in Code Table 2 */ // LOOK


      case 129:
      case 131:
        isInterval = true;
        start = p1;
        end = p2;
        break;

      /* 130: Average of N forecast products; valid time of the first product ranges between R + P1 and R + P2, where R is reference time given in octets 13 to 17,
        then subsequent products have valid time range at interval of P2 - P1; thus all N products cover continuous time span; products have reference times at
        intervals of P2 - P1, beginning at the given reference time
       */
      case 130:
        isInterval = true;
        start = p1;
        end = (n > 0) ? p1 + n * (p2 - p1) : p2;  // prob n >= 1
        break;

       /* Temporal variance of N uninitialized analyses (P1 = 0) or instantaneous forecasts (P1 > 0);
          each product has valid time at the reference time + P1;
          products have reference times at intervals of P2, beginning at the given reference time;
          unit of measurement is square of that in Code Table 2 */ // LOOK
      case 132:
        forecastTime = p1;
        start = p1;
        end = (n > 0) ? p1 + (n - 1) * p2 : p1;  // LOOK ??
        isInterval = (n > 0);
        break;

      default:
        return super.getParamTime(pds);
    }

    return new Grib1ParamTime(this, timeRangeIndicator, isInterval, start, end, forecastTime);
  }

  @Override
  public String getTimeTypeName(int timeRangeIndicator) {
    switch (timeRangeIndicator) {
      case 128:
        return "Average over the days in the month";
      case 129:
        return "Temporal variance of N forecasts at 24 hour intervals";
      case 130:
        return "Forecast, 6-hour averaged, then one-month averaged";
      case 131:
        return "Temporal variance of N forecasts at intervals of P1 - P2";
      case 132:
        return "Temporal variance of N uninitialized analyses (P1 = 0) or instantaneous forecasts (P1 > 0)";

      default:
        return super.getTimeTypeName(timeRangeIndicator);
    }
  }

  @Override
  public GribStatType getStatType(int timeRangeIndicator) {
    switch (timeRangeIndicator) {
      case 128:
      case 130:
        return GribStatType.Average;

      case 129:
      case 131:
      case 132:
        return GribStatType.Variance;
      default:
        return super.getStatType(timeRangeIndicator);
    }
  }


  ///////////////////// levels
  @Override
  public Grib1ParamLevel getParamLevel(Grib1SectionProductDefinition pds) {
    int levelType = pds.getLevelType();
    int pds11 = pds.getLevelValue1();
    int pds12 = pds.getLevelValue2();
    int pds1112 = pds11 << 8 | pds12;

    switch (levelType) {
      case 211:
      case 212:
        return new Grib1ParamLevel(this, levelType, GribNumbers.MISSING, GribNumbers.MISSING);

      case 100:
        return new Grib1ParamLevel(this, levelType, pds1112, GribNumbers.MISSING);

      case 213:
        return new Grib1ParamLevel(this, levelType, pds1112, GribNumbers.MISSING);

      default:
        return new Grib1ParamLevel(this, pds);
    }
  }

  protected GribLevelType getLevelType(int code) {
    if (levelTypesMap == null) makeLevelTypesMap();
    GribLevelType levelType = levelTypesMap.get(code);
    if (levelType != null) return levelType;
    return super.getLevelType(code);
  }

  static private void makeLevelTypesMap() {
    levelTypesMap = new HashMap<>(10);
    // (int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    levelTypesMap.put(100, new GribLevelType(100, "Isobaric Surface", "isobaric_surface_low", "hPa", null, false, false));   // 3D
    levelTypesMap.put(211, new GribLevelType(211, "Entire soil", "entire_soil", "", null, false, false));
    levelTypesMap.put(212, new GribLevelType(212, "The bottom of land surface model", "bottom_of_model", "", null, false, false));
    levelTypesMap.put(213, new GribLevelType(213, "Underground layer number of land surface model", "underground_layer", "layer", null, false, false));   // 3D
  }

  //////////////////// gen process
  private static Map<Integer, String> genProcessMap;

  @Override
  public String getGeneratingProcessName(int genProcess) {
    if (genProcessMap == null) makeGenProcessMap();
    return genProcessMap.get(genProcess);
  }

  static private void makeGenProcessMap() {
    genProcessMap = new HashMap<>(100);
    genProcessMap.put(0, "Undefined (not to specify generating process)");
    genProcessMap.put(1, "Global Spectral Model (GSM8803_T63L16)");
    genProcessMap.put(2, "Global Spectral Model (GSM8903_T106L21)");
    genProcessMap.put(3, "Global Spectral Model (GSM9603_T213L30)");
    genProcessMap.put(4, "Global Spectral Model (GSM0103_T213L40) ");
    genProcessMap.put(21, "One-week EPS (GSM0103_T106L40)");
    genProcessMap.put(31, "Regional Spectral Model (RSM0103)");
    genProcessMap.put(32, "Mesoscale Model (MSM0103)");
    genProcessMap.put(51, "One-month EPS (GSM9603_T63L30)");
    genProcessMap.put(52, "One-month EPS (GSM9603_T106L40)");
    genProcessMap.put(53, "One-month EPS (GSM0603C_TL159L40)");
    genProcessMap.put(70, "Seasonal EPS (GSM0103_T63L40)");
    genProcessMap.put(71, "Seasonal EPS (GSM0502C_TL95L40)");
    genProcessMap.put(90, "Sea surface wind correction");
    genProcessMap.put(101, "NOAA-AVHRR analysis data");
    genProcessMap.put(102, "VISSR grid point data (for cloudiness, TBB etc.)");
    genProcessMap.put(103, "Long-wave radiation data");
    genProcessMap.put(104, "GMS data (sea surface temperature)");
    genProcessMap.put(105, "Snow/ice area data");
    genProcessMap.put(106, "Global solar radiation");
    genProcessMap.put(141, "Sea surface temperature analysis (average for dekad or ten days)");
    genProcessMap.put(142, "Sea surface temperature analysis");
    genProcessMap.put(143, "Ocean current analysis");
    genProcessMap.put(144, "Global ocean wave model");
    genProcessMap.put(150, "Regional ocean wave model");
    genProcessMap.put(200, "Volcaninc ash prediction");
    genProcessMap.put(201, "Japanese 55-year Reanalysis (JRA-55)");
  }

}
