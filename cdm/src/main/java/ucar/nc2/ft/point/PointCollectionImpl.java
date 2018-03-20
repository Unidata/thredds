/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.Nonnull;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Abstract superclass for PointFeatureCollection
 * Subclass must supply getPointFeatureIterator().
 *
 * @author caron
 * @since Mar 1, 2008
 */
public abstract class PointCollectionImpl extends DsgCollectionImpl implements PointFeatureCollection {

  protected PointCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits);
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.POINT;
  }

  @Override
  public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return new PointCollectionSubset(this, boundingBox, dateRange);
  }

  @Override
  public Iterator<PointFeature> iterator() {
    try {
      return getPointFeatureIterator();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // for subsetting, the best we can do in general is to filter the original iterator.
  // subclasses may do something better
  protected static class PointCollectionSubset extends PointCollectionImpl {
    protected PointCollectionImpl from;
    protected LatLonRect filter_bb;
    protected CalendarDateRange filter_date;

    public PointCollectionSubset(PointCollectionImpl from, LatLonRect filter_bb, CalendarDateRange filter_date) {
      super(from.name, from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.filter_bb = filter_bb;
      this.filter_date = filter_date;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(), filter_bb, filter_date);
    }
  }

  ///////////////////////////////////////////////////////////////////
  // deprecated, use iterator()

  protected PointFeatureIterator localIterator;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public void finish() {
    if (localIterator != null)
      localIterator.close();
  }

  @Override
  public PointFeature next() throws IOException {
    return localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureIterator();
  }
}
