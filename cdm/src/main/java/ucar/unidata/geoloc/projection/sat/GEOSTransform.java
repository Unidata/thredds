/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

 package ucar.unidata.geoloc.projection.sat;

import java.lang.Math;
import java.lang.String;
import java.lang.*;

/**
 * Geostationary Transform
 *
 * @author Tom Rink
 * @since 08/28/2014
 */
public class GEOSTransform {
  public static final String GOES = "GOES";
  public static final String GEOS = "GEOS";
  public static final String WGS84 = "WGS84";
  public static final String GRS80 = "GRS80";
  public final Geoid wgs84 = new GeoidWGS84();
  public final Geoid grs80 = new GeoidGRS80();

  private static final double DEG_TO_RAD = Math.PI / 180.0;
  private static final double RAD_TO_DEG = 180.0 / Math.PI;

  private static final  double h_msg = 42164.0;
  private static final  double h_goesr = 42164.16;

  //////////////////////////////////////

    //-  GRS80 parameters (GOES-R) default, can be changed via the ctrs ---------
  double r_pol = 6356.7523;      // semi-minor axis (polar radius km)
  double r_eq = 6378.1370;      // semi-major axis (equatorial radius km)

  private  double f = 1.0 / 298.257222101;  // flattening
  private  double fp = 1.0 / ((1.0 - f) * (1.0 - f));
  private  double h = h_goesr; // Geostationary Orbit Radius (spacecraft to barycenter distance) (km)

  double d;

  double sub_lon;
  double sub_lon_degrees;
  double sat_height;

  public String scan_geom = GEOS;

  public GEOSTransform() {
    this(0.0);
  }

  public GEOSTransform(double subLonDegrees) {
    this(subLonDegrees, GEOS);
  }

  public GEOSTransform(double subLonDegrees, String scan_geom) {
    this(subLonDegrees, scan_geom, null);
  }

  public GEOSTransform(double subLonDegrees, String scan_geom, String geoidID) {
    Geoid geoid = null;

    if (geoidID == null) {
      if (scan_geom.equals(GEOS)) {
        geoid = wgs84;
      } else if (scan_geom.equals(GOES)) {
        geoid = grs80;
      }
    } else if (geoidID.equals(WGS84)) {
      geoid = wgs84;
    } else if (geoidID.equals(GRS80)) {
      geoid = grs80;
    }

    if (geoid == null) {
      throw new IllegalArgumentException("GEOSTransform unrecognized scan_geom="+scan_geom+" geoidID="+geoidID);
    }

    init(subLonDegrees, scan_geom, geoid);
  }

  public GEOSTransform(double subLonDegrees,
                       double perspective_point_height,
                       double semi_minor_axis,
                       double semi_major_axis,
                       double inverse_flattening,
                       String sweep_angle_axis) {
    Geoid geoid;
    if (Double.isNaN(inverse_flattening)) {
      geoid = new Geoid(semi_minor_axis, semi_major_axis);
    } else {
      geoid = new Geoid(semi_minor_axis, semi_major_axis, inverse_flattening);
    }

    init(subLonDegrees, scan_geom, geoid, perspective_point_height);
  }

  public GEOSTransform(double subLonDegrees,
                       double perspective_point_height,
                       double semi_minor_axis,
                       double semi_major_axis,
                       String sweep_angle_axis) {
    Geoid geoid = new Geoid(semi_minor_axis, semi_major_axis);
    init(subLonDegrees, scan_geom, geoid, perspective_point_height);
  }

  private void init(double subLonDegrees, String scan_geom, Geoid geoid) {
    this.sub_lon_degrees = subLonDegrees;
    this.sub_lon = sub_lon_degrees * DEG_TO_RAD;
    this.scan_geom = scan_geom;

    r_pol = geoid.r_pol;
    r_eq = geoid.r_eq;
    f = geoid.f;
    fp = 1.0 / ((1.0 - f) * (1.0 - f));

    if (scan_geom.equals(GEOS)) {
      h = h_msg;
    } else if (scan_geom.equals(GOES)) {
      h = h_goesr;
    }
    this.sat_height = h - r_eq;

    d = h * h - r_eq * r_eq;
  }

