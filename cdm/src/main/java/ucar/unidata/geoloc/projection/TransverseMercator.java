/*
 * Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
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


import com.google.common.math.DoubleMath;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.SpecialMathFunction;

/**
 * Transverse Mercator projection, spherical earth.
 * Projection plane is a cylinder tangent to the earth at tangentLon.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 53
 *
 * @author John Caron
 */

public class TransverseMercator extends ProjectionImpl {

  private double lat0, lon0, scale, earthRadius;
  private double falseEasting, falseNorthing;

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new TransverseMercator(getOriginLat(), getTangentLon(),
            getScale(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameteres
   */
  public TransverseMercator() {
    this(40.0, -105.0, .9996);
  }

  /**
   * Construct a TransverseMercator Projection.
   *
   * @param lat0       origin of projection coord system is at (lat0, tangentLon)
   * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
   * @param scale      scale factor along the central meridian
   */
  public TransverseMercator(double lat0, double tangentLon, double scale) {
    this(lat0, tangentLon, scale, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a TransverseMercator Projection.
   *
   * @param lat0       origin of projection coord system is at (lat0, tangentLon)
   * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
   * @param scale      scale factor along the central meridian
   * @param east       false easting in km
   * @param north      false northing in km
   */
  public TransverseMercator(double lat0, double tangentLon, double scale, double east, double north) {
    this(lat0, tangentLon, scale, east, north, EARTH_RADIUS);
  }

  /**
   * Construct a TransverseMercator Projection.
   *
   * @param lat0       origin of projection coord system is at (lat0, tangentLon)
   * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
   * @param scale      scale factor along the central meridian
   * @param east       false easting in units of km
   * @param north      false northing in units of km
   * @param radius     earth radius in km
   */
  public TransverseMercator(double lat0, double tangentLon, double scale, double east, double north, double radius) {
    super("TransverseMercator", false);

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(tangentLon);
    this.earthRadius = radius;
    this.scale = scale * earthRadius;
    this.falseEasting = (!Double.isNaN(east)) ? east : 0.0;
    this.falseNorthing = (!Double.isNaN(north)) ? north : 0.0;

    addParameter(CF.GRID_MAPPING_NAME, CF.TRANSVERSE_MERCATOR);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, tangentLon);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.SCALE_FACTOR_AT_CENTRAL_MERIDIAN, scale);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }
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
   * Get the tangent longitude in degrees
   *
   * @return the origin longitude.
   */
  public double getTangentLon() {
    return Math.toDegrees(lon0);
  }

  /**
   * Get the origin latitude in degrees
   *
   * @return the origin latitude.
   */
  public double getOriginLat() {
    return Math.toDegrees(lat0);
  }

  /**
   * Get the false easting, in units of km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in units of km
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  //////////////////////////////////////////////
  // setters for IDV serialization - do not use except for object creating

  /**
   * Set the scale
   *
   * @param scale  the scale
   */
  public void setScale(double scale) {
      this.scale = earthRadius * scale;
  }

  /**
   * Set the origin latitude
   *
   * @param lat  the origin latitude
   */
  public void setOriginLat(double lat) {
      lat0 = Math.toRadians(lat);
  }

  /**
   * Set the tangent longitude
   *
   * @param lon the tangent longitude
   */
  public void setTangentLon(double lon) {
      lon0 = Math.toRadians(lon);
  }

  /**
   * Set the false_easting, in km.
   * natural_x_coordinate + false_easting = x coordinate
   * @param falseEasting x offset
   */
  public void setFalseEasting(double falseEasting) {
      this.falseEasting = falseEasting;
  }

  /**
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
      this.falseNorthing = falseNorthing;
  }

  /////////////////////////////////////////////

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Transverse mercator";
  }


  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "TransverseMercator{" +
            "lat0=" + lat0 +
            ", lon0=" + lon0 +
            ", scale=" + scale +
            ", earthRadius=" + earthRadius +
            ", falseEasting=" + falseEasting +
            ", falseNorthing=" + falseNorthing +
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
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1)
            || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }

    double y1 = pt1.getY() - falseNorthing;
    double y2 = pt2.getY() - falseNorthing;

    // opposite signed long lines
    return (y1 * y2 < 0) && (Math.abs(y1 - y2) > 2 * earthRadius);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TransverseMercator that = (TransverseMercator) o;
    double tolerance = 1e-6;

    if (DoubleMath.fuzzyCompare(that.earthRadius,   earthRadius,   tolerance) != 0) return false;
    if (DoubleMath.fuzzyCompare(that.falseEasting,  falseEasting,  tolerance) != 0) return false;
    if (DoubleMath.fuzzyCompare(that.falseNorthing, falseNorthing, tolerance) != 0) return false;
    if (DoubleMath.fuzzyCompare(that.lat0,          lat0,          tolerance) != 0) return false;
    if (DoubleMath.fuzzyCompare(that.lon0,          lon0,          tolerance) != 0) return false;
    if (DoubleMath.fuzzyCompare(that.scale,         scale,         tolerance) != 0) return false;

    if ((defaultMapArea == null) != (that.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !that.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = scale != +0.0d ? Double.doubleToLongBits(scale) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /*MACROBODY
    projToLatLon {} {
    double x = (fromX-falseEasting)/scale;
    double d = (fromY-falseNorthing)/scale + lat0;
    toLon = Math.toDegrees(lon0 + Math.atan2(SpecialMathFunction.sinh(x), Math.cos(d)));
    toLat = Math.toDegrees(Math.asin( Math.sin(d)/ SpecialMathFunction.cosh(x)));
    }

    latLonToProj {} {
    double lon = Math.toRadians(fromLon);
    double lat = Math.toRadians(fromLat);
    double dlon = lon-lon0;
    double b = Math.cos( lat) * Math.sin(dlon);
    // infinite projection
    if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
        toX = 0.0; toY =  0.0;
    } else {
    toX = scale * SpecialMathFunction.atanh(b) + falseEasting;
    toY = scale * (Math.atan2(Math.tan(lat),Math.cos(dlon)) - lat0) + falseNorthing;
    }
}



  MACROBODY*/

  /*BEGINGENERATED*/

  /*
  Note this section has been generated using the convert.tcl script.
  This script, run as:
  tcl convert.tcl TransverseMercator.java
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
  public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                      ProjectionPointImpl result) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    double lon = Math.toRadians(fromLon);
    double lat = Math.toRadians(fromLat);
    double dlon = lon - lon0;
    double b = Math.cos(lat) * Math.sin(dlon);

    if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) { // infinite projection
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    } else {
      toX = scale * SpecialMathFunction.atanh(b);
      toY = scale * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0);
    }

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
  public LatLonPoint projToLatLon(ProjectionPoint world,
                                  LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = world.getX();
    double fromY = world.getY();

    double x = (fromX - falseEasting) / scale;
    double d = (fromY - falseNorthing) / scale + lat0;
    toLon = Math.toDegrees(lon0 + Math.atan2(Math.sinh(x), Math.cos(d)));
    toLat = Math.toDegrees(Math.asin(Math.sin(d) / Math.cosh(x)));

    result.setLatitude(toLat);
    result.setLongitude(toLon);
    return result;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n],
   *                 where from[0][i], from[1][i] is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates,
   *                 where to[0][i], to[1][i] is the (x,y) coordinate
   *                 of the ith point
   * @param latIndex index of latitude in "from"
   * @param lonIndex index of longitude in "from"
   * @return the "to" array.
   */
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                int lonIndex) {
    int cnt = from[0].length;
    float[] fromLatA = from[latIndex];
    float[] fromLonA = from[lonIndex];
    float[] resultXA = to[INDEX_X];
    float[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      double lon = Math.toRadians(fromLon);
      double lat = Math.toRadians(fromLat);
      double dlon = lon - lon0;
      double b = Math.cos(lat) * Math.sin(dlon);
      // infinite projection
      if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
        toX = 0.0;
        toY = 0.0;
      } else {
        toX = scale * SpecialMathFunction.atanh(b) + falseEasting;
        toY = scale * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0) + falseNorthing;
      }

      resultXA[i] = (float) toX;
      resultYA[i] = (float) toY;
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *             of the ith point
   * @param to   resulting array of projection coordinates: to[2][n]
   *             where (to[0][i], to[1][i]) is the (x,y) coordinate
   *             of the ith point
   * @return the "to" array
   */
  public float[][] projToLatLon(float[][] from, float[][] to) {
    int cnt = from[0].length;
    float[] fromXA = from[INDEX_X];
    float[] fromYA = from[INDEX_Y];
    float[] toLatA = to[INDEX_LAT];
    float[] toLonA = to[INDEX_LON];

    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i];
      double fromY = fromYA[i];

      double x = (fromX - falseEasting) / scale;
      double d = (fromY - falseNorthing) / scale + lat0;
      toLon = Math.toDegrees(lon0 + Math.atan2(Math.sinh(x), Math.cos(d)));
      toLat = Math.toDegrees(Math.asin(Math.sin(d) / Math.cosh(x)));

      toLatA[i] = (float) toLat;
      toLonA[i] = (float) toLon;
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n],
   *                 where from[0][i], from[1][i] is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates,
   *                 where to[0][i], to[1][i] is the (x,y) coordinate
   *                 of the ith point
   * @param latIndex index of latitude in "from"
   * @param lonIndex index of longitude in "from"
   * @return the "to" array.
   */
  public double[][] latLonToProj(double[][] from, double[][] to,
                                 int latIndex, int lonIndex) {
    int cnt = from[0].length;
    double[] fromLatA = from[latIndex];
    double[] fromLonA = from[lonIndex];
    double[] resultXA = to[INDEX_X];
    double[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      double lon = Math.toRadians(fromLon);
      double lat = Math.toRadians(fromLat);
      double dlon = lon - lon0;
      double b = Math.cos(lat) * Math.sin(dlon);
      // infinite projection
      if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
        toX = 0.0;
        toY = 0.0;
      } else {
        toX = scale * SpecialMathFunction.atanh(b) + falseEasting;
        toY = scale * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0) + falseNorthing;
      }

      resultXA[i] = (double) toX;
      resultYA[i] = (double) toY;
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *             of the ith point
   * @param to   resulting array of projection coordinates: to[2][n]
   *             where (to[0][i], to[1][i]) is the (x,y) coordinate
   *             of the ith point
   * @return the "to" array
   */
  public double[][] projToLatLon(double[][] from, double[][] to) {
    int cnt = from[0].length;
    double[] fromXA = from[INDEX_X];
    double[] fromYA = from[INDEX_Y];
    double[] toLatA = to[INDEX_LAT];
    double[] toLonA = to[INDEX_LON];

    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i];
      double fromY = fromYA[i];

      double x = (fromX - falseEasting) / scale;
      double d = (fromY - falseNorthing) / scale + lat0;
      toLon = Math.toDegrees(lon0 + Math.atan2(Math.sinh(x), Math.cos(d)));
      toLat = Math.toDegrees(Math.asin(Math.sin(d) / Math.cosh(x)));

      toLatA[i] = (double) toLat;
      toLonA[i] = (double) toLon;
    }
    return to;
  }

  /*ENDGENERATED*/


}


