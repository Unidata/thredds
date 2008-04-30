/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureCollection;

import java.io.IOException;

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

  PointCollectionIteratorFiltered(PointFeatureCollectionIterator pfciter, PointFeatureCollectionIterator.Filter filter) {
    this.pfciter = pfciter;
    this.filter = filter;
  }

  public void setBufferSize(int bytes) {
    pfciter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;

    pointFeatureCollection = nextFilteredPointFeatureCollection();
    return (pointFeatureCollection != null);
  }

  public PointFeatureCollection next() throws IOException {
    return done ? null : pointFeatureCollection;
  }

  private boolean filter(PointFeatureCollection pdata) {
    return (filter == null) || filter.filter(pdata);
  }

  private PointFeatureCollection nextFilteredPointFeatureCollection() throws IOException {
    if ( pfciter == null) return null;
    if (!pfciter.hasNext()) return null;

    PointFeatureCollection pdata = pfciter.next();
    if (!filter(pdata)) {
      if (!pfciter.hasNext()) return null;
      pdata = pfciter.next();
    }

    return pdata;
  }

}


