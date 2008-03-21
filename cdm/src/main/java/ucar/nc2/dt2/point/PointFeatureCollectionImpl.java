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

import ucar.nc2.dt2.PointFeatureCollection;
import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.dt2.PointFeature;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Abstract superclass for PointFeatureCollection
 * Subclass must supply getPointFeatureIterator().
 *
 * @author caron
 * @since Mar 1, 2008
 */
public abstract class PointFeatureCollectionImpl implements PointFeatureCollection {
  protected LatLonRect boundingBox;
  protected DateRange dateRange;

  PointFeatureCollectionImpl() {}

  PointFeatureCollectionImpl(LatLonRect boundingBox, DateRange dateRange) {
    this.boundingBox = boundingBox;
    this.dateRange = dateRange;
  }

  public Class getCollectionFeatureType() {
    return PointFeature.class;
  }

  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointFeatureCollectionSubset(this, boundingBox, dateRange);
  }

  private class PointFeatureCollectionSubset extends PointFeatureCollectionImpl {
    PointFeatureCollectionImpl from;

    PointFeatureCollectionSubset(PointFeatureCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) {
      this.from = from;

      if (filter_bb == null)
        this.boundingBox = from.boundingBox;
      else
        this.boundingBox = (from.boundingBox == null) ? filter_bb : from.boundingBox.intersect( filter_bb);

      if (filter_date == null) {
        this.dateRange = from.dateRange;
      } else {
        this.dateRange =  (from.dateRange == null) ? filter_date : from.dateRange.intersect( filter_date);
      }
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointFeatureIteratorFiltered( from.getPointFeatureIterator(bufferSize), this.boundingBox, this.dateRange);
    }
  }

}
