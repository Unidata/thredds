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

import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;

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
  protected int npts;

  public StationFeatureImpl( String name, String desc, double lat, double lon, double alt, DateUnit timeUnit, int npts) {
    super(name);
    s = new StationImpl(name, desc, lat, lon, alt);
    this.timeUnit = timeUnit;
    this.npts = npts;
  }

  public StationFeatureImpl( Station s, DateUnit timeUnit, int npts) {
    super(s.getName());
    this.s = s;
    this.timeUnit = timeUnit;
    this.npts = npts;
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

  @Override
  public FeatureType getCollectionFeatureType() {
    return FeatureType.STATION;
  }

  public StationTimeSeriesFeature subset(DateRange dateRange) throws IOException {
    if (dateRange == null) return this;
    return new StationFeatureSubset(this, dateRange);
  }

  private class StationFeatureSubset extends StationFeatureImpl {
    StationFeatureImpl from;

    StationFeatureSubset(StationFeatureImpl from, DateRange filter_date) {
      super(from.s, from.timeUnit, -1);
      this.from = from;

      if (filter_date == null) {
        this.dateRange = from.dateRange;
      } else {
        this.dateRange =  (from.dateRange == null) ? filter_date : from.dateRange.intersect( filter_date);
      }
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new PointIteratorFiltered( from.getPointFeatureIterator(bufferSize), null, this.dateRange);
    }
  }
}