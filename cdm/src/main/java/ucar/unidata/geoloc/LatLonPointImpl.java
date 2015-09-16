/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc;

import java.util.Formatter;

/**
 * Standard implementation of LatLonPoint.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 *
 * @author Russ Rew
 * @author John Caron
 * @see LatLonPoint
 */
public class LatLonPointImpl implements LatLonPoint, java.io.Serializable {

  /**
   * Test if point lies between two longitudes, deal with wrapping.
   *
   * @param lon    point to test
   * @param lonBeg beginning longitude
   * @param lonEnd ending longitude
   * @return true if lon is between lonBeg and lonEnd.
   * @deprecated
   */
  static public boolean betweenLon(double lon, double lonBeg, double lonEnd) {
    lonBeg = lonNormal(lonBeg, lon);
    lonEnd = lonNormal(lonEnd, lon);
    return (lon >= lonBeg) && (lon <= lonEnd);
  }

  static public double getClockwiseDistanceTo(double from, double to) {
    double distance = to - from;
    while (distance < 0.0)
      distance += 360.0;
    return distance;
  }


  /**
   * put longitude into the range [-180, 180] deg
   *
   * @param lon lon to normalize
   * @return longitude in range [-180, 180] deg
   */
  static public double range180(double lon) {
    return lonNormal(lon);
  }

  /**
   * put longitude into the range [0, 360] deg
   *
   * @param lon lon to normalize
   * @return longitude into the range [0, 360] deg
   */
  static public double lonNormal360(double lon) {
    return lonNormal(lon, 180.0);
  }

  /**
   * put longitude into the range [center +/- 180] deg
   *
   * @param lon    lon to normalize
   * @param center center point
   * @return longitude into the range [center +/- 180] deg
   */
  static public double lonNormal(double lon, double center) {
    return center + Math.IEEEremainder(lon - center, 360.0);
  }

  /**
   * put longitude into the range [start, start+360] deg
   *
   * @param lon    lon to normalize
   * @param start starting point
   * @return longitude into the [start, start+360] deg
   */
  static public double lonNormalFrom(double lon, double start) {
    while (lon < start) lon += 360;
    while (lon > start+360) lon -= 360;
    return lon;
  }

  /**
   * Normalize the longitude to lie between +/-180
   *
   * @param lon east latitude in degrees
   * @return normalized lon
   */
  static public double lonNormal(double lon) {
    if ((lon < -180.) || (lon > 180.)) {
      return Math.IEEEremainder(lon, 360.0);
    } else {
      return lon;
    }
  }

  /**
   * Find difference (lon1 - lon2) normalized so that maximum value is += 180.
   * @param lon1 start
   * @param lon2 end
   * @return
   */
  static public double lonDiff(double lon1, double lon2) {
    return Math.IEEEremainder(lon1-lon2, 360.0);
  }

  /**
   * Normalize the latitude to lie between +/-90
   *
   * @param lat north latitude in degrees
   * @return normalized lat
   */
  static public double latNormal(double lat) {
    if (lat < -90.) {
      return -90.;
    } else if (lat > 90.) {
      return 90.;
    } else {
      return lat;
    }
  }

  /**
   * Make a nicely formatted representation of a latitude, eg 40.34N or 12.9S.
   *
   * @param lat       the latitude.
   * @param ndec      number of digits to right of decimal point
   * @return String representation.
   */
  static public String latToString(double lat, int ndec) {
    boolean is_north = (lat >= 0.0);
    if (!is_north)
      lat = -lat;

    String f = "%."+ndec+"f";

    Formatter latBuff = new Formatter();
    latBuff.format(f, lat);
    latBuff.format("%s", is_north ? "N" : "S");

    return latBuff.toString();
  }

  /**
   * Make a nicely formatted representation of a longitude, eg 120.3W or 99.99E.
   *
   * @param lon       the longitude.
   * @param ndec      number of digits to right of decimal point
   * @return String representation.
   */
  static public String lonToString(double lon, int ndec) {
    double wlon = lonNormal(lon);
    boolean is_east = (wlon >= 0.0);
    if (!is_east)
      wlon = -wlon;

    String f = "%."+ndec+"f";

    Formatter latBuff = new Formatter();
    latBuff.format(f, wlon);
    latBuff.format("%s", is_east ? "E" : "W");

    return latBuff.toString();
  }

  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * East latitude in degrees, always +/- 90
   */
  private double lat;

