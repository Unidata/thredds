/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;

import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCIterator;
import ucar.nc2.util.IOIterator;

/**
 * Implement NestedPointFeatureCollectionIterator interface
 * @author caron
 * @since Mar 20, 2008
 */
public class PointFeatureCCIteratorFiltered implements PointFeatureCCIterator, IOIterator<PointFeatureCC> {

  private PointFeatureCCIterator npfciter;
  private PointFeatureCCIterator.Filter filter;

  private PointFeatureCC pointFeatureCollection;
  private boolean done = false;

  PointFeatureCCIteratorFiltered(PointFeatureCCIterator npfciter, PointFeatureCCIterator.Filter filter) {
    this.npfciter = npfciter;
    this.filter = filter;
  }

  @Override
  public boolean hasNext() throws IOException {
    if (done) return false;
    pointFeatureCollection = nextFilteredPointFeatureCollection();
    return (pointFeatureCollection != null);
  }

  @Override
  public PointFeatureCC next() throws IOException {
    return done ? null : pointFeatureCollection;
  }

  @Override
  public void close() {
    npfciter.close();
  }

  private boolean filter(PointFeatureCC pdata) {
    return (filter == null) || filter.filter(pdata);
  }

  private PointFeatureCC nextFilteredPointFeatureCollection() throws IOException {
    if ( npfciter == null) return null;
    if (!npfciter.hasNext()) {
      npfciter.close();
      return null;
    }

    PointFeatureCC pdata = npfciter.next();
    if (!filter(pdata)) {
      if (!npfciter.hasNext()) return null;
      pdata = npfciter.next();
    }

    return pdata;
  }

}



