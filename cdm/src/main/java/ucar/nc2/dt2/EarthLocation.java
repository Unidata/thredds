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
package ucar.nc2.dt2;

/**
 * A location on the Earth.
 *
 * @author caron
 */
public class EarthLocation {
  protected double lat, lon, alt;

  /**
   * latitude in decimal degrees north
   * @return latitude in decimal degrees north
   */
  public double getLatitude() { return lat; }

  /**
   * longitude in decimal degrees east
   * @return longitude in decimal degrees east
   */
  public double getLongitude() { return lon; }

  /**
   * altitude in meters;  missing = NaN.
   * @return altitude in meters;  missing = NaN.
   */
  public double getAltitude() { return alt; }


  public EarthLocation( double lat, double lon, double alt) {
    this.lat = lat;
    this.lon = lon;
    this.alt = alt;
  }

  protected void setLatitude(double lat) { this.lat = lat; }
  protected void setLongitude(double lon) { this.lon = lon; }
  protected void setAltitude(double alt) { this.alt = alt; }

  public String toString() { return "lat="+lat+" lon="+lon+" alt="+alt; }
}