  private void init(double subLonDegrees, String scan_geom, Geoid geoid, double perspective_point_height) {
    this.sub_lon_degrees = subLonDegrees;
    this.sub_lon = sub_lon_degrees * DEG_TO_RAD;
    this.scan_geom = scan_geom;
    this.sat_height = perspective_point_height;

    r_pol = geoid.r_pol;
    r_eq = geoid.r_eq;
    f = geoid.f;
    fp = 1.0 / ((1.0 - f) * (1.0 - f));

    h = perspective_point_height + r_eq;

    d = h * h - r_eq * r_eq;
  }


  /**
   * Transform geographic Earth coordinates to satellite view angle coordinate system
   * also known as the "intermediate" coordinate system in CGMS Normalized Geostationary Projection.
   *
   * @param geographic_lon longitude, units: degrees
   * @param geographic_lat latitude, units: degrees
   * @return (lamda, theta) units: radian. This is the (x,y) or (East-West, North_South) view angle.
   */
  public double[] earthToSat(double geographic_lon, double geographic_lat) {

    geographic_lat = geographic_lat * DEG_TO_RAD;
    geographic_lon = geographic_lon * DEG_TO_RAD;

    double geocentric_lat = Math.atan(((r_pol * r_pol) / (r_eq * r_eq)) * Math.tan(geographic_lat));

    double r_earth = r_pol / Math.sqrt(1.0 - ((r_eq * r_eq - r_pol * r_pol) / (r_eq * r_eq)) * Math.cos(geocentric_lat) * Math.cos(geocentric_lat));

    double r_1 = h - r_earth * Math.cos(geocentric_lat) * Math.cos(geographic_lon - sub_lon);
    double r_2 = -r_earth * Math.cos(geocentric_lat) * Math.sin(geographic_lon - sub_lon);
    double r_3 = r_earth * Math.sin(geocentric_lat);

    if (r_1 > h) { // often two geoid intersect points, use the closer one.
      return new double[]{Double.NaN, Double.NaN};
    }

    double lamda_sat = Double.NaN;
    double theta_sat = Double.NaN;

    if (scan_geom.equals(GEOS)) { // GEOS (eg. SEVIRI, MSG)  CGMS 03, 4.4.3.2, Normalized Geostationary Projection
      lamda_sat = Math.atan(-r_2 / r_1);
      theta_sat = Math.asin(r_3 / Math.sqrt(r_1 * r_1 + r_2 * r_2 + r_3 * r_3));
    } else if (scan_geom.equals(GOES)) { // GOES (eg. GOES-R ABI)
      lamda_sat = Math.asin(-r_2 / Math.sqrt(r_1 * r_1 + r_2 * r_2 + r_3 * r_3));
      theta_sat = Math.atan(r_3 / r_1);
    }

    return new double[]{lamda_sat, theta_sat};
  }

