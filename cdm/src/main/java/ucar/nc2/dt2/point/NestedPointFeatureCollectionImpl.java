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

import ucar.nc2.dt2.NestedPointFeatureCollection;
import ucar.nc2.dt2.PointFeatureCollectionIterator;
import ucar.nc2.dt2.PointFeatureCollection;
import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Abstract superclass for NestedPointFeatureCollection
 * Subclass must supply getPointFeatureCollectionIterator() or getNestedPointFeatureCollection().
 * @author caron
 * @since Mar 20, 2008
 */
public abstract class NestedPointFeatureCollectionImpl implements NestedPointFeatureCollection {
  private boolean isMultipleNested;
  private Class collectionFeatureType;

  NestedPointFeatureCollectionImpl(boolean isMultipleNested, Class collectionFeatureType) {
    this.isMultipleNested = isMultipleNested;
    this.collectionFeatureType = collectionFeatureType;
  }

  public boolean isMultipleNested() {
    return isMultipleNested;
  }

  // All features in this collection have this feature type
  public Class getCollectionFeatureType() {
    return collectionFeatureType;
  }

  /* use this only if its not multiply nested
  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    if (isMultipleNested) throw new UnsupportedOperationException("MultipleNested NestedPointFeatureCollection do not implement getPointFeatureCollectionIterator()");
    return null;
  }

  // use this only if it is multiply nested
  public NestedPointFeatureCollection getNestedPointFeatureCollection(int bufferSize) throws IOException {
    if (!isMultipleNested) throw new UnsupportedOperationException("!MultipleNested NestedPointFeatureCollection do not implement getNestedPointFeatureCollection()");
    return null;
  } */

  // flatten into a PointFeatureCollection
  // if empty, may return null
  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new NestedPointFeatureCollectionFlatten(this, boundingBox, dateRange);
  }

  private class NestedPointFeatureCollectionFlatten extends PointFeatureCollectionImpl {
    protected NestedPointFeatureCollectionImpl from;
    protected LatLonRect boundingBox;
    protected DateRange dateRange;

    NestedPointFeatureCollectionFlatten(NestedPointFeatureCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) {
      this.from = from;
      this.boundingBox = filter_bb;
      this.dateRange = filter_date;
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      // LOOK need the isMultipleNested case
      return new PointFeatureIteratorAdapter( from.getPointFeatureCollectionIterator(bufferSize), this.boundingBox, this.dateRange);
    }
  }
}
