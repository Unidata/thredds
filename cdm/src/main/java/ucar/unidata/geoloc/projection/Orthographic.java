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

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

/**
 * Orthographic Projection spherical earth.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 * 2nd edition (1983), p 145
 *
 * @author Unidata Development Team
 * @see Projection
 * @see ProjectionImpl
 */

public class Orthographic extends ProjectionImpl {

  private double lat0, lon0; // center lat/lon in radians
  private final double R; // radius

  /**
   * constants from Snyder's equations
   */
  private double lon0Degrees;
  private double cosLat0, sinLat0;


  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new Orthographic(getOriginLat(), getOriginLon(), R);
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters
   */
  public Orthographic() {
    this(0.0, 0.0);
  }

  /**
   * Construct a Orthographic Projection.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @throws IllegalArgumentException
   */
  public Orthographic(double lat0, double lon0) {
    this(lat0, lon0, EARTH_RADIUS);
  }

  /**
   * Construct a Orthographic Projection, specify earth radius
   *
   * @param lat0        lat origin of the coord. system on the projection plane
   * @param lon0        lon origin of the coord. system on the projection plane
   * @param earthRadius radius of the earth
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public Orthographic(double lat0, double lon0, double earthRadius) {
    super("Orthographic", false);

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);
    R = earthRadius;

    precalculate();

    addParameter(CF.GRID_MAPPING_NAME, CF.ORTHOGRAPHIC);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
  }

  // move this to ucar.unit or ucar.unidata.util

  /**
   * Precalculate some stuff
   */
  private void precalculate() {
    sinLat0 = Math.sin(lat0);
    cosLat0 = Math.cos(lat0);
    lon0Degrees = Math.toDegrees(lon0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Orthographic that = (Orthographic) o;

    if (Double.compare(that.R, R) != 0) return false;
    if (Double.compare(that.lat0, lat0) != 0) return false;
    if (Double.compare(that.lon0, lon0) != 0) return false;
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
    temp = R != +0.0d ? Double.doubleToLongBits(R) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

// bean properties

  /**
   * Get the origin longitude in degrees
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
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

  //////////////////////////////////////////////
  // setters for IDV serialization - do not use except for object creating

  /**
   * Set the origin longitude.
   * @param lon   the origin longitude.
   */
  public void setOriginLon(double lon) {
      lon0 = Math.toRadians(lon);
      precalculate();
  }

  /**
   * Set the origin latitude.
   *
   * @param lat   the origin latitude.
   */
  public void setOriginLat(double lat) {
      lat0 = Math.toRadians(lat);
      precalculate();
  }

  //////////////////////////////////////////////

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Orthographic";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "Orthographic{" +
            "lat0=" + lat0 +
            ", lon0=" + lon0 +
            ", R=" + R +
            '}';
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2))
      return true;

    // opposite signed X values, larger then 5000 km
    return (pt1.getX() * pt2.getX() < 0) && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
  }


  /*MACROBODY
    latLonToProj {} {
      fromLat = Math.toRadians(fromLat);
      double lonDiff =
          Math.toRadians(LatLonPointImpl.lonNormal(fromLon-lon0Degrees));
      double cosc = sinLat0*Math.sin(fromLat) + cosLat0*Math.cos(fromLat)*Math.cos(lonDiff);
      if (cosc >= 0) {
          toX = R*Math.cos(fromLat)*Math.sin(lonDiff);
          toY = R*(cosLat0*Math.sin(fromLat) - sinLat0*Math.cos(fromLat)*Math.cos(lonDiff));
      } else {
          toX = Double.POSITIVE_INFINITY;
          toY = Double.POSITIVE_INFINITY;
      }
    }

    projToLatLon {} {

      fromX = fromX;
      fromY = fromY;
      double rho = Math.sqrt(fromX*fromX + fromY*fromY);
      double c = Math.asin(rho/R);

      toLon = lon0;
      double temp = 0;
      if (Math.abs(rho) > TOLERANCE) {
        toLat = Math.asin(Math.cos(c)*sinLat0 + (fromY*Math.sin(c)*cosLat0/rho));
        if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) { // not 90 or -90
          temp = rho*cosLat0*Math.cos(c) - fromY*sinLat0*Math.sin(c);
          toLon = lon0 + Math.atan(fromX*Math.sin(c)/temp);
        } else if (lat0 == PI_OVER_4) {
          toLon = lon0 + Math.atan(fromX/-fromY);
          temp = -fromY;
        } else {
          toLon = lon0 + Math.atan(fromX/fromY);
          temp = fromY;
        }
      } else {
        toLat = lat0;
      }
      toLat= Math.toDegrees(toLat);
      toLon= Math.toDegrees(toLon);
      if (temp < 0) toLon += 180;
      toLon= LatLonPointImpl.lonNormal(toLon);
    }


  MACROBODY*/

  /*BEGINGENERATED*/