  /**
   * Transform satellite view angle coordinates, known as the "intermeidate" coordinates in the
   * CGMS Normalized Geostationary Projection, to geographic Earth coordinates.
   *
   * @param x is lamda (East-West) angle, units: radians
   * @param y is theta (Norht-South) angle, units: radians
   * @return (Longitude, Latitude), units degrees
   */
  public double[] satToEarth(double x, double y) {

    if (scan_geom.equals(GOES)) { // convert from GOES to GEOS for transfrom below
      double[] lambda_theta_geos = GOES_to_GEOS(x, y);
      x = lambda_theta_geos[0];
      y = lambda_theta_geos[1];
    }

    double c1 = (h * Math.cos(x) * Math.cos(y)) * (h * Math.cos(x) * Math.cos(y));
    double c2 = (Math.cos(y) * Math.cos(y) + fp * Math.sin(y) * Math.sin(y)) * d;

    if (c1 < c2) {
      return new double[]{Double.NaN, Double.NaN};
    }

    double s_d = Math.sqrt(c1 - c2);

    double s_n = (h * Math.cos(x) * Math.cos(y) - s_d) / (Math.cos(y) * Math.cos(y) + fp * Math.sin(y) * Math.sin(y));

    double s_1 = h - s_n * Math.cos(x) * Math.cos(y);
    double s_2 = s_n * Math.sin(x) * Math.cos(y);
    double s_3 = -s_n * Math.sin(y);

    double s_xy = Math.sqrt(s_1 * s_1 + s_2 * s_2);
    double geographic_lon = Math.atan(s_2 / s_1) + sub_lon;

    double geographic_lat = Math.atan(-fp * (s_3 / s_xy));

    double lonDegrees = RAD_TO_DEG * geographic_lon;
    double latDegrees = RAD_TO_DEG * geographic_lat;

    // force output longitude to -180 to 180 range
    if (lonDegrees < -180.0) lonDegrees += 360.0;
    if (lonDegrees > 180.0) lonDegrees -= 360.0;

    return new double[]{lonDegrees, latDegrees};
  }

  /**
   * Transform view angle coordinates in the GOES scan geometry frame to view angle coordinates
   * in the GEOS scan geometry frame.
   */
  public double[] GOES_to_GEOS(double lamda_goes, double theta_goes) {
    double theta_geos = Math.asin(Math.sin(theta_goes) * Math.cos(lamda_goes));
    double lamda_geos = Math.atan(Math.tan(lamda_goes) / Math.cos(theta_goes));

    return new double[]{lamda_geos, theta_geos};
  }


  /**
   * Transform fractional FGF coordinates to (longitude, latitude).
   *
   * @param fgf_x    fractional FGF coordinate, zero-based
   * @param fgf_y    fractional FGF coordinate, zero-based
   * @param scale_x  scaleFactor from the x coordinate variable
   * @param offset_x addOffset from the x coordinate variable
   * @param scale_y  scaleFactor from the y coordinate variable
   * @param offset_y addOffset from the y coordinate variable
   * @return (Longitude, Latitude), units: degrees
   */
  public double[] FGFtoEarth(double fgf_x, double fgf_y, double scale_x, double offset_x, double scale_y, double offset_y) {
    double[] xy = FGFtoSat(fgf_x, fgf_y, scale_x, offset_x, scale_y, offset_y);
    return satToEarth(xy[0], xy[1]);
  }

  /**
   * Transform fractional FGF coordinates to (lamda, theta) radians.
   *
   * @param fgf_x    fractional FGF coordinate, zero-based
   * @param fgf_y    fractional FGF coordinate, zero-based
   * @param scale_x  scaleFactor from the x coordinate variable
   * @param offset_x addOffset from the x coordinate variable
   * @param scale_y  scaleFactor from the y coordinate variable
   * @param offset_y addOffset from the y coordinate variable
   * @return (lamda, theta), units: radians
   */
  public double[] FGFtoSat(double fgf_x, double fgf_y, double scale_x, double offset_x, double scale_y, double offset_y) {
    double x = fgf_x * scale_x + offset_x;
    double y = fgf_y * scale_y + offset_y;

    return new double[]{x, y};
  }

  /**
   * Transform integer FGF coordinates to (longitude, latitude) of pixel center
   * The (i,j) pixel, zero-based, refers to the pixel center.
   *
   * @param scale_x  scaleFactor from the x coordinate variable
   * @param offset_x addOffset from the x coordinate variable
   * @param scale_y  scaleFactor from the y coordinate variable
   * @param offset_y addOffset from the y coordinate variable
   * @return (Longitude, Latitude), units: degrees, of FGF (i,j) pixel center
   */
  public double[] elemLineToEarth(int elem, int line, double scale_x, double offset_x, double scale_y, double offset_y) {
    return FGFtoEarth((double) elem, (double) line, scale_x, offset_x, scale_y, offset_y);
  }

