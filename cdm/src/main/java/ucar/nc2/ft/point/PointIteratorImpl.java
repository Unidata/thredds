/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.nc2.ft.*;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.IOException;

/**
 * Use StructureDataIterator to iterate over members of a Structure, with optional filtering.
 * Subclass must implement makeFeature().
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class PointIteratorImpl implements PointFeatureIterator {

  // if return null, skip it
  protected abstract PointFeature makeFeature(int recnum, StructureData sdata) throws IOException;

  private Filter filter;
  private PointFeature feature = null;
  private StructureDataIterator structIter;
  private int count = 0;

  // optionally calculate bounding box, date range
  protected boolean calcBB;
  private LatLonRect bb = null;
  private double minTime = Double.MAX_VALUE;
  private double maxTime = -Double.MAX_VALUE;

  protected PointIteratorImpl(Filter filter, boolean calcBB) throws IOException {
    this.filter = filter;
    this.calcBB = calcBB;
  }

  protected PointIteratorImpl(StructureDataIterator structIter, Filter filter, boolean calcBB) throws IOException {
    this.structIter = structIter;
    this.filter = filter;
    this.calcBB = calcBB;
  }

  public boolean hasNext() throws IOException {
    while (true) {
      StructureData sdata = nextStructureData();
      if (sdata == null) break;
      feature = makeFeature(count, sdata);
      if (feature == null) continue;
      if (filter == null || filter.filter(feature))
        return true;
    }

    // all done
    if (calcBB) finishCalc();
    feature = null;
    return false;
  }

  public PointFeature next() throws IOException {
    if (feature == null) return null;
    if (calcBB) doCalc(feature);
    count++;
    return feature;
  }

  public void setBufferSize(int bytes) {
    structIter.setBufferSize(bytes);
  }

  // so subclasses can override
  protected StructureData nextStructureData() throws IOException {
    return structIter.hasNext() ? structIter.next() : null;
  }

  ////////////////////////////////////////////////////////////////////
  // bb, dateRange calculations
  private void doCalc(PointFeature pf) {
    if (bb == null)
      bb = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
    else
      bb.extend(pf.getLocation().getLatLon());

    double obsTime = pf.getObservationTime();
    minTime = Math.min(minTime, obsTime);
    maxTime = Math.max(maxTime, obsTime);
  }

  private void finishCalc() {
    if (bb.crossDateline() && bb.getWidth() > 350.0) { // call it global - less confusing
      double lat_min = bb.getLowerLeftPoint().getLatitude();
      double deltaLat = bb.getUpperLeftPoint().getLatitude() - lat_min;
      bb = new LatLonRect(new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
    }
    //calcBB = false;
  }

  /**
   * Get BoundingBox after iteration is finished, if calcBB was set true
   * @return BoundingBox of all returned points
   */
  public LatLonRect getBoundingBox() {
    return bb;
  }

  /**
   * Get DateRange of observation time after iteration is finished, if calcBB was set true
   * @param timeUnit to convert times to dates
   * @return DateRange of all returned points
   */
  public DateRange getDateRange(DateUnit timeUnit) {
    return new DateRange(timeUnit.makeDate(minTime), timeUnit.makeDate(maxTime));
  }

  /**
   * Get number of points returned so far
   * @return number of points returned so far
   */
  public int getCount() {
    return count;
  }

}
