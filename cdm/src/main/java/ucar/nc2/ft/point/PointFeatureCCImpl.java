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
package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Abstract superclass for singly nested NestedPointFeatureCollection, such as Station, Profile, and Trajectory.
 * Subclass must supply getPointFeatureCollectionIterator()
 *
 * @author caron
 * @since Mar 20, 2008
 */
public abstract class PointFeatureCCImpl extends DsgCollectionImpl implements PointFeatureCC {
  protected FeatureType collectionFeatureType;

  protected PointFeatureCCImpl(String name, CalendarDateUnit timeUnit, String altUnits, FeatureType collectionFeatureType) {
    super(name, timeUnit, altUnits);
    this.collectionFeatureType = collectionFeatureType;
  }

  // All features in this collection have this feature type
  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return collectionFeatureType;
  }

  // flatten into a PointFeatureCollection
  /* if empty, may return null
  @Override
  public PointFeatureCollection flatten(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return new NestedPointFeatureCollectionFlatten(this, boundingBox, dateRange);
  } */

  private static class NestedPointFeatureCollectionFlatten extends PointCollectionImpl {
    protected PointFeatureCCImpl from;
    protected LatLonRect filter_bb;
    protected CalendarDateRange filter_date;

    NestedPointFeatureCollectionFlatten(PointFeatureCCImpl from, LatLonRect filter_bb, CalendarDateRange filter_date) {
      super( from.getName(), from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.filter_bb = filter_bb;
      this.filter_date = filter_date;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      return new PointIteratorFlatten( from.getCollectionIterator(), filter_bb, filter_date);
    }
  }
}
