/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;

import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;

/**
 * Filter a PointFeatureCollectionIterator
 * @author caron
 * @since Mar 20, 2008
 */
public class PointCollectionIteratorFiltered implements PointFeatureCollectionIterator {

  private PointFeatureCollectionIterator pfciter;
  private PointFeatureCollectionIterator.Filter filter;

  private PointFeatureCollection pointFeatureCollection;
  private boolean done = false;

  public PointCollectionIteratorFiltered(PointFeatureCollectionIterator pfciter, PointFeatureCollectionIterator.Filter filter) {
    this.pfciter = pfciter;
    this.filter = filter;
  }

  @Override
  public boolean hasNext() throws IOException {
    if (done) return false;

    pointFeatureCollection = nextFilteredPointFeatureCollection();
    return (pointFeatureCollection != null);
  }

  @Override
  public PointFeatureCollection next() throws IOException {
    return done ? null : pointFeatureCollection;
  }

  @Override
  public void close() {
    pfciter.close();
  }

  private boolean filter(PointFeatureCollection pdata) {
    return (filter == null) || filter.filter(pdata);
  }

  private PointFeatureCollection nextFilteredPointFeatureCollection() throws IOException {
    //if ( pfciter == null) return null;
    if (!pfciter.hasNext()) {
      pfciter.close();
      return null;
    }

    PointFeatureCollection pdata = pfciter.next();
    while (!filter(pdata)) {
      if (!pfciter.hasNext()) {
        pfciter.close();
        return null;
      }
      pdata = pfciter.next();
    }

    return pdata;
  }

}


