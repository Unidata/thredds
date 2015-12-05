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
package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Stereographic projection, spherical earth.
 * Projection plane is a plane tangent to the earth at latt, lont.
 * see John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 153
 *
 * @author John Caron
 */

public class Stereographic extends ProjectionImpl {

  /**
   * Construct a Stereographic Projection using latitude of true scale and calculating scale factor.
   * <p> Since the scale factor at lat = k = 2*k0/(1+sin(lat))  [Snyder,Working Manual p157]
   * then to make scale = 1 at lat, set k0 = (1+sin(lat))/2
   *
   * @param latt    tangent point of projection, also origin of projection coord system
   * @param lont    tangent point of projection, also origin of projection coord system
   * @param latTrue latitude of true scale in degrees north; latitude where scale factor = 1.0
   * @return Stereographic projection
   */
  static public Stereographic factory(double latt, double lont, double latTrue) {
    double scale = (1.0 + Math.sin(Math.toRadians(latTrue))) / 2.0;
    return new Stereographic(latt, lont, scale);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private double falseEasting, falseNorthing;
  private double scale, earthRadius;
  private double latt, lont;
  private double sinlatt, coslatt, latts;
  private boolean isNorth = false;
  private boolean isPolar = false;

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new Stereographic(getTangentLat(), getTangentLon(), getScale(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters = North Polar
   */
  public Stereographic() {
    this(90.0, -105.0, 1.0);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt  tangent point of projection, also origin of projection coord system
   * @param lont  tangent point of projection, also origin of projection coord system
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public Stereographic(double latt, double lont, double scale) {
    this(latt, lont, scale, 0, 0, EARTH_RADIUS);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt  tangent point of projection, also origin of projection coord system
   * @param lont  tangent point of projection, also origin of projection coord system
   * @param scale scale factor at tangent point, "normally 1.0 but may be reduced"
   */
  public Stereographic(double latt, double lont, double scale, double false_easting, double false_northing) {
    this(latt, lont, scale, false_easting, false_northing, EARTH_RADIUS);
  }

  /**
   * Construct a Stereographic Projection.
   *
   * @param latt           tangent point of projection, also origin of projection coord system
   * @param lont           tangent point of projection, also origin of projection coord system
   * @param scale          scale factor at tangent point, "normally 1.0 but may be reduced"
   * @param false_easting  false easting in units of x coords
   * @param false_northing false northing in units of y coords
   * @param radius         earth radius in km
   */
  public Stereographic(double latt, double lont, double scale, double false_easting, double false_northing, double radius) {
    super("Stereographic", false);

    this.latt = Math.toRadians(latt);
    this.lont = Math.toRadians(lont);
    this.earthRadius = radius;
    this.scale = scale * earthRadius;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    precalculate();

    addParameter(CF.GRID_MAPPING_NAME, CF.STEREOGRAPHIC);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lont);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, latt);
    addParameter(CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, scale);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }
  }

  /**
   * Construct a polar Stereographic Projection, from the "natural origin" and the tangent point,
   * calculating the scale factor.
   *
   * @param lat_ts_deg Latitude at natural origin (degrees_north)
   * @param latt_deg   tangent point of projection (degrees_north)
   * @param lont_deg   tangent point of projection, also origin of projection coord system  ((degrees_east)
   * @param north      true if north pole, false is south pole
   */
  public Stereographic(double lat_ts_deg, double latt_deg, double lont_deg, boolean north) {
    super("PolarStereographic", false);

    this.latts = Math.toRadians(lat_ts_deg);
    this.latt = Math.toRadians(latt_deg);
    this.lont = Math.toRadians(lont_deg);
    this.isPolar = true;
    this.isNorth = north;
    this.earthRadius = EARTH_RADIUS;
    this.falseEasting = 0;
    this.falseNorthing = 0;

    precalculate();

    double scaleFactor = (lat_ts_deg == 90 || lat_ts_deg == -90) ? 1.0 : getScaleFactor(latts, north);
    this.scale = scaleFactor * earthRadius;

    addParameter(CF.GRID_MAPPING_NAME, "polar_stereographic");
    addParameter("longitude_of_projection_origin", lont_deg);
    addParameter("latitude_of_projection_origin", latt_deg);
    addParameter("scale_factor_at_projection_origin", scaleFactor);
  }

  /**
   * Calculate polar stereographic scale factor based on the natural latitude and
   * longitude of the original
   * Ref: OGP Surveying and Positioning Guidance Note number 7, part 2 April 2009
   * http://www.epsg.org
   * added by Qun He <qunhe@unc.edu>
   *
   * @param lat_ts Latitude at natural origin
   * @param north  Is it north polar?
   * @return scale factor
   */
  private double getScaleFactor(double lat_ts, boolean north) {
    double e = 0.081819191;
    double tf = 1.0, mf = 1.0, k0 = 1.0;
    double root = (1 + e * Math.sin(lat_ts)) / (1 - e * Math.sin(lat_ts));
    double power = e / 2;

    if (north)
      tf = Math.tan(Math.PI / 4 - lat_ts / 2) * (Math.pow(root, power));
    else
      tf = Math.tan(Math.PI / 4 + lat_ts / 2) / (Math.pow(root, power));

    mf = Math.cos(lat_ts) / Math.sqrt(1 - e * e * Math.pow(Math.sin(lat_ts), 2));
    k0 = mf * Math.sqrt(Math.pow(1 + e, 1 + e) * Math.pow(1 - e, 1 - e)) / (2 * tf);

    return Double.isNaN(k0) ? 1.0 : k0;
  }

  /**
   * precalculate some stuff
   */
  private void precalculate() {
    sinlatt = Math.sin(latt);
    coslatt = Math.cos(latt);
  }

  // bean properties

  /**
   * Get the scale
   *
   * @return the scale
   */
  public double getScale() {
    return scale / earthRadius;
  }

  /**
   * Get the latitude at natural origin
   *
   * @return latitude at natural origin
   */
  public double getNaturalOriginLat() {
    return Math.toDegrees(latts);
  }

    /**
   * Get the tangent longitude in degrees
   *
   * @return the origin longitude.
   */
  public double getTangentLon() {
    return Math.toDegrees(lont);
  }

  /**
   * Get the tangent latitude in degrees
   *
   * @return the origin latitude.
   */
  public double getTangentLat() {
    return Math.toDegrees(latt);
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  public boolean isNorth() {
    return isNorth;
  }

  public boolean isPolar() {
    return isPolar;
  }

   //////////////////////////////////////////////
  // setters for IDV serialization - do not use except for object creating

  /**
   * @deprecated
   */
  public void setScale(double scale) {
    this.scale = earthRadius * scale;
  }

  /**
   * @deprecated
   */
  public void setTangentLat(double latt) {
    this.latt =  Math.toRadians(latt);
    precalculate();
  }

  /**
   * @deprecated
   */
  public void setTangentLon(double lont) {
    this.lont = Math.toRadians(lont);
    precalculate();
  }

  // cruft from IDV bundle

  /**
   * @deprecated
   */
  public void setCentralMeridian(double lont) {
    setTangentLon(lont);
  }

  /**
   * Set the false_easting, in km.
   * natural_x_coordinate + false_easting = x coordinate
   *
   * @param falseEasting x offset
   */
  public void setFalseEasting(double falseEasting) {
    this.falseEasting = falseEasting;
  }

  /**
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   *
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
    this.falseNorthing = falseNorthing;
  }

  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return toString();
  }

  //////////////////////////////////////////////

  @Override
  public String toString() {
    return "Stereographic{" +
            "falseEasting=" + falseEasting +
            ", falseNorthing=" + falseNorthing +
            ", scale=" + scale +
            ", earthRadius=" + earthRadius +
            ", latt=" + latt +
            ", lont=" + lont +
            '}';
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;  // ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Stereographic that = (Stereographic) o;

    if (Double.compare(that.earthRadius, earthRadius) != 0) return false;
    if (Double.compare(that.falseEasting, falseEasting) != 0) return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0) return false;
    if (Double.compare(that.latt, latt) != 0) return false;
    if (Double.compare(that.lont, lont) != 0) return false;
    if (Double.compare(that.scale, scale) != 0) return false;
    if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = scale != +0.0d ? Double.doubleToLongBits(scale) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = latt != +0.0d ? Double.doubleToLongBits(latt) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lont != +0.0d ? Double.doubleToLongBits(lont) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

   /**
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }


  /*MACROBODY
  projToLatLon {double phi, lam;} {
    double rho = Math.sqrt( fromX*fromX + fromY*fromY);
    double c = 2.0 * Math.atan2( rho, 2.0*scale);
    double sinc = Math.sin(c);
    double cosc = Math.cos(c);

    if (Math.abs(rho) < TOLERANCE)
        phi = latt;
    else
        phi = Math.asin( cosc * sinlatt + fromY * sinc * coslatt / rho);

    toLat = Math.toDegrees(phi);

    if ((Math.abs(fromX) < TOLERANCE) && (Math.abs(fromY) < TOLERANCE))
        lam = lont;
    else if (Math.abs(coslatt) < TOLERANCE)
        lam = lont + Math.atan2( fromX, ((latt > 0) ? -fromY : fromY) );
    else
        lam = lont + Math.atan2( fromX*sinc, rho*coslatt*cosc - fromY*sinc*sinlatt);

    toLon = Math.toDegrees(lam);
  }

  latLonToProj {} {
    double lat = Math.toRadians (fromLat);
    double lon = Math.toRadians(fromLon);
    // keep away from the singular point
    if ((Math.abs(lat + latt) <= TOLERANCE)) {
        lat = -latt * (1.0 - TOLERANCE);
    }

    double sdlon = Math.sin(lon - lont);
    double cdlon = Math.cos(lon - lont);
    double sinlat = Math.sin(lat);
    double coslat = Math.cos(lat);

    double k = 2.0 * scale / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
    toX =  k * coslat * sdlon;
    toY =  k * ( coslatt * sinlat - sinlatt * coslat * cdlon);
}
  MACROBODY*/


  /*BEGINGENERATED*/

  /*
  Note this section has been generated using the convert.tcl script.
  This script, run as:
  tcl convert.tcl Stereographic.java
  takes the actual projection conversion code defined in the MACROBODY
  section above and generates the following 6 methods
  */


  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();


    double lat = Math.toRadians(fromLat);
    double lon = Math.toRadians(fromLon);
    // keep away from the singular point
    if ((Math.abs(lat + latt) <= TOLERANCE)) {
      lat = -latt * (1.0 - TOLERANCE);
    }

    double sdlon = Math.sin(lon - lont);
    double cdlon = Math.cos(lon - lont);
    double sinlat = Math.sin(lat);
    double coslat = Math.cos(lat);

    double k = 2.0 * scale
            / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
    toX = k * coslat * sdlon;
    toY = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

    result.setLocation(toX + falseEasting, toY + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double phi, lam;

    double rho = Math.sqrt(fromX * fromX + fromY * fromY);
    double c = 2.0 * Math.atan2(rho, 2.0 * scale);
    double sinc = Math.sin(c);
    double cosc = Math.cos(c);

    if (Math.abs(rho) < TOLERANCE) {
      phi = latt;
    } else {
      phi = Math.asin(cosc * sinlatt + fromY * sinc * coslatt / rho);
    }

    toLat = Math.toDegrees(phi);

    if ((Math.abs(fromX) < TOLERANCE) && (Math.abs(fromY) < TOLERANCE)) {
      lam = lont;
    } else if (Math.abs(coslatt) < TOLERANCE) {
      lam = lont + Math.atan2(fromX, ((latt > 0)
              ? -fromY
              : fromY));
    } else {
      lam = lont + Math.atan2(fromX * sinc, rho * coslatt * cosc - fromY * sinc * sinlatt);
    }

    toLon = Math.toDegrees(lam);

    result.setLatitude(toLat);
    result.setLongitude(toLon);
    return result;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n], where
   *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates: to[2][n]
   *                 where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *                 the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
    int cnt = from[0].length;
    float[] fromLatA = from[latIndex];
    float[] fromLonA = from[lonIndex];
    float[] resultXA = to[INDEX_X];
    float[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      double lat = Math.toRadians(fromLat);
      double lon = Math.toRadians(fromLon);
      // keep away from the singular point
      if ((Math.abs(lat + latt) <= TOLERANCE)) {
        lat = -latt * (1.0 - TOLERANCE);
      }

      double sdlon = Math.sin(lon - lont);
      double cdlon = Math.cos(lon - lont);
      double sinlat = Math.sin(lat);
      double coslat = Math.cos(lat);

      double k = 2.0 * scale
              / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
      toX = k * coslat * sdlon;
      toY = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

      resultXA[i] = (float) (toX + falseEasting);
      resultYA[i] = (float) (toY + falseNorthing);
    }
    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
   */
  public float[][] projToLatLon(float[][] from, float[][] to) {
    int cnt = from[0].length;
    float[] fromXA = from[INDEX_X];
    float[] fromYA = from[INDEX_Y];
    float[] toLatA = to[INDEX_LAT];
    float[] toLonA = to[INDEX_LON];
    double phi, lam;
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;

      double rho = Math.sqrt(fromX * fromX + fromY * fromY);
      double c = 2.0 * Math.atan2(rho, 2.0 * scale);
      double sinc = Math.sin(c);
      double cosc = Math.cos(c);

      if (Math.abs(rho) < TOLERANCE) {
        phi = latt;
      } else {
        phi = Math.asin(cosc * sinlatt
                + fromY * sinc * coslatt / rho);
      }

      toLat = Math.toDegrees(phi);

      if ((Math.abs(fromX) < TOLERANCE)
              && (Math.abs(fromY) < TOLERANCE)) {
        lam = lont;
      } else if (Math.abs(coslatt) < TOLERANCE) {
        lam = lont + Math.atan2(fromX, ((latt > 0)
                ? -fromY
                : fromY));
      } else {
        lam = lont
                + Math.atan2(fromX * sinc,
                rho * coslatt * cosc
                        - fromY * sinc * sinlatt);
      }

      toLon = Math.toDegrees(lam);

      toLatA[i] = (float) toLat;
      toLonA[i] = (float) toLon;
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n], where
   *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates: to[2][n]
   *                 where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *                 the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
    int cnt = from[0].length;
    double[] fromLatA = from[latIndex];
    double[] fromLonA = from[lonIndex];
    double[] resultXA = to[INDEX_X];
    double[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      double lat = Math.toRadians(fromLat);
      double lon = Math.toRadians(fromLon);
      // keep away from the singular point
      if ((Math.abs(lat + latt) <= TOLERANCE)) {
        lat = -latt * (1.0 - TOLERANCE);
      }

      double sdlon = Math.sin(lon - lont);
      double cdlon = Math.cos(lon - lont);
      double sinlat = Math.sin(lat);
      double coslat = Math.cos(lat);

      double k = 2.0 * scale
              / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
      toX = k * coslat * sdlon;
      toY = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

      resultXA[i] = toX + falseEasting;
      resultYA[i] = toY + falseNorthing;
    }
    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
   */
  public double[][] projToLatLon(double[][] from, double[][] to) {
    int cnt = from[0].length;
    double[] fromXA = from[INDEX_X];
    double[] fromYA = from[INDEX_Y];
    double[] toLatA = to[INDEX_LAT];
    double[] toLonA = to[INDEX_LON];
    double phi, lam;
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;

      double rho = Math.sqrt(fromX * fromX + fromY * fromY);
      double c = 2.0 * Math.atan2(rho, 2.0 * scale);
      double sinc = Math.sin(c);
      double cosc = Math.cos(c);

      if (Math.abs(rho) < TOLERANCE) {
        phi = latt;
      } else {
        phi = Math.asin(cosc * sinlatt
                + fromY * sinc * coslatt / rho);
      }

      toLat = Math.toDegrees(phi);

      if ((Math.abs(fromX) < TOLERANCE)
              && (Math.abs(fromY) < TOLERANCE)) {
        lam = lont;
      } else if (Math.abs(coslatt) < TOLERANCE) {
        lam = lont + Math.atan2(fromX, ((latt > 0)
                ? -fromY
                : fromY));
      } else {
        lam = lont
                + Math.atan2(fromX * sinc,
                rho * coslatt * cosc
                        - fromY * sinc * sinlatt);
      }

      toLon = Math.toDegrees(lam);

      toLatA[i] = toLat;
      toLonA[i] = toLon;
    }
    return to;
  }

  /*ENDGENERATED*/

}
