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
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Abstract superclass for implementations of StationProfileFeature.
 * Subclass must implement getNestedPointFeatureCollection();
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class StationProfileFeatureImpl extends OneNestedPointCollectionImpl implements StationProfileFeature {
  protected DateUnit timeUnit;
  protected int timeSeriesNpts;
  protected Station s;

  public StationProfileFeatureImpl(String name, String desc, double lat, double lon, double alt, DateUnit timeUnit, int npts) {
    super( name, FeatureType.STATION_PROFILE);
    s = new StationImpl(name, desc, lat, lon, alt);
    this.timeUnit = timeUnit;
    this.timeSeriesNpts = npts;
  }

  public StationProfileFeatureImpl(Station s, DateUnit timeUnit, int npts) {
    super( s.getName(), FeatureType.STATION_PROFILE);
    this.s = s;
    this.timeUnit = timeUnit;
    this.timeSeriesNpts = npts;
  }

  public String getId() {
    return s.getName();
  }

  public int getNumberPoints() {
    return timeSeriesNpts;
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

}
