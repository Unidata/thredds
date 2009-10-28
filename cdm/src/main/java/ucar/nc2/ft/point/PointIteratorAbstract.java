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

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;

/**
 * Abstract superclass for PointFeatureIterator.
 * Mostly implements the bounds calculations.
 *
 * @author caron
 * @since May 11, 2009
 */
public abstract class PointIteratorAbstract implements PointFeatureIterator {
  protected boolean calcBounds = false;
  protected PointFeatureCollection collection;

  private LatLonRect bb = null;
  private double minTime = Double.MAX_VALUE;
  private double maxTime = -Double.MAX_VALUE;
  private DateUnit timeUnit;
  private int count;

  protected PointIteratorAbstract() {
  }

  public void setCalculateBounds( PointFeatureCollection collection) {
    this.calcBounds = true;
    this.collection = collection;
  }

  protected void calcBounds(PointFeature pf) {
    count++;
    if (!calcBounds) return;
    if (pf == null) return;

    if (bb == null)
      bb = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
    else
      bb.extend(pf.getLocation().getLatLon());

    if (timeUnit == null)
      timeUnit = pf.getTimeUnit();

    double obsTime = pf.getObservationTime();
    minTime = Math.min(minTime, obsTime);
    maxTime = Math.max(maxTime, obsTime);
  }

  protected void finishCalcBounds() {
    if (!calcBounds) return;

    if ((bb != null) && bb.crossDateline() && (bb.getWidth() > 350.0)) { // call it global - less confusing
      double lat_min = bb.getLowerLeftPoint().getLatitude();
      double deltaLat = bb.getUpperLeftPoint().getLatitude() - lat_min;
      bb = new LatLonRect(new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
    }

    if (collection != null) {
      if (collection.getBoundingBox() == null)
        collection.setBoundingBox(bb);
      if (collection.getDateRange() == null) {
        DateRange dr = getDateRange();
        if (dr != null)
          collection.setDateRange(dr);
      }
      if (collection.size() <= 0) {
        if (count < 0) count = 0;
        collection.setSize(count);
      }
    }
  }

  public LatLonRect getBoundingBox() {
    return bb;
  }

  public DateRange getDateRange() {
    if (!calcBounds) return null;
    if (timeUnit == null) return null;
    return new DateRange(timeUnit.makeDate(minTime), timeUnit.makeDate(maxTime));
  }

  public int getCount() { return count; }

  static public class Filter implements PointFeatureIterator.Filter {

    private LatLonRect filter_bb;
    private DateRange filter_date;

    public Filter(LatLonRect filter_bb, DateRange filter_date) {
      this.filter_bb = filter_bb;
      this.filter_date = filter_date;
    }

    public boolean filter(PointFeature pdata) {
      if ((filter_date != null) && !filter_date.included(pdata.getObservationTimeAsDate()))
        return false;

      if ((filter_bb != null) && !filter_bb.contains(pdata.getLocation().getLatitude(), pdata.getLocation().getLongitude()))
        return false;

      return true;
    }

  }

}
