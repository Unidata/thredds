/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import javax.annotation.Nullable;
import ucar.nc2.constants.CF;

/**
 * Grib1 derived from Code table 5 : Time range indicator
 * Grib2 code table 4.10: Statistical process used to calculate the processed field from the field at each time increment during the time range
 * These are the standard WMO tables only.
 *
 * @author caron
 * @since 1/17/12
 */
public enum GribStatType {
  Average, Accumulation, Maximum, Minimum, DifferenceFromEnd, RootMeanSquare, StandardDeviation, Covariance, DifferenceFromStart, Ratio, Variance;

     // (code table 4.10) Statistical process used to calculate the processed field from the field at each time increment during the time range
    @Nullable
    public static GribStatType getStatTypeFromGrib2(int grib2StatCode) {
      switch (grib2StatCode) {
        case 0:
          return GribStatType.Average;
        case 1:
          return GribStatType.Accumulation;
        case 2:
          return GribStatType.Maximum;
        case 3:
          return GribStatType.Minimum;
        case 4:
          return GribStatType.DifferenceFromEnd;
        case 5:
          return GribStatType.RootMeanSquare;
        case 6:
          return GribStatType.StandardDeviation;
        case 7:
          return GribStatType.Covariance;
        case 8:
          return GribStatType.DifferenceFromStart;
        case 9:
          return GribStatType.Ratio;
        default:
          return null;
      }
    }

  /* public static String getStatTypeDescription(GribStatType statType) {
    switch (statType) {
      case Average:
        return "Average";
      case Accumulation:
        return "Accumulation";
      case Maximum:
        return "Maximum";
      case Minimum:
        return "Minimum";
      case DifferenceFromEnd:
        return "Difference (Value at the end of time range minus value at the beginning)";
      case RootMeanSquare:
        return "RootMeanSquare";
      case StandardDeviation:
        return "StandardDeviation";
      case Covariance:
        return "Covariance (Temporal variance)";
      case DifferenceFromStart:
        return "Difference (Value at the start of time range minus value at the end)";
      case Ratio:
        return "Ratio";
      default:
        return "UnknownIntervalType-" + statType;
    }
  } */

  public static int getStatTypeNumber(String  name) {
    if (name.startsWith("Average")) return 0;
    if (name.startsWith("Accumulation")) return 1;
    if (name.startsWith("Maximum")) return 2;
    if (name.startsWith("Minimum")) return 3;
    if (name.startsWith("Difference (Value at the end")) return 4;
    if (name.startsWith("Root")) return 5;
    if (name.startsWith("Standard")) return 6;
    if (name.startsWith("Covariance")) return 7;
    if (name.startsWith("Difference (Value at the start")) return 8;
    if (name.startsWith("Ratio")) return 9;
    if (name.startsWith("Variance")) return 10;
    return -1;
  }

  /**
   * Convert StatType to CF.CellMethods
   * @param stat the GRIB1 statistical type
   * @return equivalent CF, or null
   */
  @Nullable
  public static CF.CellMethods getCFCellMethod(GribStatType stat) {
    switch (stat) {
      case Average:
        return CF.CellMethods.mean;
      case Accumulation:
        return CF.CellMethods.sum;
      case Covariance:
        return CF.CellMethods.variance;
      case Minimum:
        return CF.CellMethods.minimum;
      case Maximum:
        return CF.CellMethods.maximum;
      case StandardDeviation:
        return CF.CellMethods.standard_deviation;
      case Variance:
         return CF.CellMethods.variance;
       default:
        return null;
    }
  }

}
