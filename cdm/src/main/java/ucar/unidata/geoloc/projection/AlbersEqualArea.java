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
import ucar.unidata.util.Parameter;

/**
 * Albers Equal Area Conic Projection, one or two standard parallels,
 * spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 * 2nd edition (1983), p 98
 *
 * @author Unidata Development Team
 * @see Projection
 * @see ProjectionImpl
 */

public class AlbersEqualArea extends ProjectionImpl {

  private double lat0, lon0;  // radians
  private double par1, par2;  // degrees
  private double falseEasting, falseNorthing;
  private final double earth_radius; // radius in km

  /**
   * constants from Snyder's equations
   */
  private double n, C, rho0, lon0Degrees;


  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = new AlbersEqualArea(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
            getFalseEasting(), getFalseNorthing(), getEarthRadius());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters
   */
  public AlbersEqualArea() {
    this(23, -96, 29.5, 45.5);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2) {
    this(lat0, lon0, par1, par2, 0, 0, Earth.getRadius()*.001);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0          lat origin of the coord. system on the projection plane
   * @param lon0          lon origin of the coord. system on the projection plane
   * @param par1          standard parallel 1
   * @param par2          standard parallel 2
   * @param falseEasting  false easting in km
   * @param falseNorthing false easting in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2, double falseEasting, double falseNorthing) {
    this(lat0, lon0, par1, par2, falseEasting, falseNorthing, Earth.getRadius()*.001);
  }

  /**
   * Construct a AlbersEqualArea Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0          lat origin of the coord. system on the projection plane
   * @param lon0          lon origin of the coord. system on the projection plane
   * @param par1          standard parallel 1
   * @param par2          standard parallel 2
   * @param falseEasting  false easting in km
   * @param falseNorthing false easting in km
   * @param earth_radius  radius of the earth in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public AlbersEqualArea(double lat0, double lon0, double par1, double par2, double falseEasting, double falseNorthing, double earth_radius) {
    super("AlbersEqualArea", false);
    
    this.lat0 = Math.toRadians(lat0);
    lon0Degrees = lon0;
    this.lon0 = Math.toRadians(lon0);

    this.par1 = par1;
    this.par2 = par2;

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;
    this.earth_radius = earth_radius;

    precalculate();

    addParameter(CF.GRID_MAPPING_NAME, CF.ALBERS_CONICAL_EQUAL_AREA);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);

    if (par2 == par1) {
      addParameter(CF.STANDARD_PARALLEL, par1);
    } else {
      double[] data = new double[2];
      data[0] = par1;
      data[1] = par2;
      addParameter(new Parameter(CF.STANDARD_PARALLEL, data));
    }

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.EARTH_RADIUS, earth_radius * 1000); // must be in meters
  }

  /**
   * Precalculate some stuff
   */
  private void precalculate() {

    double par1r = Math.toRadians(this.par1);
    double par2r = Math.toRadians(this.par2);

    if (Math.abs(par2 - par1) < TOLERANCE) {  // single parallel
      n = Math.sin(par1r);
    } else {
      n = (Math.sin(par1r) + Math.sin(par2r)) / 2.0;
    }

    double c2 = Math.pow(Math.cos(par1r), 2);
    C = c2 + 2 * n * Math.sin(par1r);
    rho0 = computeRho(lat0);

  }

  /**
   * Compute the RHO parameter
   *
   * @param lat latitude
   * @return the rho parameter
   */
  private double computeRho(double lat) {
    return earth_radius * Math.sqrt(C - 2 * n * Math.sin(lat)) / n;
  }

  /**
   * Compute theta
   *
   * @param lon longitude
   * @return theta
   */
  private double computeTheta(double lon) {
    double dlon = LatLonPointImpl.lonNormal(Math.toDegrees(lon) - lon0Degrees);
    return n * Math.toRadians(dlon);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AlbersEqualArea that = (AlbersEqualArea) o;

    if (Double.compare(that.earth_radius, earth_radius) != 0) return false;
    if (Double.compare(that.falseEasting, falseEasting) != 0) return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0) return false;
    if (Double.compare(that.lat0, lat0) != 0) return false;
    if (Double.compare(that.lon0, lon0) != 0) return false;
    if (Double.compare(that.par1, par1) != 0) return false;
    if (Double.compare(that.par2, par2) != 0) return false;
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
    temp = par1 != +0.0d ? Double.doubleToLongBits(par1) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = par2 != +0.0d ? Double.doubleToLongBits(par2) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earth_radius != +0.0d ? Double.doubleToLongBits(earth_radius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  // bean properties

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel
   */
  public double getParallelTwo() {
    return par2;
  }

    /**
   * Get the first standard parallel
   *
   * @return the first standard parallel
   */
  public double getParallelOne() {
    return par1;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
    return Math.toDegrees(lon0);
  }

  /**
   * Get the origin latitude.
   *
   * @return the origin latitude.
   */
  public double getOriginLat() {
    return Math.toDegrees(lat0);
  }

  //////////////////////////////////////////////
  // setters for IDV serialization - do not use except for object creating


  /**
     * Set the second standard parallel
     *
     * @param par   the second standard parallel
     */
    public void setParallelTwo(double par) {
        par2 = par;
        precalculate();
    }

  /**
   * Set the first standard parallel
   *
   * @param par   the first standard parallel
   */
  public void setParallelOne(double par) {
      par1 = par;
      precalculate();
  }

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

    //////////////////////////////////////////////


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

  /**
   * Earth radius in km
   * @return Earth radius in km
   */
  public double getEarthRadius() {
    return this.earth_radius;
  }

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Albers Equal Area";
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
    return "AlbersEqualArea{" +
            "lat0=" + lat0 +
            ", lon0=" + lon0 +
            ", par1=" + par1 +
            ", par2=" + par2 +
            ", falseEasting=" + falseEasting +
            ", falseNorthing=" + falseNorthing +
            ", earth_radius=" + earth_radius +
            '}';
  }