  /**
   * Transform Earth coordinates (lon,lat) to fractional FGF coordinates.
   *
   * @param geographic_lon Longitude, units: degrees
   * @param geographic_lat Latitude, units: degrees
   * @param scale_x        scaleFactor from the x coordinate variable
   * @param offset_x       addOffset from the x coordinate variable
   * @param scale_y        scaleFactor from the y coordinate variable
   * @param offset_y       addOffset from the y coordinate variable
   * @return fractional fgf coordinates
   */
  public double[] earthToFGF(double geographic_lon, double geographic_lat, double scale_x, double offset_x, double scale_y, double offset_y) {
    double[] xy = earthToSat(geographic_lon, geographic_lat);
    return SatToFGF(xy[0], xy[1], scale_x, offset_x, scale_y, offset_y);
  }

  /**
   * Transform pixel center Earth coordinates (lon,lat) to integer FGF coordinates.
   *
   * @param scale_x  scaleFactor from the x coordinate variable
   * @param offset_x addOffset from the x coordinate variable
   * @param scale_y  scaleFactor from the y coordinate variable
   * @param offset_y addOffset from the y coordinate variable
   */
  public int[] earthToElemLine(double geographic_lon, double geographic_lat, double scale_x, double offset_x, double scale_y, double offset_y) {
    double[] fgf = earthToFGF(geographic_lon, geographic_lat, scale_x, offset_x, scale_y, offset_y);
    int elem = (int) Math.floor(fgf[0] + 0.5);
    int line = (int) Math.floor(fgf[1] + 0.5);
    return new int[]{elem, line};
  }

  /**
   * Transform (lamda, theta) in radians to fractional FGF coordinates.
   *
   * @param scale_x  scaleFactor from the x coordinate variable
   * @param offset_x addOffset from the x coordinate variable
   * @param scale_y  scaleFactor from the y coordinate variable
   * @param offset_y addOffset from the y coordinate variable
   */
  public double[] SatToFGF(double lamda, double theta, double scale_x, double offset_x, double scale_y, double offset_y) {
    double fgf_x = (lamda - offset_x) / scale_x;
    double fgf_y = (theta - offset_y) / scale_y;
    return new double[]{fgf_x, fgf_y};
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GEOSTransform that = (GEOSTransform) o;

    if (Double.compare(that.sub_lon, sub_lon) != 0) return false;

    return scan_geom.equals(that.scan_geom);
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(sub_lon);
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + scan_geom.hashCode();
    return result;
  }

  /**
   * Earth Geoid definitions
   * Note:  CGMS Doc No CGMS 03, Issue 2.6 states the following geoid parameters:
   * r_pol = 6356.5838 km
   * r_eq  = 6378.1690 km
   */

  class Geoid {
    double r_pol;  // semi-minor axis (polar radius km)
    double r_eq;   // semi-major axis (equatorial radius km)
    double f;      // flattening

    public Geoid() {
    }

    public Geoid(double r_pol, double r_eq, double invf) {
      this.r_pol = r_pol;
      this.r_eq = r_eq;
      this.f = 1.0 / invf;
    }

    public Geoid(double r_pol, double r_eq) {
      this.r_pol = r_pol;
      this.r_eq = r_eq;
      this.f = (r_eq - r_pol) / r_eq;
    }

  }

  class GeoidWGS84 extends Geoid {
    //-  WGS84 parameters  ------------------------------------------
    public GeoidWGS84() {
      r_pol = 6356.7523;  // kilometers
      r_eq = 6378.1370;
      f = 1.0 / 298.257223563;
    }
  }

  class GeoidGRS80 extends Geoid {
    //-  GRS80 parameters (GOES-R) --------------------------------------
    public GeoidGRS80() {
      r_pol = 6356.7523;  // kilometers
      r_eq = 6378.1370;
      f = 1.0 / 298.257222101;
    }
  }
}
