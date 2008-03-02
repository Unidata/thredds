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

import ucar.nc2.dt2.FeatureIterator;
import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.dt2.PointFeature;
import ucar.nc2.dt2.Feature;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Adapt a PointFeatureIterator to a FeatureAdapter, deal with possible date and bounding box filtering
 *
 * @author caron
 * @since Mar 1, 2008
 */
public class FeatureIteratorAdapter implements FeatureIterator, PointFeatureIterator {
  private PointFeatureIterator pfiter;
  private LatLonRect filter_bb;
  private DateRange filter_date;
  private PointFeature pointFeature;

  FeatureIteratorAdapter(PointFeatureIterator pfiter, LatLonRect filter_bb, DateRange filter_date) {
    this.pfiter = pfiter;
    this.filter_bb = filter_bb;
    this.filter_date = filter_date;
  }

  public boolean hasNext() throws IOException {
    pointFeature = nextFilteredFeature();
    return (pointFeature != null);
  }

  public PointFeature nextData() throws IOException {
    return pointFeature;
  }

  public Feature nextFeature() throws IOException {
    return pointFeature;
  }

  private PointFeature nextFilteredFeature() throws IOException {
    if (!pfiter.hasNext()) return null;
    pointFeature = pfiter.nextData();

    if (filter_date != null) {
      while (!filter_date.included(pointFeature.getObservationTimeAsDate())) {
        if (!pfiter.hasNext()) return null;
        pointFeature = pfiter.nextData();
      }
    }

    if (filter_bb != null) {
      while (!filter_bb.contains(pointFeature.getLocation().getLatLon())) {
        if (!pfiter.hasNext()) return null;
        pointFeature = pfiter.nextData();
      }
    }

    return pointFeature;
  }

  public void setBufferSize(int bytes) {
    pfiter.setBufferSize(bytes);
  }
}