  /**
   * Get the scale at the given lat.
   *
   * @param lat lat to use
   * @return scale factor at that latitude
   */
  public double getScale(double lat) {
    lat = Math.toRadians(lat);
    double n = Math.cos(lat);
    double d = Math.sqrt(C - 2 * n * Math.sin(lat));
    return n / d;
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
    if (ProjectionPointImpl.isInfinite(pt1)
            || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }
    // opposite signed X values, larger then 5000 km
    return (pt1.getX() * pt2.getX() < 0)
            && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
  }

  /*MACROBODY
    latLonToProj {} {
      fromLat = Math.toRadians(fromLat);
      fromLon = Math.toRadians(fromLon);
      double rho = computeRho(fromLat);
      double theta = computeTheta(fromLon);

      toX = rho * Math.sin(theta);
      toY = rho0 - rho*Math.cos(theta);
    }
    projToLatLon {double rrho0 = rho0;} {
      if (n < 0) {
          rrho0 *= -1.0;
          fromX *= -1.0;
          fromY *= -1.0;
      }


      double yd = rrho0-fromY;
      double rho = Math.sqrt(fromX * fromX + yd*yd);
      double theta = Math.atan2( fromX, yd);
      if (n < 0) rho *= -1.0;

      toLat = Math.toDegrees(Math.asin((C-Math.pow((rho*n/EARTH_RADIUS),2))/(2*n)));

      toLon = Math.toDegrees(theta/n + lon0);

           }
  MACROBODY*/

  /*BEGINGENERATED*/

  /*
  Note this section has been generated using the convert.tcl script.
  This script, run as:
  tcl convert.tcl AlbersEqualArea.java
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

    fromLat = Math.toRadians(fromLat);
    fromLon = Math.toRadians(fromLon);
    double rho = computeRho(fromLat);
    double theta = computeTheta(fromLon);

    toX = rho * Math.sin(theta) + falseEasting;
    toY = rho0 - rho * Math.cos(theta) + falseNorthing;

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
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double rrho0 = rho0;

    if (n < 0) {
      rrho0 *= -1.0;
      fromX *= -1.0;
      fromY *= -1.0;
    }

    double yd = rrho0 - fromY;
    double rho = Math.sqrt(fromX * fromX + yd * yd);
    double theta = Math.atan2(fromX, yd);
    if (n < 0) {
      rho *= -1.0;
    }
    toLat = Math.toDegrees(Math.asin((C - Math.pow((rho * n / earth_radius), 2)) / (2 * n)));

    toLon = Math.toDegrees(theta / n + lon0);

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
      fromLon = Math.toRadians(fromLon);
      double rho = computeRho(fromLat);
      double theta = computeTheta(fromLon);

      toX = rho * Math.sin(theta);
      toY = rho0 - rho * Math.cos(theta);

      resultXA[i] = (float) (toX + falseEasting);
      resultYA[i] = (float) (toY + falseNorthing);
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
    double rrho0 = rho0;
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;

      if (n < 0) {
        rrho0 *= -1.0;
        fromX *= -1.0;
        fromY *= -1.0;
      }


      double yd = rrho0 - fromY;
      double rho = Math.sqrt(fromX * fromX + yd * yd);
      double theta = Math.atan2(fromX, yd);
      if (n < 0) {
        rho *= -1.0;
      }

      toLat = Math.toDegrees(Math.asin((C
              - Math.pow((rho * n / earth_radius), 2)) / (2 * n)));

      toLon = Math.toDegrees(theta / n + lon0);


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
      fromLon = Math.toRadians(fromLon);
      double rho = computeRho(fromLat);
      double theta = computeTheta(fromLon);

      toX = rho * Math.sin(theta);
      toY = rho0 - rho * Math.cos(theta);

      resultXA[i] = toX + falseEasting;
      resultYA[i] = toY + falseNorthing;
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
    double rrho0 = rho0;
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;

      if (n < 0) {
        rrho0 *= -1.0;
        fromX *= -1.0;
        fromY *= -1.0;
      }


      double yd = rrho0 - fromY;
      double rho = Math.sqrt(fromX * fromX + yd * yd);
      double theta = Math.atan2(fromX, yd);
      if (n < 0) {
        rho *= -1.0;
      }

      toLat = Math.toDegrees(Math.asin((C
              - Math.pow((rho * n / earth_radius), 2)) / (2 * n)));

      toLon = Math.toDegrees(theta / n + lon0);


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
    AlbersEqualArea a = new AlbersEqualArea(23, -96, 29.5, 45.5);
    System.out.printf("name=%s%n", a.getName());
    System.out.println("ll = 35N 75W");
    ProjectionPoint p = a.latLonToProj(35, -75);
    System.out.println("proj point = " + p);
    LatLonPoint ll = a.projToLatLon(p);
    System.out.println("ll = " + ll);
  }

}