  /*
  Note this section has been generated using the convert.tcl script.
  This script, run as:
  tcl convert.tcl Orthographic.java
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


    fromLat = Math.toRadians(fromLat);
    double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon - lon0Degrees));
    double cosc = sinLat0 * Math.sin(fromLat) + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
    if (cosc >= 0) {
      toX = R * Math.cos(fromLat) * Math.sin(lonDiff);
      toY = R * (cosLat0 * Math.sin(fromLat) - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
    } else {
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    }

    result.setLocation(toX, toY);
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
    double fromX = world.getX();
    double fromY = world.getY();

    double rho = Math.sqrt(fromX * fromX + fromY * fromY);
    double c = Math.asin(rho / R);

    toLon = lon0;
    double temp = 0;
    if (Math.abs(rho) > TOLERANCE) {
      toLat = Math.asin(Math.cos(c) * sinLat0
              + (fromY * Math.sin(c) * cosLat0 / rho));
      if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
        temp = rho * cosLat0 * Math.cos(c)
                - fromY * sinLat0 * Math.sin(c);
        toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
      } else if (Double.compare(lat0, PI_OVER_4) == 0) {
        toLon = lon0 + Math.atan(fromX / -fromY);
        temp = -fromY;
      } else {
        toLon = lon0 + Math.atan(fromX / fromY);
        temp = fromY;
      }
    } else {
      toLat = lat0;
    }
    toLat = Math.toDegrees(toLat);
    toLon = Math.toDegrees(toLon);
    if (temp < 0) {
      toLon += 180;
    }
    toLon = LatLonPointImpl.lonNormal(toLon);

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

      fromLat = Math.toRadians(fromLat);
      double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon
              - lon0Degrees));
      double cosc = sinLat0 * Math.sin(fromLat)
              + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
      if (cosc >= 0) {
        toX = R * Math.cos(fromLat) * Math.sin(lonDiff);
        toY = R * (cosLat0 * Math.sin(fromLat)
                - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
      } else {
        toX = Double.POSITIVE_INFINITY;
        toY = Double.POSITIVE_INFINITY;
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

      double rho = Math.sqrt(fromX * fromX + fromY * fromY);
      double c = Math.asin(rho / R);

      toLon = lon0;
      double temp = 0;
      if (Math.abs(rho) > TOLERANCE) {
        toLat = Math.asin(Math.cos(c) * sinLat0
                + (fromY * Math.sin(c) * cosLat0 / rho));
        if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
          temp = rho * cosLat0 * Math.cos(c)
                  - fromY * sinLat0 * Math.sin(c);
          toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
        } else if (Double.compare(lat0, PI_OVER_4) == 0) {
          toLon = lon0 + Math.atan(fromX / -fromY);
          temp = -fromY;
        } else {
          toLon = lon0 + Math.atan(fromX / fromY);
          temp = fromY;
        }
      } else {
        toLat = lat0;
      }
      toLat = Math.toDegrees(toLat);
      toLon = Math.toDegrees(toLon);
      if (temp < 0) {
        toLon += 180;
      }
      toLon = LatLonPointImpl.lonNormal(toLon);

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

      fromLat = Math.toRadians(fromLat);
      double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon
              - lon0Degrees));
      double cosc = sinLat0 * Math.sin(fromLat)
              + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
      if (cosc >= 0) {
        toX = R * Math.cos(fromLat) * Math.sin(lonDiff);
        toY = R * (cosLat0 * Math.sin(fromLat)
                - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
      } else {
        toX = Double.POSITIVE_INFINITY;
        toY = Double.POSITIVE_INFINITY;
      }

      resultXA[i] = toX;
      resultYA[i] = toY;
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

      double rho = Math.sqrt(fromX * fromX + fromY * fromY);
      double c = Math.asin(rho / R);

      toLon = lon0;
      double temp = 0;
      if (Math.abs(rho) > TOLERANCE) {
        toLat = Math.asin(Math.cos(c) * sinLat0
                + (fromY * Math.sin(c) * cosLat0 / rho));
        if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
          temp = rho * cosLat0 * Math.cos(c)
                  - fromY * sinLat0 * Math.sin(c);
          toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
        } else if (Double.compare(lat0, PI_OVER_4) == 0) {
          toLon = lon0 + Math.atan(fromX / -fromY);
          temp = -fromY;
        } else {
          toLon = lon0 + Math.atan(fromX / fromY);
          temp = fromY;
        }
      } else {
        toLat = lat0;
      }
      toLat = Math.toDegrees(toLat);
      toLon = Math.toDegrees(toLon);
      if (temp < 0) {
        toLon += 180;
      }
      toLon = LatLonPointImpl.lonNormal(toLon);

      toLatA[i] = toLat;
      toLonA[i] = toLon;
    }
    return to;
  }

  /*ENDGENERATED*/

  /**
   * Test
   *
   * @param args not used
   */
  public static void main(String[] args) {
    Orthographic a = new Orthographic(40, -100);
    ProjectionPoint p = a.latLonToProj(30, -110);
    System.out.println("proj point = " + p);
    LatLonPoint ll = a.projToLatLon(p);
    System.out.println("ll = " + ll);
  }
}

