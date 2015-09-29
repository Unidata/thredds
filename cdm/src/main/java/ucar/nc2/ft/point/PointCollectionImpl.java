/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.Nonnull;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Abstract superclass for PointFeatureCollection
 * Subclass must supply getPointFeatureIterator().
 *
 * @author caron
 * @since Mar 1, 2008
 */
public abstract class PointCollectionImpl extends DsgCollectionImpl implements PointFeatureCollection {

  protected PointCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits);
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.POINT;
  }

  @Override
  public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
    return new PointCollectionSubset(this, boundingBox, dateRange);
  }

  @Override
  public Iterator<PointFeature> iterator() {
    try {
      return getPointFeatureIterator(-1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // for subsetting, the best we can do in general is to filter the original iterator.
  // subclasses may do something better
  protected static class PointCollectionSubset extends PointCollectionImpl {
    protected PointCollectionImpl from;
    protected LatLonRect filter_bb;
    protected CalendarDateRange filter_date;

    public PointCollectionSubset(PointCollectionImpl from, LatLonRect filter_bb, CalendarDateRange filter_date) {
      super(from.name, from.getTimeUnit(), from.getAltUnits());
      this.from = from;
      this.filter_bb = filter_bb;
      this.filter_date = filter_date;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(bufferSize), filter_bb, filter_date);
    }
  }

  ///////////////////////////////////////////////////////////////////
  // deprecated, use iterator()

  protected PointFeatureIterator localIterator;

  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  public void finish() {
    if (localIterator != null)
      localIterator.close();
  }

  public PointFeature next() throws IOException {
    return localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = getPointFeatureIterator(-1);
  }
}