  /**
   * North longitude in degrees, always +/- 180
   */
  private double lon;

  /**
   * Default constructor with values 0,0.
   */
  public LatLonPointImpl() {
    this(0.0, 0.0);
  }

  /**
   * Copy Constructor.
   *
   * @param pt point to copy
   */
  public LatLonPointImpl(LatLonPoint pt) {
    this(pt.getLatitude(), pt.getLongitude());
  }

  /**
   * Creates a LatLonPoint from component latitude and longitude values.
   * The longitude is adjusted to be in the range [-180.,180.].
   *
   * @param lat north latitude in degrees
   * @param lon east longitude in degrees
   */
  public LatLonPointImpl(double lat, double lon) {
    setLatitude(lat);
    setLongitude(lon);
  }

  /**
   * Returns the longitude, in degrees.
   *
   * @return the longitude, in degrees
   */
  public double getLongitude() {
    return lon;
  }

  /**
   * Returns the latitude, in degrees.
   *
   * @return the latitude, in degrees
   */
  public double getLatitude() {
    return lat;
  }

  /**
   * set lat, lon using values of pt
   *
   * @param pt point to use
   */
  public void set(LatLonPoint pt) {
    setLongitude(pt.getLongitude());
    setLatitude(pt.getLatitude());
  }

  /**
   * set lat, lon using double values
   *
   * @param lat lat value
   * @param lon lon value
   */
  public void set(double lat, double lon) {
    setLongitude(lon);
    setLatitude(lat);
  }

  /**
   * set lat, lon using float values
   *
   * @param lat lat value
   * @param lon lon value
   */
  public void set(float lat, float lon) {
    setLongitude((double) lon);
    setLatitude((double) lat);
  }

  /**
   * Set the longitude, in degrees. It is normalized to +/-180.
   *
   * @param lon east longitude in degrees
   */
  public void setLongitude(double lon) {
    this.lon = lonNormal(lon);
  }

  /**
   * Set the latitude, in degrees. Must lie beween +/-90
   *
   * @param lat north latitude in degrees
   */
  public void setLatitude(double lat) {
    this.lat = latNormal(lat);
  }


  /**
   * Check for equality with another object.
   *
   * @param obj object to check
   * @return true if this represents the same point as pt
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof LatLonPointImpl)) {
      return false;
    }
    LatLonPointImpl that = (LatLonPointImpl) obj;
    return (this.lat == that.lat) && (this.lon == that.lon);
  }

  /**
   * Check for equality with another point.
   *
   * @param pt point to check
   * @return true if this represents the same point as pt
   */
  public boolean equals(LatLonPoint pt) {
    boolean lonOk = closeEnough(pt.getLongitude(), this.lon);
    if (!lonOk) {
      lonOk = closeEnough(lonNormal360(pt.getLongitude()), lonNormal360(this.lon));
    }
    return lonOk && closeEnough(pt.getLatitude(), this.lat);
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Check to see if the values are close enough.
   *
   * @param d1 first value
   * @param d2 second value
   * @return true if they are pretty close
   */
  private boolean closeEnough(double d1, double d2) {
    // TODO:  This should be moved to a utility method in ucar.util
    // that all ucar classes could use.
    if (d1 != 0.0) {
      return Math.abs((d1 - d2) / d1) < 1.0e-9;
    }
    if (d2 != 0.0) {
      return Math.abs((d1 - d2) / d2) < 1.0e-9;
    }
    return true;
  }

  /**
   * Default string representation
   *
   * @return string representing this point
   */
  public String toString() {
    return toString(4);
  }

  /**
   * String representation in the form, eg 40.23N 105.1W
   *
   * @param sigDigits significant digits
   * @return String representation
   */
  public String toString(int sigDigits) {
    Formatter sbuff = new Formatter();
    sbuff.format("%s %s", latToString(lat, sigDigits), lonToString(lon, sigDigits));
    return sbuff.toString();
  }

}