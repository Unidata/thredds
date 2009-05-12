/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
      return new PointIteratorFlatten(from.getPointFeatureCollectionIterator(bufferSize), this.boundingBox, this.dateRange);
    }

  }
}
