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

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * An implementation of PointFeatureIterator which allows filtering by dateRange and/or bounding box.
 * @author caron
 * @since Mar 20, 2008
 */
public class PointIteratorFiltered implements PointFeatureIterator {
  private PointFeatureIterator pfiter;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  private PointFeature pointFeature;
  private boolean done = false;

  PointIteratorFiltered(PointFeatureIterator pfiter, LatLonRect filter_bb, DateRange filter_date) {
    this.pfiter = pfiter;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  public void setBufferSize(int bytes) {
    pfiter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;

    pointFeature = nextFilteredDataPoint();
    return (pointFeature != null);
  }

  public PointFeature next() throws IOException {
    return done ? null : pointFeature;
  }

  private boolean filter(PointFeature pdata) {
    if ((filter_date != null) && !filter_date.included(pdata.getObservationTimeAsDate()))
      return false;

    if ((filter_bb != null) && !filter_bb.contains(pdata.getLocation().getLatLon()))
      return false;

    return true;
  }

  private PointFeature nextFilteredDataPoint() throws IOException {
    if ( pfiter == null) return null;
    if (!pfiter.hasNext()) return null;

    PointFeature pdata = pfiter.next();
    if (!filter(pdata)) {
      if (!pfiter.hasNext()) return null;
      pdata = pfiter.next();
    }

    return pdata;
  }

}

