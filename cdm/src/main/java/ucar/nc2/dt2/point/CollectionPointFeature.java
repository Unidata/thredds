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

import ucar.nc2.dt2.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.List;

/**
 * A PointFeatureCollection of PointFeature
 * @author caron
 * @since Mar 1, 2008
 */
public class CollectionPointFeature extends PointFeatureCollectionImpl {
  private LatLonRect filter_bb;
  private DateRange filter_date;

  public CollectionPointFeature(List<VariableSimpleIF> dataVariables, PointFeatureIterator pfiter) {
    super( PointFeature.class, dataVariables);
    FeatureIteratorAdapter iter = new FeatureIteratorAdapter(pfiter, null, null);
    setIterators(iter, iter);
  }

  // copy constructor
  CollectionPointFeature(CollectionPointFeature from, LatLonRect filter_bb, DateRange filter_date) {
    super(from);

    if (from.filter_bb == null)
      this.filter_bb = filter_bb;
    else
      this.filter_bb = (filter_bb == null) ? from.filter_bb : from.filter_bb.intersect( filter_bb);

    if (from.filter_date == null)
      this.filter_date = filter_date;
    else
      this.filter_date = (filter_date == null) ? from.filter_date : from.filter_date.intersect( filter_date);

    FeatureIteratorAdapter iter = new FeatureIteratorAdapter(pfiter, this.filter_bb, this.filter_date);
    setIterators(iter, iter);
  }

  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new CollectionPointFeature( this, boundingBox, dateRange);
  }

}
