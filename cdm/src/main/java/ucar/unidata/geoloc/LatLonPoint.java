/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import ucar.nc2.util.Misc;

/**
 * Points on the Earth's surface, represented as (longitude,latitude),
 * in units of degrees.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 *
 * @author John Caron
 */
public interface LatLonPoint {

  /**
   * Returns the longitude, between +/-180 degrees
   *
   * @return longitude (degrees)
   */
  double getLongitude();

  /**
   * Returns the latitude, between +/- 90 degrees.
   *
   * @return latitude (degrees)
   */
  double getLatitude();

  /**
   * Returns the result of {@link #nearlyEquals(LatLonPoint, double)}, with {@link Misc#defaultMaxRelativeDiffDouble}.
   */
  default boolean nearlyEquals(LatLonPoint that) {
    return nearlyEquals(that, Misc.defaultMaxRelativeDiffDouble);
  }

  /**
   * Returns {@code true} if this point is nearly equal to {@code that}. The "near equality" of points is determined
   * using {@link Misc#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
   *
   * @param that    the other point to check.
   * @param maxRelDiff  the maximum {@link Misc#relativeDifference relative difference} the two points may have.
   * @return {@code true} if this point is nearly equal to {@code that}.
   */
  boolean nearlyEquals(LatLonPoint that, double maxRelDiff);
}
