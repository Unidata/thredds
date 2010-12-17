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

import ucar.unidata.geoloc.*;

import java.util.List;
import java.util.ArrayList;

/**
 *  Vertical Perspective Projection, spherical earth.
 *  <p>
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 *  2nd edition (1983), p 176
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author Unidata Development Team
 */

public class VerticalPerspectiveView extends ProjectionImpl {


    /** constants from Snyder's equations */
    private double P, H, R, lon0Degrees;

    /** center lat/lon in radians */
    private double lat0, lon0;

    /** some constants */
    private double cosLat0, sinLat0;

    /** some constants */
    private double false_east, false_north;

    /** square of "map limit" circle of this radius from the origin, p 173 */
    private double maxR, maxR2;

    /** origin */
    private LatLonPointImpl origin;  // why are we keeping this?

    /** spherical vs ellipsoidal */
    private boolean spherical = true;

          /** copy constructor - avoid clone !! */
    public ProjectionImpl constructCopy() {
      return new VerticalPerspectiveView( getOriginLat(), getOriginLon(), R, getHeight(), false_east, false_north);
    }

    /**
     *  Constructor with default parameters
     */
    public VerticalPerspectiveView() {
        this(0.0, 0.0, EARTH_RADIUS, 35800);
    }

    /**
     * Construct a VerticalPerspectiveView Projection.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     */
    public VerticalPerspectiveView(double lat0, double lon0) {
        this(lat0, lon0, EARTH_RADIUS, 35800);
    }


    /**
     * Construct a VerticalPerspectiveView Projection
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param earthRadius   radius of the earth (km)
     * @param distance  height above the earth (km)
     */
    public VerticalPerspectiveView(double lat0, double lon0,
                                   double earthRadius, double distance) {
        this(lat0, lon0, earthRadius, distance, 0, 0);
    }

    /**
     * Construct a VerticalPerspectiveView Projection
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param earthRadius   radius of the earth (km)
     * @param distance  height above the earth  (km)
     * @param false_easting  easting offset (km)
     * @param false_northing  northing offset (km)
     */
    public VerticalPerspectiveView(double lat0, double lon0,
                                   double earthRadius, double distance,
                                   double false_easting,
                                   double false_northing) {

        this.lat0   = Math.toRadians(lat0);
        this.lon0   = Math.toRadians(lon0);
        R           = earthRadius;
        H           = distance;
        false_east  = false_easting;
        false_north = false_northing;

        origin      = new LatLonPointImpl(lat0, lon0);
        precalculate();

        addParameter(ATTR_NAME, "vertical_perspective");
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("longitude_of_projection_origin", lon0);
        addParameter("height_above_earth", distance);
        addParameter("false_easting", false_easting);
        addParameter("false_northing", false_northing);
        addParameter("earth_radius", earthRadius);
    }

    // move this to ucar.unit or ucar.unidata.util

    /**
     * Precalculate some stuff
     */
    private void precalculate() {
        sinLat0     = Math.sin(lat0);
        cosLat0     = Math.cos(lat0);
        lon0Degrees = Math.toDegrees(lon0);
        P           = 1.0 + H / R;

        // "map limit" circle of this radius from the origin, p 173
        maxR = .99 * R * Math.sqrt((P-1)/(P+1));
        maxR2 = maxR * maxR;
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        VerticalPerspectiveView cl = (VerticalPerspectiveView) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
        return cl;
    }


    /**
     * Check for equality with the Object in question
     *
     * @param proj  object to check
     * @return true if they are equal
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof VerticalPerspectiveView)) {
            return false;
        }

        VerticalPerspectiveView oo = (VerticalPerspectiveView) proj;
        return (this.getOriginLat() == oo.getOriginLat()) &&
               (this.getOriginLon() == oo.getOriginLon()) &&
               (this.getHeight() == oo.getHeight()) &&
               (this.defaultMapArea.equals(oo.defaultMapArea));
    }


    // bean properties

    /**
     * Get the origin longitude.
     * @return the origin longitude.
     */
    public double getOriginLon() {
        return origin.getLongitude();
    }

    /**
     * Set the origin longitude.
     * @param lon   the origin longitude.
     */
    public void setOriginLon(double lon) {
        origin.setLongitude(lon);
        lon0 = Math.toRadians(lon);
        precalculate();
    }

    /**
     * Get the height above the earth
     * @return the height above the earth
     */
    public double getHeight() {
        return H;
    }

    /**
     * Set the height above the earth
     * @param height   height above the earth
     */
    public void setHeight(double height) {
        H = height;
        precalculate();
    }

