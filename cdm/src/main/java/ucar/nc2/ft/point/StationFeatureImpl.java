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

import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Superclass for implementations of StationFeature: time series of data at a point
 * Concrete subclass must implement getPointFeatureIterator();
 *
 * @author caron
 */


public abstract class StationFeatureImpl extends PointCollectionImpl implements StationTimeSeriesFeature {
  protected Station s;
  protected DateUnit timeUnit;

  public StationFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt, DateUnit timeUnit, int npts) {
    super(name);
    s = new StationImpl(name, desc, wmoId, lat, lon, alt);
    this.timeUnit = timeUnit;
    this.npts = npts;
  }

  public StationFeatureImpl(Station s, DateUnit timeUnit, int npts) {
    super(s.getName());
    this.s = s;
    this.timeUnit = timeUnit;
    this.npts = npts;
    setBoundingBox( new LatLonRect(s.getLatLon(), .0001, .0001));
  }

  public String getWmoId() {
    return s.getWmoId();
  }

  public int size() {
    return npts;
  }

  public void setNumberPoints(int npts) {
    this.npts = npts;
  }

  public String getName() {
    return s.getName();
  }

  public String getDescription() {
    return s.getDescription();
  }

  public double getLatitude() {
    return s.getLatitude();
  }

  public double getLongitude() {
    return s.getLongitude();
  }

  public double getAltitude() {
    return s.getAltitude();
  }

  public LatLonPoint getLatLon() {
    return s.getLatLon();
  }

  public boolean isMissing() {
    return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
  }

  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.STATION;
  }

  @Override
  public String toString() {
    return "StationFeatureImpl{" +
        "s=" + s +
        ", timeUnit=" + timeUnit +
        ", npts=" + npts +
        '}';
  }

  public StationTimeSeriesFeature subset(DateRange dateRange) throws IOException {
    if (dateRange == null) return this;
    return new StationFeatureSubset(this, dateRange);
  }

  public int compareTo(Station so) {
    return name.compareTo( so.getName());
  }

  private class StationFeatureSubset extends StationFeatureImpl {
    StationFeatureImpl from;

    StationFeatureSubset(StationFeatureImpl from, DateRange filter_date) {
      super(from.s, from.timeUnit, -1);
      this.from = from;

      if (filter_date == null) {
        this.dateRange = from.dateRange;
      } else {
        this.dateRange = (from.dateRange == null) ? filter_date : from.dateRange.intersect(filter_date);
      }
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(bufferSize), null, this.dateRange);
    }

  }
}