/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import ucar.ma2.StructureData;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A set of observations along the vertical (z) axis.
 * A profile has a nominal lat/lon and time.
 * Actual time may be constant, or vary with z.
 * The z coordinates are monotonic, and may be increasing or decreasing.
 *
 * @author caron
 * @since Feb 8, 2008
 */
public interface ProfileFeature extends PointFeatureCollection, Iterable<PointFeature> {

  /**
   * Nominal location of this profile
   */
  @Nonnull
  LatLonPoint getLatLon();

  /**
   * Nominal time of the profile
   */
  @Nonnull
  CalendarDate getTime();

  /**
   * The number of points along the z axis. May not be known until after iterating through the collection.
   * @return number of points along the z axis, or -1 if not known.
   */
  int size();

  /**
   * The data associated with the profile feature.
   * @return the actual data of this profile. may be empty, not null.
   */
  @Nonnull
  StructureData getFeatureData() throws IOException;
}