    /**
     * Get the origin latitude.
     * @return the origin latitude.
     */
    public double getOriginLat() {
        return origin.getLatitude();
    }

    /**
     * Set the origin latitude.
     *
     * @param lat   the origin latitude.
     */
    public void setOriginLat(double lat) {
        origin.setLatitude(lat);
        lat0 = Math.toRadians(lat);
        precalculate();
    }

    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "VerticalPerspectiveView";
    }

    /**
     * Create a String of the parameters.
     * @return a String of the parameters
     */
    public String paramsToString() {
        return " origin " + origin.toString();
    }


    /**
     *  This returns true when the line between pt1 and pt2 crosses the seam.
     *  When the cone is flattened, the "seam" is lon0 +- 180.
     *
     * @param pt1   point 1
     * @param pt2   point 2
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
        double lonDiff =
            Math.toRadians(LatLonPointImpl.lonNormal(fromLon-lon0Degrees));
        double cosc = sinLat0*Math.sin(fromLat) + cosLat0*Math.cos(fromLat)*Math.cos(lonDiff);
        double ksp = (P-1.0)/(P-cosc);
        if (cosc < 1.0/P) {
            toX = Double.POSITIVE_INFINITY;
            toY = Double.POSITIVE_INFINITY;
        } else {
            toX = false_east + R*ksp*Math.cos(fromLat)*Math.sin(lonDiff);
            toY = false_north + R*ksp*(cosLat0*Math.sin(fromLat) - sinLat0*Math.cos(fromLat)*Math.cos(lonDiff));
        }
      }

      projToLatLon {} {

        fromX = fromX - false_east;
        fromY = fromY - false_north;
        double rho = Math.sqrt(fromX*fromX + fromY*fromY);
        double r = rho /R;
        double con = P - 1.0;
        double com = P + 1.0;
        double c = Math.asin((P - Math.sqrt(1.0 - (r * r * com) / con)) / (con / r + r/con));

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
    tcl convert.tcl VerticalPerspectiveView.java
    takes the actual projection conversion code defined in the MACROBODY
    section above and generates the following 6 methods
    */


    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude();
        double fromLon = latLon.getLongitude();


        fromLat = Math.toRadians(fromLat);
        double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon
                             - lon0Degrees));
        double cosc = sinLat0 * Math.sin(fromLat)
                      + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
        double ksp = (P - 1.0) / (P - cosc);
        if (cosc < 1.0 / P) {
            toX = Double.POSITIVE_INFINITY;
            toY = Double.POSITIVE_INFINITY;
        } else {
            toX = false_east
                  + R * ksp * Math.cos(fromLat) * Math.sin(lonDiff);
            toY = false_north
                  + R * ksp
                    * (cosLat0 * Math.sin(fromLat)
                       - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
        }

        result.setLocation(toX, toY);
        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double toLat, toLon;
        double fromX = world.getX();
        double fromY = world.getY();



        fromX = fromX - false_east;
        fromY = fromY - false_north;
        double rho = Math.sqrt(fromX * fromX + fromY * fromY);
        double r   = rho / R;
        double con = P - 1.0;
        double com = P + 1.0;
        double c = Math.asin((P - Math.sqrt(1.0 - (r * r * com) / con))
                             / (con / r + r / con));

        toLon = lon0;
        double temp = 0;
        if (Math.abs(rho) > TOLERANCE) {
            toLat = Math.asin(Math.cos(c) * sinLat0
                              + (fromY * Math.sin(c) * cosLat0 / rho));
            if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
                temp = rho * cosLat0 * Math.cos(c)
                       - fromY * sinLat0 * Math.sin(c);
                toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
            } else if (lat0 == PI_OVER_4) {
                toLon = lon0 + Math.atan(fromX / -fromY);
                temp  = -fromY;
            } else {
                toLon = lon0 + Math.atan(fromX / fromY);
                temp  = fromY;
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
     *
     * @return the "to" array.
     */
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        int     cnt      = from[0].length;
        float[] fromLatA = from[latIndex];
        float[] fromLonA = from[lonIndex];
        float[] resultXA = to[INDEX_X];
        float[] resultYA = to[INDEX_Y];
        double  toX, toY;

        for (int i = 0; i < cnt; i++) {
            double fromLat = fromLatA[i];
            double fromLon = fromLonA[i];

            fromLat = Math.toRadians(fromLat);
            double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon
                                 - lon0Degrees));
            double cosc = sinLat0 * Math.sin(fromLat)
                          + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
            double ksp = (P - 1.0) / (P - cosc);
            if (cosc < 1.0 / P) {
                toX = Double.POSITIVE_INFINITY;
                toY = Double.POSITIVE_INFINITY;
            } else {
                toX = false_east
                      + R * ksp * Math.cos(fromLat) * Math.sin(lonDiff);
                toY = false_north
                      + R * ksp
                        * (cosLat0 * Math.sin(fromLat)
                           - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
            }

            resultXA[i] = (float) toX;
            resultYA[i] = (float) toY;
        }
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {
        int     cnt    = from[0].length;
        float[] fromXA = from[INDEX_X];
        float[] fromYA = from[INDEX_Y];
        float[] toLatA = to[INDEX_LAT];
        float[] toLonA = to[INDEX_LON];

        double  toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];


            fromX = fromX - false_east;
            fromY = fromY - false_north;
            double rho = Math.sqrt(fromX * fromX + fromY * fromY);
            double r   = rho / R;
            double con = P - 1.0;
            double com = P + 1.0;
            double c = Math.asin((P - Math.sqrt(1.0 - (r * r * com) / con))
                                 / (con / r + r / con));

            toLon = lon0;
            double temp = 0;
            if (Math.abs(rho) > TOLERANCE) {
                toLat = Math.asin(Math.cos(c) * sinLat0
                                  + (fromY * Math.sin(c) * cosLat0 / rho));
                if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
                    temp = rho * cosLat0 * Math.cos(c)
                           - fromY * sinLat0 * Math.sin(c);
                    toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
                } else if (lat0 == PI_OVER_4) {
                    toLon = lon0 + Math.atan(fromX / -fromY);
                    temp  = -fromY;
                } else {
                    toLon = lon0 + Math.atan(fromX / fromY);
                    temp  = fromY;
                }
            } else {
                toLat = lat0;
            }
            toLat = Math.toDegrees(toLat);
            toLon = Math.toDegrees(toLon);
            if (temp < 0) {
                toLon += 180;
            }
            toLon     = LatLonPointImpl.lonNormal(toLon);

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
     *
     * @return the "to" array.
     */
    public double[][] latLonToProj(double[][] from, double[][] to,
                                   int latIndex, int lonIndex) {
        int      cnt      = from[0].length;
        double[] fromLatA = from[latIndex];
        double[] fromLonA = from[lonIndex];
        double[] resultXA = to[INDEX_X];
        double[] resultYA = to[INDEX_Y];
        double   toX, toY;

        for (int i = 0; i < cnt; i++) {
            double fromLat = fromLatA[i];
            double fromLon = fromLonA[i];

            fromLat = Math.toRadians(fromLat);
            double lonDiff = Math.toRadians(LatLonPointImpl.lonNormal(fromLon
                                 - lon0Degrees));
            double cosc = sinLat0 * Math.sin(fromLat)
                          + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);
            double ksp = (P - 1.0) / (P - cosc);
            if (cosc < 1.0 / P) {
                toX = Double.POSITIVE_INFINITY;
                toY = Double.POSITIVE_INFINITY;
            } else {
                toX = false_east
                      + R * ksp * Math.cos(fromLat) * Math.sin(lonDiff);
                toY = false_north
                      + R * ksp
                        * (cosLat0 * Math.sin(fromLat)
                           - sinLat0 * Math.cos(fromLat) * Math.cos(lonDiff));
            }

            resultXA[i] = toX;
            resultYA[i] = toY;
        }
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {
        int      cnt    = from[0].length;
        double[] fromXA = from[INDEX_X];
        double[] fromYA = from[INDEX_Y];
        double[] toLatA = to[INDEX_LAT];
        double[] toLonA = to[INDEX_LON];

        double   toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];


            fromX = fromX - false_east;
            fromY = fromY - false_north;
            double rho = Math.sqrt(fromX * fromX + fromY * fromY);
            double r   = rho / R;
            double con = P - 1.0;
            double com = P + 1.0;
            double c = Math.asin((P - Math.sqrt(1.0 - (r * r * com) / con))
                                 / (con / r + r / con));

            toLon = lon0;
            double temp = 0;
            if (Math.abs(rho) > TOLERANCE) {
                toLat = Math.asin(Math.cos(c) * sinLat0
                                  + (fromY * Math.sin(c) * cosLat0 / rho));
                if (Math.abs(lat0 - PI_OVER_4) > TOLERANCE) {  // not 90 or -90
                    temp = rho * cosLat0 * Math.cos(c)
                           - fromY * sinLat0 * Math.sin(c);
                    toLon = lon0 + Math.atan(fromX * Math.sin(c) / temp);
                } else if (lat0 == PI_OVER_4) {
                    toLon = lon0 + Math.atan(fromX / -fromY);
                    temp  = -fromY;
                } else {
                    toLon = lon0 + Math.atan(fromX / fromY);
                    temp  = fromY;
                }
            } else {
                toLat = lat0;
            }
            toLat = Math.toDegrees(toLat);
            toLon = Math.toDegrees(toLon);
            if (temp < 0) {
                toLon += 180;
            }
            toLon     = LatLonPointImpl.lonNormal(toLon);

            toLatA[i] = toLat;
            toLonA[i] = toLon;
        }
        return to;
    }

    /*ENDGENERATED*/

  /**
   * Create a ProjectionRect from the given LatLonRect.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param rect the LatLonRect
   * @return  ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
   */
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect rect) {

    ProjectionPoint llpt = latLonToProj(rect.getLowerLeftPoint(), new ProjectionPointImpl());
    ProjectionPoint urpt = latLonToProj(rect.getUpperRightPoint(), new ProjectionPointImpl());
    ProjectionPoint lrpt = latLonToProj(rect.getLowerRightPoint(), new ProjectionPointImpl());
    ProjectionPoint ulpt = latLonToProj(rect.getUpperLeftPoint(), new ProjectionPointImpl());

    // how many are bad?
    List<ProjectionPoint> goodPts = new ArrayList<ProjectionPoint>(4);
    int countBad = 0;
    if (!addGoodPts(goodPts, llpt))
      countBad++;
    if (!addGoodPts(goodPts, urpt))
      countBad++;
    if (!addGoodPts(goodPts, lrpt))
      countBad++;
    if (!addGoodPts(goodPts, ulpt))
      countBad++;

    // case : 3 or 4 good points, just use those

    // case: only 2 good ones : extend to edge of the limit circle
    if (countBad == 2) {

      if (!ProjectionPointImpl.isInfinite(llpt) && !ProjectionPointImpl.isInfinite(lrpt)) {
        addGoodPts( goodPts, new ProjectionPointImpl(0, maxR));

      } else if (!ProjectionPointImpl.isInfinite(ulpt) && !ProjectionPointImpl.isInfinite(llpt)) {
        addGoodPts( goodPts, new ProjectionPointImpl(maxR, 0));

      } else if (!ProjectionPointImpl.isInfinite(ulpt) && !ProjectionPointImpl.isInfinite(urpt)) {
        addGoodPts( goodPts, new ProjectionPointImpl(0, -maxR));

      } else if (!ProjectionPointImpl.isInfinite(urpt) && !ProjectionPointImpl.isInfinite(lrpt)) {
        addGoodPts( goodPts, new ProjectionPointImpl(-maxR, 0));

      } else {
        throw new IllegalStateException();
}

    } else if (countBad == 3) { // case: only 1 good one : extend to wedge of the limit circle

      if (!ProjectionPointImpl.isInfinite(llpt)) {
        double xcoord = llpt.getX();
        addGoodPts( goodPts, new ProjectionPointImpl(xcoord, getLimitCoord( xcoord)));

        double ycoord = llpt.getY();
        addGoodPts( goodPts, new ProjectionPointImpl(getLimitCoord( ycoord), ycoord));
      }

      else if (!ProjectionPointImpl.isInfinite(urpt)) {
        double xcoord = urpt.getX();
        addGoodPts( goodPts, new ProjectionPointImpl(xcoord, -getLimitCoord( xcoord)));

        double ycoord = urpt.getY();
        addGoodPts( goodPts, new ProjectionPointImpl( -getLimitCoord( ycoord), ycoord));
      }

      else if (!ProjectionPointImpl.isInfinite(ulpt)) {
        double xcoord = ulpt.getX();
        addGoodPts( goodPts, new ProjectionPointImpl(xcoord, -getLimitCoord( xcoord)));

        double ycoord = ulpt.getY();
        addGoodPts( goodPts, new ProjectionPointImpl( getLimitCoord( ycoord), ycoord));
      }

      else if (!ProjectionPointImpl.isInfinite(lrpt)) {
        double xcoord = lrpt.getX();
        addGoodPts( goodPts, new ProjectionPointImpl(xcoord, getLimitCoord( xcoord)));

        double ycoord = lrpt.getY();
        addGoodPts( goodPts, new ProjectionPointImpl( -getLimitCoord( ycoord), ycoord));

      } else {
        throw new IllegalStateException();
      }

    }

    return makeRect(goodPts);
  }

  private boolean addGoodPts(List<ProjectionPoint> goodPts, ProjectionPoint pt) {
    if (!ProjectionPointImpl.isInfinite(pt)) {
      goodPts.add(pt);
      //System.out.println("  good= "+pt);
      return true;
    } else return false;
  }

    // where does line x|y = coord intersest the map limit circle?
    // return the positive root.
  private double getLimitCoord(double coord) {
    return Math.sqrt(maxR2 - coord*coord);
  }

  private ProjectionRect makeRect(List<ProjectionPoint> goodPts) {
    double minx = Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;
    for (ProjectionPoint pp : goodPts) {
      minx = Math.min(minx, pp.getX());
      maxx = Math.max(maxx, pp.getX());
      miny = Math.min(miny, pp.getY());
      maxy = Math.max(maxy, pp.getY());
    }
    return new ProjectionRect(minx,miny,maxx,maxy);
  }



  /*LatLonPointImpl clip(LatLonPointImpl llpt) {

  }


  ProjectionRect clip(LatLonRect bb) {
    LatLonPointImpl llpt = bb.getLowerLeftPoint();
    LatLonPointImpl urpt = bb.getUpperRightPoint();
    LatLonPointImpl lrpt = bb.getLowerRightPoint();
    LatLonPointImpl ulpt = bb.getUpperLeftPoint();

  } */


    private static void test(double lat, double lon) {
      double radius = 6371.0;

      VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, 5.62*radius);
      ProjectionPointImpl p = a.latLonToProj(lat, lon);
      System.out.println("-----\nproj point = " + p);
      System.out.println("x/r = " + p.getX()/radius); // see snyder p 174
      System.out.println("y/r = " + p.getY()/radius);

      LatLonPoint ll = a.projToLatLon(p);
      System.out.println(" lat = "+ll.getLatitude()+  " should be= " + lat);
      System.out.println(" lon = "+ll.getLongitude()+  " should be= " + lon);
    }

    /**
     * Test
     *
     * @param args not used
     */
    public static void main2(String[] args) {
      test(40.0, 0.0);
      test(40.0, 40.0);
      test(0.0, 40.0);
    }

  public static void main(String[] args) {
    double radius = 6371.0;
    double height = 35747.0;

    VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, height);

    double limit = .99 * Math.sqrt((a.P-1)/(a.P+1));
    System.out.println(" limit = "+limit);
    System.out.println(" limit*90 = "+limit*90);

    /* whats the min and max lat/lon ?
    double theta = 0;
    while (theta <= 360) {
      double x = limit*radius * Math.cos(Math.toRadians(theta));
      double y = limit*radius * Math.sin(Math.toRadians(theta));
      LatLonPointImpl llpt = (LatLonPointImpl) a.projToLatLon( new ProjectionPointImpl(x, y));
      System.out.println(theta+" = "+llpt.toString());
      theta += 15;
    } */

    LatLonRect rect = new LatLonRect(new LatLonPointImpl(-45.0, -45.0), -45.0, -45.0);
    ProjectionRect r = a.latLonToProjBB(rect);
    System.out.println(" ProjectionRect result = "+r);


    /*  double minx, maxx, miny, maxy;

    // first clip the request rectangle to the bounding box of the grid
    LatLonRect bb = getLatLonBoundingBox();
    rect = bb.intersect( rect);
    if (null == rect)
      throw new InvalidRangeException("Request Bounding box does not intersect Grid");

    LatLonPointImpl llpt = rect.getLowerLeftPoint();
    LatLonPointImpl urpt = rect.getUpperRightPoint();
    LatLonPointImpl lrpt = rect.getLowerRightPoint();
    LatLonPointImpl ulpt = rect.getUpperLeftPoint();

    if (isLatLon()) {
      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

    } else {
      Projection dataProjection = getProjection();
      ProjectionPoint ll = dataProjection.latLonToProj(llpt, new ProjectionPointImpl());
      ProjectionPoint ur = dataProjection.latLonToProj(urpt, new ProjectionPointImpl());
      ProjectionPoint lr = dataProjection.latLonToProj(lrpt, new ProjectionPointImpl());
      ProjectionPoint ul = dataProjection.latLonToProj(ulpt, new ProjectionPointImpl());

      minx = Math.min(ll.getX(), ul.getX());
      miny = Math.min(ll.getY(), lr.getY());
      maxx = Math.max(ur.getX(), lr.getX());
      maxy = Math.max(ul.getY(), ur.getY());
    }  */
  }
}

