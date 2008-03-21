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

import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.dt2.PointFeature;
import ucar.nc2.dt2.PointFeatureCollectionIterator;
import ucar.nc2.dt2.PointFeatureCollection;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Adapt a FeatureIterator to a PointFeatureIterator
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class PointFeatureIteratorAdapter implements PointFeatureIterator {
  private PointFeatureCollectionIterator fiter;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  private PointFeatureIterator pfiter;
  private PointFeature pointFeature;
  private boolean done = false;

  PointFeatureIteratorAdapter(PointFeatureCollectionIterator fiter, LatLonRect filter_bb, DateRange filter_date) {
    this.fiter = fiter;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  public void setBufferSize(int bytes) {
    fiter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;

    pointFeature = nextFilteredDataPoint();
    if (pointFeature != null) return true;

    PointFeatureCollection feature = nextFilteredCollection();
    if (feature == null) {
      done = true;
      return false;
    }

    pfiter = feature.getPointFeatureIterator(-1);
    return hasNext();
  }

  public PointFeature nextData() throws IOException {
    return done ? null : pointFeature;
  }

  private PointFeatureCollection nextFilteredCollection() throws IOException {
    if (!fiter.hasNext()) return null;
    PointFeatureCollection f = (PointFeatureCollection) fiter.nextFeature();

    if (filter_bb == null)
      return f;

    /* while (!filter_bb.contains(f.getLatLon())) {
      if (!fiter.hasNext()) return null;
      f = (PointFeatureCollection) fiter.nextFeature();
    } LOOK */
    return f;
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if (pfiter == null) return null;
    if (!pfiter.hasNext()) return null;
    PointFeature pdata = pfiter.nextData();

    if (filter_date == null)
      return pdata;

    while (!filter_date.included(pdata.getObservationTimeAsDate())) {
      if (!pfiter.hasNext()) return null;
      pdata = pfiter.nextData();
    }
    return pdata;
  }

}
