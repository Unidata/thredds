/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import ucar.nc2.util.Misc;

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
  protected double lat;

  /**
   * North longitude in degrees, always +/- 180
   */
  protected double lon;

  /**
   * Default constructor with values 0,0.
   */
  public LatLonPointImpl() {
    // Don't initialize by calling setLatitude() and setLongitude(), as those methods throw
    // UnsupportedOperationException in LatLonPointImmutable, and this constructor is (implicitly) invoked in the
    // LatLonPointImmutable constructors.
    this.lat = 0;
    this.lon = 0;
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

  @Override
  public boolean nearlyEquals(LatLonPoint that, double maxRelDiff) {
    boolean lonOk = Misc.nearlyEquals(that.getLongitude(), this.lon, maxRelDiff);
    if (!lonOk) {
      // We may be in a situation where "this.lon ≈ -180" and "that.lon ≈ +180", or vice versa.
      // Those longitudes are equivalent, but not "nearlyEquals()". So, we normalize them both to lie in
      // [0, 360] and compare them again.
      lonOk = Misc.nearlyEquals(lonNormal360(that.getLongitude()), lonNormal360(this.lon), maxRelDiff);
    }
    return lonOk && Misc.nearlyEquals(that.getLatitude(), this.lat, maxRelDiff);
  }

  // Exact comparison is needed in order to be consistent with hashCode().
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof LatLonPoint)) {
      return false;
    }
    LatLonPoint that = (LatLonPoint) other;
    if (Double.compare(that.getLatitude(), this.getLatitude()) != 0) {
      return false;
    }
    return Double.compare(that.getLongitude(), this.getLongitude()) == 0;
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
