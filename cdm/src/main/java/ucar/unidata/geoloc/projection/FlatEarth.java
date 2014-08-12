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
 * FlatEarth Projection
 * This projection surface is tangent at some point (lat0, lon0) and
 * has a y axis rotated from true North by some angle.
 * <p/>
 * We call it "flat" because it should only be used where the spherical
 * geometry of the earth is not significant. In actuallity, we use the simple
 * "arclen" routine which computes dy along a meridian, and dx along a
 * latitude circle.  We rotate the coordinate system to/from a true north system.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 * 2nd edition (1983), p 145
 *
 * @author Unidata Development Team
 * @see Projection
 * @see ProjectionImpl
 */

public class FlatEarth extends ProjectionImpl {
  public static final String ROTATIONANGLE = "rotationAngle";

  /**
   * constants from Snyder's equations
   */
  private final double rotAngle, radius;
  private final double lat0, lon0; // center lat/lon in radians

  /**
   * some constants
   */
  private double cosRot, sinRot;

  /**
   * origin
   */
  //private LatLonPointImpl origin;  // why are we keeping this?

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = new FlatEarth(getOriginLat(), getOriginLon(), getRotationAngle());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters
   */
  public FlatEarth() {
    this(0.0, 0.0, 0.0, EARTH_RADIUS);
  }

   public FlatEarth(double lat0, double lon0) {
     this(lat0, lon0, 0.0, EARTH_RADIUS);
   }

  public FlatEarth(double lat0, double lon0, double rotAngle) {
    this(lat0, lon0, rotAngle, EARTH_RADIUS);
  }

  /**
   * Construct a FlatEarth Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0     lat origin of the coord. system on the projection plane
   * @param lon0     lon origin of the coord. system on the projection plane
   * @param rotAngle angle of rotation, in degrees
   * @param radius  earth radius in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public FlatEarth(double lat0, double lon0, double rotAngle, double radius) {
    super("FlatEarth", false);

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);
    this.rotAngle = Math.toRadians(rotAngle);
    this.radius = radius;

    precalculate();

    addParameter(CF.GRID_MAPPING_NAME, "flat_earth");
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(ROTATIONANGLE, rotAngle);
    addParameter(CF.EARTH_RADIUS, radius * 1000);
  }

  /**
   * Precalculate some stuff
   */
  private void precalculate() {
    sinRot = Math.sin(rotAngle);
    cosRot = Math.cos(rotAngle);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FlatEarth flatEarth = (FlatEarth) o;

    if (Double.compare(flatEarth.lat0, lat0) != 0) return false;
    if (Double.compare(flatEarth.lon0, lon0) != 0) return false;
    if (Double.compare(flatEarth.radius, radius) != 0) return false;
    if (Double.compare(flatEarth.rotAngle, rotAngle) != 0) return false;
    if ((defaultMapArea == null) != (flatEarth.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !flatEarth.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = rotAngle != +0.0d ? Double.doubleToLongBits(rotAngle) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = radius != +0.0d ? Double.doubleToLongBits(radius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

// bean properties

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

  /**
   * Get the rotation angle.
   *
   * @return the origin latitude.
   */
  public double getRotationAngle() {
    return rotAngle;
  }

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "FlatEarth";
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
    return "FlatEarth{" +
            "rotAngle=" + rotAngle +
            ", radius=" + radius +
            ", lat0=" + lat0 +
            ", lon0=" + lon0 +
            '}';
  }

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
    double dx, dy;

    fromLat = Math.toRadians(fromLat);

    dy = radius * (fromLat - lat0);
    dx = radius * Math.cos(fromLat)
            * (Math.toRadians(fromLon) - lon0);


    toX = cosRot * dx - sinRot * dy;
    toY = sinRot * dx + cosRot * dy;


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

  public LatLonPoint projToLatLon(ProjectionPoint world,
                                  LatLonPointImpl result) {
    double toLat, toLon;
    double x = world.getX();
    double y = world.getY();
    double cosl;
    int TOLERENCE = 1;
    double xp, yp;

    xp = cosRot * x + sinRot * y;
    yp = -sinRot * x + cosRot * y;

    toLat = Math.toDegrees(lat0) + Math.toDegrees(yp / radius);
    //double lat2;
    //lat2 = lat0 + Math.toDegrees(yp/radius);
    cosl = Math.cos(Math.toRadians(toLat));
    if (Math.abs(cosl) < TOLERANCE) {
      toLon = Math.toDegrees(lon0);
    } else {
      toLon = Math.toDegrees(lon0)
              + Math.toDegrees(xp / cosl / radius);
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
      double dy = radius * (fromLat - lat0);
      double dx = radius * Math.cos(fromLat)
              * (Math.toRadians(fromLon) - lon0);


      toX = cosRot * dx - sinRot * dy;
      toY = sinRot * dx + cosRot * dy;

      resultXA[i] = (float) toX;
      resultYA[i] = (float) toY;
    }
    return to;
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

    return (pt1.getX() * pt2.getX() < 0);
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

      double xp = cosRot * fromX + sinRot * fromY;
      double yp = -sinRot * fromX + cosRot * fromY;


      toLat = Math.toDegrees(lat0)
              + Math.toDegrees(yp / radius);
      double cosl = Math.cos(Math.toRadians(toLat));

      if (Math.abs(cosl) < TOLERANCE) {
        toLon = Math.toDegrees(lon0);
      } else {
        toLon = Math.toDegrees(lon0)
                + Math.toDegrees(xp / cosl / radius);
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
      double dy = radius * (fromLat - lat0);
      double dx = radius * Math.cos(fromLat)
              * (Math.toRadians(fromLon) - lon0);

      toX = cosRot * dx - sinRot * dy;
      toY = sinRot * dx + cosRot * dy;


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

      double xp = cosRot * fromX + sinRot * fromY;
      double yp = -sinRot * fromX + cosRot * fromY;

      //toLat =  lat0 + Math.toDegrees(yp);
      toLat = Math.toDegrees(lat0)
              + Math.toDegrees(yp / radius);
      double cosl = Math.cos(Math.toRadians(toLat));

      if (Math.abs(cosl) < TOLERANCE) {
        toLon = Math.toDegrees(lon0);
      } else {
        toLon = Math.toDegrees(lon0)
                + Math.toDegrees(xp / cosl / radius);
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
    FlatEarth a = new FlatEarth(90, -100, 0.0);
    ProjectionPoint p = a.latLonToProj(89, -101);
    System.out.println("proj point = " + p);
    LatLonPoint ll = a.projToLatLon(p);
    System.out.println("ll = " + ll);
  }

}

