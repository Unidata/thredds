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
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
//import ucar.unidata.geoloc.Station;

/**
 * Superclass for implementations of StationFeature: time series of data at a point
 * Concrete subclass must implement getFeatureData() and getPointFeatureIterator();
 *
 * @author caron
 */

public abstract class StationTimeSeriesFeatureImpl extends PointCollectionImpl implements StationTimeSeriesFeature {
  protected StationFeature s;

  public StationTimeSeriesFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt,
                                      CalendarDateUnit timeUnit, String altUnits, int npts, StructureData sdata) {
    super(name, timeUnit, altUnits);
    s = new StationFeatureImpl(name, desc, wmoId, lat, lon, alt, npts, sdata);
  }

  public StationTimeSeriesFeatureImpl(StationFeature s, CalendarDateUnit timeUnit, String altUnits, int nfeatures) {
    super(s.getName(), timeUnit, altUnits);
    this.s = s;
    if (nfeatures >= 0) {
      getInfo(); // create the object
      info.nfeatures = nfeatures;
    }
  }

  @Override
  public String getWmoId() {
    return s.getWmoId();
  }

  @Override
  public String getDescription() {
    return s.getDescription();
  }

  @Override
  public double getLatitude() {
    return s.getLatitude();
  }

  @Override
  public double getLongitude() {
    return s.getLongitude();
  }

  @Override
  public double getAltitude() {
    return s.getAltitude();
  }

  @Override
  public LatLonPoint getLatLon() {
    return s.getLatLon();
  }

  @Override
  public boolean isMissing() {
    return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.STATION;
  }

  @Override
  public String toString() {
    return "StationFeatureImpl{" +
        "s=" + s +
        '}';
  }

  @Override
  public StationTimeSeriesFeature subset(CalendarDateRange dateRange) throws IOException {
    if (dateRange == null) return this;
    return new StationFeatureSubset(this, dateRange);
  }

  public int compareTo(Station so) {
    return name.compareTo(so.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StationTimeSeriesFeatureImpl)) {
      return false;
    }

    StationTimeSeriesFeatureImpl that = (StationTimeSeriesFeatureImpl) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public static class StationFeatureSubset extends StationTimeSeriesFeatureImpl {
    private final StationTimeSeriesFeatureImpl from;
    private CalendarDateRange filter_date;

    public StationFeatureSubset(StationTimeSeriesFeatureImpl from, CalendarDateRange filter_date) {
      super(from.s, from.getTimeUnit(), from.getAltUnits(), -1);
      this.from = from;
      this.filter_date = filter_date;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointIteratorFiltered(from.getPointFeatureIterator(bufferSize), null, filter_date);
    }

    @Nonnull
    @Override
     public StructureData getFeatureData() throws IOException {
       return from.getFeatureData();
     }
  }
}
