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

import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Abstract superclass for multiply nested NestedPointFeatureCollection
 * Subclass must supply getNestedPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class MultipleNestedPointCollectionImpl implements NestedPointFeatureCollection {
  protected String name;
  private FeatureType collectionFeatureType;
  private int npts;

  protected MultipleNestedPointCollectionImpl(String name, FeatureType collectionFeatureType) {
    this.name = name;
    this.collectionFeatureType = collectionFeatureType;
    this.npts = -1;
  }

  public String getName() {
    return name;
  }

  public int size() {
    return npts;
  }

  protected void setSize( int npts) {
    this.npts = npts;
  }

  public boolean isMultipleNested() {
    return true;
  }

  // All features in this collection have this feature type
  public FeatureType getCollectionFeatureType() {
    return collectionFeatureType;
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException(getClass().getName() + " (multipleNested) PointFeatureCollection does not implement getPointFeatureCollectionIterator()");
  }

  // flatten into a PointFeatureCollection
  // if empty, may return null
  public PointFeatureCollection flatten(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new NestedPointFeatureCollectionFlatten(this, boundingBox, dateRange);
  }

  private class NestedPointFeatureCollectionFlatten extends PointCollectionImpl {
    protected MultipleNestedPointCollectionImpl from;
    protected LatLonRect boundingBox;
    protected DateRange dateRange;

    NestedPointFeatureCollectionFlatten(MultipleNestedPointCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) {
      super(from.getName());
      this.from = from;
      this.boundingBox = filter_bb;
      this.dateRange = filter_date;
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      // LOOK need the isMultipleNested case
      return new PointIteratorAdapter(from.getPointFeatureCollectionIterator(bufferSize), this.boundingBox, this.dateRange);
    }

  }
}
