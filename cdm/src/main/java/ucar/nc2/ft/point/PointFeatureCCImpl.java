/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
