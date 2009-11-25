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
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;

/**
 * Abstract superclass for implementations of StationProfileFeature.
 * Subclass must implement getPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class StationProfileFeatureImpl extends OneNestedPointCollectionImpl implements StationProfileFeature {
  protected DateUnit timeUnit;
  protected int timeSeriesNpts;
  protected Station s;
  protected PointFeatureCollectionIterator localIterator;

  public StationProfileFeatureImpl(String name, String desc, String wmoId, double lat, double lon, double alt, DateUnit timeUnit, int npts) {
    super( name, FeatureType.STATION_PROFILE);
    s = new StationImpl(name, desc, wmoId, lat, lon, alt);
    this.timeUnit = timeUnit;
    this.timeSeriesNpts = npts;
  }

  public StationProfileFeatureImpl(Station s, DateUnit timeUnit, int npts) {
    super( s.getName(), FeatureType.STATION_PROFILE);
    this.s = s;
    this.timeUnit = timeUnit;
    this.timeSeriesNpts = npts;
  }

  @Override
  public String getWmoId() {
    return s.getWmoId();
  }

  @Override
  public int size() {
    return timeSeriesNpts;
  }

  @Override
  public String getName() {
    return s.getName();
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

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public ProfileFeature next() throws IOException {
    return (ProfileFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator(-1);
  }

  @Override
  public int compareTo(Station so) {
    return s.getName().compareTo( so.getName());
  }

  @Override
  public StationProfileFeature subset(DateRange dateRange) throws IOException {
    return null;  // LOOK
  }

  @Override
  public StationProfileFeature subset(LatLonRect dateRange) throws IOException {
    return this;
  }

}
