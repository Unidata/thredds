/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft;

import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.EarthLocation;

/**
 * A collection of observations at one time and location.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public interface PointFeature {
  /**
   * Location of this observation
   * @return the location of this observation
   */
  @Nonnull
  EarthLocation getLocation();

  /**
   * Actual time of this observation.
   * Convert to CalendarDate with getFeatureCollection().getTimeUnit().makeDate()
   * @return actual time of this observation.
   */
  double getObservationTime();

  /**
   * Actual time of this observation, as a CalendarDate.
   * @return actual time of this observation, as a CalendarDate.
   */
  @Nonnull
  CalendarDate getObservationTimeAsCalendarDate();

  /**
   * Nominal time of this observation.
   * Convert to Date with getTimeUnit().makeDate().
   * When the nominal time is not given in the data, it is usually set to the observational time.
   * @return Nominal time of this observation.
   */
  double getNominalTime();

  /**
   * Nominal time of this observation, as a CalendarDate.
   * Will be equal to the observation date if not exists independently.
   * @return Nominal time of this observation, as a CalendarDate.
   */
  @Nonnull
  CalendarDate getNominalTimeAsCalendarDate();

  /**
   * The actual data of just this PointFeature.
   * This is the data of the innermost nested table, aka leaf data.
   */
  @Nonnull
  StructureData getFeatureData() throws java.io.IOException;

  /**
   * All the data of this observation, joined with data from all parent features.
   */
  @Nonnull
  StructureData getDataAll() throws java.io.IOException;

 /**
  * Get the containing DsgFeatureCollection
  */
 @Nonnull
 DsgFeatureCollection getFeatureCollection();
}
