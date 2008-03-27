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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.NestedPointFeatureCollectionIterator;
import ucar.nc2.dt2.NestedPointFeatureCollection;

import java.io.IOException;

/**
 * @author caron
 * @since Mar 20, 2008
 */
public class NestedPointCollectionIteratorFiltered implements NestedPointFeatureCollectionIterator {

  private NestedPointFeatureCollectionIterator npfciter;
  private NestedPointFeatureCollectionIterator.Filter filter;

  private NestedPointFeatureCollection pointFeatureCollection;
  private boolean done = false;

  NestedPointCollectionIteratorFiltered(NestedPointFeatureCollectionIterator npfciter, NestedPointFeatureCollectionIterator.Filter filter) {
    this.npfciter = npfciter;
    this.filter = filter;
  }

  public void setBufferSize(int bytes) {
    npfciter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;
    pointFeatureCollection = nextFilteredPointFeatureCollection();
    return (pointFeatureCollection != null);
  }

  public NestedPointFeatureCollection nextFeature() throws IOException {
    return done ? null : pointFeatureCollection;
  }

  private boolean filter(NestedPointFeatureCollection pdata) {
    return (filter == null) || filter.filter(pdata);
  }

  private NestedPointFeatureCollection nextFilteredPointFeatureCollection() throws IOException {
    if ( npfciter == null) return null;
    if (!npfciter.hasNext()) return null;

    NestedPointFeatureCollection pdata = npfciter.nextFeature();
    if (!filter(pdata)) {
      if (!npfciter.hasNext()) return null;
      pdata = npfciter.nextFeature();
    }

    return pdata;
  }

}



