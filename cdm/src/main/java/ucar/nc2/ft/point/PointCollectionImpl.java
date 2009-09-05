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

import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Abstract superclass for PointFeatureCollection
 * Subclass must supply getPointFeatureIterator().
 *
 * @author caron
 * @since Mar 1, 2008
 */
public abstract class PointCollectionImpl implements PointFeatureCollection {
  protected String name;
  protected LatLonRect boundingBox;
  protected DateRange dateRange;
  protected int npts;
  protected PointFeatureIterator localIterator;

  protected PointCollectionImpl(String name) {
    this.name = name;
    this.npts = -1;
  }

  protected PointCollectionImpl(String name, LatLonRect boundingBox, DateRange dateRange, int npts) {
    this.name = name;
    this.boundingBox = boundingBox;
    this.dateRange = dateRange;
    this.npts = npts;
  }

  public String getName() {
    return name;
  }

  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  public void finish() {
    if (localIterator != null)
      localIterator.finish();
  }

  public PointFeature next() throws IOException {
    return localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = getPointFeatureIterator(-1);
  }

  public int size() {
    return npts;
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  public ucar.unidata.geoloc.LatLonRect getBoundingBox() {
    return boundingBox;
  }

  public void setDateRange(DateRange range) {
    this.dateRange = range;
  }

  public void setBoundingBox(ucar.unidata.geoloc.LatLonRect bb) {
    this.boundingBox = bb;
  }

  public void calcBounds() throws java.io.IOException {
    if ((dateRange != null) && (boundingBox != null) && (size() > 0))
      return;

    PointFeatureIterator iter = getPointFeatureIterator(-1);
    iter.setCalculateBounds(this);
    try {
      while (iter.hasNext())
        iter.next();

    } finally {
      iter.finish();
    }

  }

  public void setSize(int npts) {
    this.npts = npts;
  }

  public FeatureType getCollectionFeatureType() {
    return FeatureType.POINT;
  }

  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new PointCollectionSubset(this, boundingBox, dateRange);
  }

  // for subsetting, the best we can do in general is to filter the original iterator.
  // subclasses may do something better
  protected class PointCollectionSubset extends PointCollectionImpl {
    protected PointCollectionImpl from;

    public PointCollectionSubset(PointCollectionImpl from, LatLonRect filter_bb, DateRange filter_date) {
      super(from.name);
      this.from = from;

      if (filter_bb == null)
        this.boundingBox = from.boundingBox;
      else
        this.boundingBox = (from.boundingBox == null) ? filter_bb : from.boundingBox.intersect(filter_bb);

      if (filter_date == null) {
        this.dateRange = from.dateRange;
      } else {
        this.dateRange = (from.dateRange == null) ? filter_date : from.dateRange.intersect(filter_date);
      }
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(bufferSize), this.boundingBox, this.dateRange);
    }
  }

}
