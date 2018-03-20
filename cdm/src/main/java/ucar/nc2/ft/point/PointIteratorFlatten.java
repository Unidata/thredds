/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.IOException;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.IOIterator;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Adapt a PointFeatureCollectionIterator to a PointFeatureIterator, by flattening all the iterators in the collection
 * into a single iterator over PointFeatures. Optionally add date and space filters.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class PointIteratorFlatten extends PointIteratorAbstract {
  private IOIterator<PointFeatureCollection> collectionIter;
  private Filter filter = null;

  private PointFeatureCollection currCollection;
  private PointFeatureIterator pfiter; // iterator over the current PointFeatureCollection
  private PointFeature pointFeature; // current PointFeature in the current PointFeatureCollection
  private boolean finished = false;

  /**
   * Constructor.
   *
   * @param collectionIter iterator over the collections
   * @param filter_bb      boundingbox, or null
   * @param filter_date    data range, or null
   */
  PointIteratorFlatten(IOIterator<PointFeatureCollection> collectionIter, LatLonRect filter_bb, CalendarDateRange filter_date) {
    this.collectionIter = collectionIter;
    if ((filter_bb != null) || (filter_date != null))
      this.filter = new PointIteratorFiltered.SpaceAndTimeFilter(filter_bb, filter_date);
  }

  @Override
  public void close() {
    if (finished) return;
    if (pfiter != null) pfiter.close();
    // collectionIter.close();
    finishCalcBounds();
    finished = true;
  }

  @Override
  public boolean hasNext() {
    try {
      pointFeature = nextFilteredDataPoint();
      if (pointFeature != null) return true;

      PointFeatureCollection feature = nextCollection();
      if (feature == null) {
        close();
        return false;
      }

      currCollection = feature;
      pfiter = feature.getPointFeatureIterator();
      return hasNext();

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public PointFeature next() {
    if (pointFeature == null) return null;
    calcBounds(pointFeature);
    return pointFeature;
  }

  private PointFeatureCollection nextCollection() throws IOException {
    if (!collectionIter.hasNext()) return null;
    return collectionIter.next();
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if (pfiter == null) return null;
    if (!pfiter.hasNext()) return null;
    PointFeature pdata = pfiter.next();

    if (filter == null)
      return pdata;

    while (!filter.filter(pdata)) {
      if (!pfiter.hasNext()) return null;
      pdata = pfiter.next();
    }
    return pdata;
  }

}
