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

/**
 *  Lambert AzimuthalEqualArea Projection spherical earth.
 *  <p>
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532,
 *  2nd edition (1983), p 184
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author Unidata Development Team
 */

public class LambertAzimuthalEqualArea extends ProjectionImpl {

    /** constants from Snyder's equations */
    private double R, sinLat0, cosLat0, lon0Degrees;

    /** center lat/lon in degrees */
    private double lat0, lon0;

    /** false easting */
    private double falseEasting;

    /** false northing */
    private double falseNorthing;

    /** origin */
    private LatLonPointImpl origin;  // why are we keeping this?

    /** spherical vs ellipsoidal */
    private boolean spherical = true;

        /** copy constructor - avoid clone !! */
    public ProjectionImpl constructCopy( ) {
      return new LambertAzimuthalEqualArea( getOriginLat(), getOriginLon(), getFalseEasting(), getFalseNorthing(), R);
    }

    /**
     *  Constructor with default parameters
     */
    public LambertAzimuthalEqualArea() {
        this(40.0, 100.0);
    }

    /**
     * Construct a LambertAzimuthalEqualArea Projection.
     *
     * @param lat0   lat origin of the coord system on the projection plane
     * @param lon0   lon origin of the coord system on the projection plane
     * @throws IllegalArgumentException
     */
    public LambertAzimuthalEqualArea(double lat0, double lon0) {
        this(lat0, lon0, 0.0, 0.0, EARTH_RADIUS);
    }

    /**
     * Construct a LambertAzimuthalEqualArea Projection.
     *
     * @param lat0   lat origin of the coord system on the projection plane
     * @param lon0   lon origin of the coord system on the projection plane
     * @param false_easting natural_x_coordinate + false_easting = x coordinate in km
     * @param false_northing natural_y_coordinate + false_northing = y coordinate in km
     * @param earthRadius   radius of the earth in km
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public LambertAzimuthalEqualArea(double lat0, double lon0,
                                     double false_easting,
                                     double false_northing,
                                     double earthRadius) {

        this.lat0   = Math.toRadians(lat0);
        lon0Degrees = lon0;
        this.lon0   = Math.toRadians(lon0);
        R           = earthRadius;

        if (Double.isNaN(false_easting))
            false_easting = 0.0;
        if (Double.isNaN(false_northing))
            false_northing = 0.0;
        this.falseEasting  = false_easting;
        this.falseNorthing = false_northing;

        origin             = new LatLonPointImpl(lat0, lon0);
        precalculate();

        addParameter(ATTR_NAME, "lambert_azimuthal_equal_area");
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("longitude_of_projection_origin", lon0);
        addParameter("earth_radius", R * 1000); // m
        if ((false_easting != 0.0) || (false_northing != 0.0)) {
            addParameter("false_easting", false_easting);  
            addParameter("false_northing", false_northing);
            addParameter("units", "km");
        }
    }
  
    /**
     * Precalculate some stuff
     */
    private void precalculate() {
        sinLat0     = Math.sin(lat0);
        cosLat0     = Math.cos(lat0);
        lon0Degrees = Math.toDegrees(lon0);
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        LambertAzimuthalEqualArea cl = (LambertAzimuthalEqualArea) super.clone();
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
        if ( !(proj instanceof LambertAzimuthalEqualArea)) {
            return false;
        }

        LambertAzimuthalEqualArea oo = (LambertAzimuthalEqualArea) proj;
        return ((this.getOriginLat() == oo.getOriginLat())
                && (this.getOriginLon() == oo.getOriginLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
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
     * Get the false easting, in km.
     * @return the false easting.
     */
    public double getFalseEasting() {
        return falseEasting;
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
     * Get the false northing, in km.
     * @return the false northing.
     */
    public double getFalseNorthing() {
        return falseNorthing;
    }

    /**
     * Set the false northing, in km.
     * natural_y_coordinate + false_northing = y coordinate
     * @param falseNorthing y offset
     */
    public void setFalseNorthing(double falseNorthing) {
        this.falseNorthing = falseNorthing;
    }



    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "Lambert Azimuth Equal Area";
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
        double g =
        sinLat0*Math.sin(fromLat) + cosLat0*Math.cos(fromLat)*Math.cos(lonDiff);

        double kPrime = Math.sqrt(2/(1 + g));
        toX = R*kPrime*Math.cos(fromLat)*Math.sin(lonDiff) + falseEasting;
        toY = R*kPrime*(cosLat0*Math.sin(fromLat) - sinLat0*Math.cos(fromLat)*Math.cos(lonDiff)) + falseNorthing;
      }

      projToLatLon {} {

        fromX = fromX - falseEasting;
        fromY = fromY - falseNorthing;
        double rho = Math.sqrt(fromX*fromX + fromY*fromY);
        double c = 2*Math.asin(rho/(2*R));
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
    tcl convert.tcl LambertAzimuthalEqualArea.java
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
        double g = sinLat0 * Math.sin(fromLat)
                   + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);

        double kPrime = Math.sqrt(2 / (1 + g));
        toX = R * kPrime * Math.cos(fromLat) * Math.sin(lonDiff)
              + falseEasting;
        toY = R * kPrime
              * (cosLat0 * Math.sin(fromLat)
                 - sinLat0 * Math.cos(fromLat)
                   * Math.cos(lonDiff)) + falseNorthing;

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



        fromX = fromX - falseEasting;
        fromY = fromY - falseNorthing;
        double rho = Math.sqrt(fromX * fromX + fromY * fromY);
        double c   = 2 * Math.asin(rho / (2 * R));
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
            double g = sinLat0 * Math.sin(fromLat)
                       + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);

            double kPrime = Math.sqrt(2 / (1 + g));
            toX = R * kPrime * Math.cos(fromLat) * Math.sin(lonDiff)
                  + falseEasting;
            toY = R * kPrime
                  * (cosLat0 * Math.sin(fromLat)
                     - sinLat0 * Math.cos(fromLat)
                       * Math.cos(lonDiff)) + falseNorthing;

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


            fromX = fromX - falseEasting;
            fromY = fromY - falseNorthing;
            double rho = Math.sqrt(fromX * fromX + fromY * fromY);
            double c   = 2 * Math.asin(rho / (2 * R));
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
            double g = sinLat0 * Math.sin(fromLat)
                       + cosLat0 * Math.cos(fromLat) * Math.cos(lonDiff);

            double kPrime = Math.sqrt(2 / (1 + g));
            toX = R * kPrime * Math.cos(fromLat) * Math.sin(lonDiff)
                  + falseEasting;
            toY = R * kPrime
                  * (cosLat0 * Math.sin(fromLat)
                     - sinLat0 * Math.cos(fromLat)
                       * Math.cos(lonDiff)) + falseNorthing;

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


            fromX = fromX - falseEasting;
            fromY = fromY - falseNorthing;
            double rho = Math.sqrt(fromX * fromX + fromY * fromY);
            double c   = 2 * Math.asin(rho / (2 * R));
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
     * Test
     *
     * @param args not used
     */
    public static void main(String[] args) {
        LambertAzimuthalEqualArea a = new LambertAzimuthalEqualArea(40, -100);
        ProjectionPointImpl       p = a.latLonToProj(-20, 100);
        System.out.println("proj point = " + p);
        LatLonPoint ll = a.projToLatLon(p);
        System.out.println("ll = " + ll);
    }
}

