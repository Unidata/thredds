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
package ucar.nc2.ft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ucar.nc2.Variable;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;

import java.util.List;

/**
 * A collection of FeatureTypes.
 * Will either be a PointFeatureCollection, PointFeatureCC, or PointFeatureCCC
 *
 * @author caron
 * @since Mar 20, 2008
 */
public interface DsgFeatureCollection {
  /**
   * Get the name of this feature collection.
   * @return the name of this feature collection
   */
  @Nonnull
  String getName();

  /**
   * All features in this collection have this feature type
   * @return the feature type
   */
  @Nonnull
  ucar.nc2.constants.FeatureType getCollectionFeatureType();

  /**
   * The time unit.
   * @return  time unit, may not be null
   */
  @Nonnull
  CalendarDateUnit getTimeUnit();

  /**
   * The altitude unit string if it exists.
   * @return altitude unit string, may be null
   */
  @Nullable
  String getAltUnits();

  /*
   * Other variables needed for completeness, eg joined coordinate variables
   * @return list of extra variables, may be empty not null
   */
  @Nonnull
  List<Variable> getExtraVariables();

  /**
   * Calendar date range for the FeatureCollection. May not be known until after iterating through the collection.
   *
   * @return the calendar date range for the entire collection, or null if unknown
   */
  @Nullable
  CalendarDateRange getCalendarDateRange();

  /**
   * The boundingBox for the FeatureCollection. May not be known until after iterating through the collection.
   *
   * @return the lat/lon boundingBox for the entire collection, or null if unknown.
   */
  @Nullable
  ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /**
   * The number of Features in the collection. May not be known until after iterating through the collection.
   * @return number of elements in the collection, or -1 if not known.
   */
  int size();

}
