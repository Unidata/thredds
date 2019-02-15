/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib1.tables;

import javax.annotation.Nullable;
import ucar.nc2.grib.GribStatType;

/**
 * Standard WMO tables for time range indicator - Grib1 table 5.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1WmoTimeType {

  /**
   * The time unit statistical type, derived from code table 5)
   *
   * @return time unit statistical type, or null if unknown.
   */
  @Nullable
  public static GribStatType getStatType(int timeRangeIndicator) {
    switch (timeRangeIndicator) {
      case 3:
      case 6:
      case 7:
      case 51:
      case 113:
      case 115:
      case 117:
      case 120:
      case 123:
        return GribStatType.Average;
      case 4:
      case 114:
      case 116:
      case 124:
        return GribStatType.Accumulation;
      case 5:
        return GribStatType.DifferenceFromEnd;
      case 118:
        return GribStatType.Covariance;
      case 119:
      case 125:
        return GribStatType.StandardDeviation;
      default:
        return null;
    }
  }

}
