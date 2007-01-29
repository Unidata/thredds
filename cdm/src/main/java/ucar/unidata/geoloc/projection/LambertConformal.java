/*
 * $Id: LambertConformal.java,v 1.34 2006/11/18 19:03:22 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.unidata.geoloc.projection;


import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;
import ucar.unidata.util.Parameter;

import ucar.units.*;


/**
 *  Lambert Conformal Projection, one or two standard parallels, spherical earth.
 *  Projection plane is a cone whose vertex lies on the line of the earth's axis,
 *  and intersects the earth at two parellels (par1, par2), or is tangent to the earth at
 *  one parellel par1 = par2. The cone is flattened by splitting along the longitude = lon0+180.
 *  <p>
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 101
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author John Caron
 *   @version $Id: LambertConformal.java,v 1.34 2006/11/18 19:03:22 dmurray Exp $
 */

public class LambertConformal extends ProjectionImpl {


    /** constants from Snyder's equations */
    private double n, F, rho;

    /** Earth's radius time F */
    private double earthRadiusTimesF;

    /** lon naught */
    private double lon0Degrees;

    /** lat/lon in radians */
    private double lat0, lon0;  // radians

    /** parallel 1 and 2 */
    private double par1, par2;  // degrees


    /** _more_ */
    private double falseEasting, falseNorthing;

    /** origin */
    private LatLonPointImpl origin;  // why are we keeping this?
    //private double maxY;       // for crossSeam

    // spherical vs ellipsoidal

    /** _more_ */
    private boolean spherical = true;

    /**
     *  Constructor with default parameters
     */
    public LambertConformal() {
        this(40.0, -105.0, 20.0, 60.0);
    }

    /**
     * Construct a LambertConformal Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param par1   standard parallel 1
     * @param par2   standard parallel 2
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public LambertConformal(double lat0, double lon0, double par1,
                            double par2) {
        this(lat0, lon0, par1, par2, 0.0, 0.0, null);
    }

    /**
     * Construct a LambertConformal Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0   lat origin of the coord. system on the projection plane
     * @param lon0   lon origin of the coord. system on the projection plane
     * @param par1   standard parallel 1
     * @param par2   standard parallel 2
     * @param false_easting natural_x_coordinate + false_easting = x coordinate
     * @param false_northing natural_y_coordinate + false_northing = y coordinate
     * @param units units of the false_easting, false_northing (ignored if zero), must be udunits convertible to km.
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public LambertConformal(double lat0, double lon0, double par1,
                            double par2, double false_easting,
                            double false_northing, String units) {

        this.lat0 = Math.toRadians(lat0);
        this.lon0 = Math.toRadians(lon0);

        this.par1 = par1;
        this.par2 = par2;

        double scale = 1.0;
        if (Double.isNaN(false_easting)) {
            false_easting = 0.0;
        }
        if (Double.isNaN(false_northing)) {
            false_northing = 0.0;
        }
        boolean useFalseOrigin = (false_easting != 0.0)
                                 || (false_northing != 0.0);

        if (useFalseOrigin) {
            if (units == null) {
                units = "km";
            }
            try {
                scale = getConversionFactor(units, "km");
            } catch (Exception e) {
                throw new IllegalArgumentException(units
                        + " not convertible to km");
            }
        }
        this.falseEasting  = scale * false_easting;
        this.falseNorthing = scale * false_northing;

        origin             = new LatLonPointImpl(lat0, lon0);
        precalculate();

        addParameter(ATTR_NAME, "lambert_conformal_conic");
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("longitude_of_central_meridian", lon0);
        if (par2 == par1) {
            addParameter("standard_parallel", par1);
        } else {
            double[] data = new double[2];
            data[0] = par1;
            data[1] = par2;
            addParameter(new Parameter("standard_parallel", data));
        }
        if (useFalseOrigin) {
            addParameter("false_easting", false_easting);  // stay with original units
            addParameter("false_northing", false_northing);
            addParameter("units", units);
        }
    }

    // move this to ucar.unit or ucar.unidata.util

    /** _more_ */
    static private UnitFormat format = UnitFormatManager.instance();

    /**
     * _more_
     *
     * @param inputUnitString _more_
     * @param outputUnitString _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    static private double getConversionFactor(String inputUnitString,
            String outputUnitString)
            throws Exception {
        Unit uuInput  = format.parse(inputUnitString);
        Unit uuOutput = format.parse(outputUnitString);
        return uuInput.convertTo(1.0, uuOutput);
    }

    /**
     * Precalculate some stuff
     */
    private void precalculate() {
        if (Math.abs(lat0 - PI_OVER_2) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal lat0 = 90");
        }
        if (Math.abs(lat0 + PI_OVER_2) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal lat0 = -90");
        }
        if (Math.abs(par1 - 90.0) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal par1 = 90");
        }
        if (Math.abs(par1 + 90.0) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal par1 = -90");
        }
        if (Math.abs(par2 - 90.0) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal par2 = 90");
        }
        if (Math.abs(par2 + 90.0) < TOLERANCE) {
            throw new IllegalArgumentException("LambertConformal par2 = -90");
        }

        double par1r = Math.toRadians(this.par1);
        double par2r = Math.toRadians(this.par2);

        double t1    = Math.tan(Math.PI / 4 + par1r / 2);
        double t2    = Math.tan(Math.PI / 4 + par2r / 2);

        if (Math.abs(par2 - par1) < TOLERANCE) {  // single parallel
            n = Math.sin(par1r);
        } else {
            n = Math.log(Math.cos(par1r) / Math.cos(par2r))
                / Math.log(t2 / t1);
        }

        double t1n = Math.pow(t1, n);
        F                 = Math.cos(par1r) * t1n / n;
        earthRadiusTimesF = EARTH_RADIUS * F;

        double t0n = Math.pow(Math.tan(Math.PI / 4 + lat0 / 2), n);
        rho         = EARTH_RADIUS * F / t0n;

        lon0Degrees = Math.toDegrees(lon0);
        // need to know the pole value for crossSeam
        //Point2D pt = latLonToProj( 90.0, 0.0);
        //maxY = pt.getY();
        //System.out.println("LC = " +pt);
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        LambertConformal cl = (LambertConformal) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
        return (Object) cl;
    }



    /**
     * Check for equality with the Object in question
     *
     * @param proj  object to check
     * @return true if they are equal
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof LambertConformal)) {
            return false;
        }

        LambertConformal oo = (LambertConformal) proj;
        return ((this.getParallelOne() == oo.getParallelOne())
                && (this.getParallelTwo() == oo.getParallelTwo())
                && (this.getOriginLat() == oo.getOriginLat())
                && (this.getOriginLon() == oo.getOriginLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
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
     * Set the second standard parallel
     *
     * @param par   the second standard parallel
     */
    public void setParallelTwo(double par) {
        par2 = par;
        precalculate();
    }

    //Keep the  mispelled name for persisted objects

    /**
     * Set the second standard parallel.
     *
     * @param par   the second standard parallel
     * @deprecated  use setParallelTwo(double). Keep the mispelled name
     *              for persisted objects
     */
    public void setParellelTwo(double par) {
        par2 = par;
        precalculate();
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
     * Set the first standard parallel
     *
     * @param par   the first standard parallel
     */
    public void setParallelOne(double par) {
        par1 = par;
        precalculate();
    }

    //Keep the  mispelled name for persisted objects

    /**
     * Set the first standard parallel.
     *
     * @param par   the first standard parallel
     * @deprecated  use setParallelOne(double). Keep the mispelled name
     *              for persisted objects
     */
    public void setParellelOne(double par) {
        par1 = par;
        precalculate();
    }

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
     * @param falseEasting
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
     * @param falseNorthing
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
        return "Lambert conformal conic";
    }

    /**
     * Create a String of the parameters.
     * @return a String of the parameters
     */
    public String paramsToString() {
        return " origin " + origin.toString() + " parallels: "
               + Format.d(par1, 3) + " " + Format.d(par2, 3);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toWKS() {
        StringBuffer sbuff = new StringBuffer();
        sbuff.append("PROJCS[\"" + getName() + "\",");
        if (spherical) {
            sbuff.append("GEOGCS[\"Normal Sphere (r=6371007)\",");
            sbuff.append("DATUM[\"unknown\",");
            sbuff.append("SPHEROID[\"sphere\",6371007,0]],");
        } else {
            sbuff.append("GEOGCS[\"WGS 84\",");
            sbuff.append("DATUM[\"WGS_1984\",");
            sbuff.append("SPHEROID[\"WGS 84\",6378137,298.257223563],");
            sbuff.append("TOWGS84[0,0,0,0,0,0,0]],");
        }
        sbuff.append("PRIMEM[\"Greenwich\",0],");
        sbuff.append("UNIT[\"degree\",0.0174532925199433]],");
        sbuff.append("PROJECTION[\"Lambert_Conformal_Conic_1SP\"],");
        sbuff.append("PARAMETER[\"latitude_of_origin\"," + getOriginLat()
                     + "],");  // LOOK assumes getOriginLat = getParellel
        sbuff.append("PARAMETER[\"central_meridian\"," + getOriginLon()
                     + "],");
        sbuff.append("PARAMETER[\"scale_factor\",1],");
        sbuff.append("PARAMETER[\"false_easting\"," + falseEasting + "],");
        sbuff.append("PARAMETER[\"false_northing\"," + falseNorthing + "],");

        return sbuff.toString();
    }

    /**
     * Get the scale for the lat.
     *
     * @param lat   lat to use
     * @return scale
     */
    public double getScale(double lat) {
        lat = Math.toRadians(lat);
        double t  = Math.tan(Math.PI / 4 + lat / 2);
        double tn = Math.pow(t, n);
        double r1 = n * F;
        double r2 = Math.cos(lat) * tn;
        return r1 / r2;
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
        double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
        double theta = n * Math.toRadians(dlon);
        double tn = Math.pow( Math.tan(PI_OVER_4 + fromLat/2), n);
        double r = earthRadiusTimesF / tn;
        toX = r * Math.sin(theta);
        toY = rho - r * Math.cos(theta);
      }
      projToLatLon {double rhop = rho;} {
        if (n < 0) {
            rhop *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }

        double yd = (rhop - fromY);
        double theta = Math.atan2( fromX, yd);
        double r = Math.sqrt( fromX*fromX + yd*yd);
        if (n < 0.0)
            r *= -1.0;

        toLon = (Math.toDegrees(theta/n + lon0));

        if (Math.abs(r) < TOLERANCE) {
            toLat = ((n < 0.0) ? -90.0 : 90.0);
        } else {
            double rn = Math.pow( EARTH_RADIUS * F / r, 1/n);
            toLat = Math.toDegrees(2.0 * Math.atan( rn) - Math.PI/2);
        }
            }

    MACROBODY*/

    /*BEGINGENERATED*/

    /*
    Note this section has been generated using the convert.tcl script.
    This script, run as:
    tcl convert.tcl LambertConformal.java
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
        double dlon  = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
        double theta = n * Math.toRadians(dlon);
        double tn    = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
        double r     = earthRadiusTimesF / tn;
        toX = r * Math.sin(theta);
        toY = rho - r * Math.cos(theta);

        result.setLocation(toX + falseEasting, toY + falseNorthing);
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
        double fromX = world.getX() - falseEasting;
        double fromY = world.getY() - falseNorthing;
        double rhop  = rho;

        if (n < 0) {
            rhop  *= -1.0;
            fromX *= -1.0;
            fromY *= -1.0;
        }

        double yd    = (rhop - fromY);
        double theta = Math.atan2(fromX, yd);
        double r     = Math.sqrt(fromX * fromX + yd * yd);
        if (n < 0.0) {
            r *= -1.0;
        }

        toLon = (Math.toDegrees(theta / n + lon0));

        if (Math.abs(r) < TOLERANCE) {
            toLat = ((n < 0.0)
                     ? -90.0
                     : 90.0);
        } else {
            double rn = Math.pow(EARTH_RADIUS * F / r, 1 / n);
            toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
        }

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
     *
     * @return the "to" array
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
            double dlon  = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
            double theta = n * Math.toRadians(dlon);
            double tn    = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
            double r     = earthRadiusTimesF / tn;
            toX         = r * Math.sin(theta);
            toY         = rho - r * Math.cos(theta);

            resultXA[i] = (float) (toX + falseEasting);
            resultYA[i] = (float) (toY + falseNorthing);
        }
        return to;
    }

    /**
     * Convert projection coordinates to lat/lon coordinate.
     *
     * @param from    array of projection coordinates: from[2][n], where
     *                (from[0][i], from[1][i]) is the (x, y) coordinate
     *                of the ith point
     * @param to      resulting array of lat/lon coordinates: to[2][n] where
     *                (to[0][i], to[1][i]) is the (lat, lon) coordinate of
     *                the ith point
     *
     * @return the "to" array
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {
        int     cnt    = from[0].length;
        float[] fromXA = from[INDEX_X];
        float[] fromYA = from[INDEX_Y];
        float[] toLatA = to[INDEX_LAT];
        float[] toLonA = to[INDEX_LON];
        double  rhop   = rho;
        double  toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i] - falseEasting;
            double fromY = fromYA[i] - falseNorthing;

            if (n < 0) {
                rhop  *= -1.0;
                fromX *= -1.0;
                fromY *= -1.0;
            }

            double yd    = (rhop - fromY);
            double theta = Math.atan2(fromX, yd);
            double r     = Math.sqrt(fromX * fromX + yd * yd);
            if (n < 0.0) {
                r *= -1.0;
            }

            toLon = (Math.toDegrees(theta / n + lon0));

            if (Math.abs(r) < TOLERANCE) {
                toLat = ((n < 0.0)
                         ? -90.0
                         : 90.0);
            } else {
                double rn = Math.pow(EARTH_RADIUS * F / r, 1 / n);
                toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
            }

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
     *
     * @return the "to" array
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
            double dlon  = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
            double theta = n * Math.toRadians(dlon);
            double tn    = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
            double r     = earthRadiusTimesF / tn;
            toX         = r * Math.sin(theta);
            toY         = rho - r * Math.cos(theta);

            resultXA[i] = (double) toX + falseEasting;
            resultYA[i] = (double) toY + falseNorthing;
        }
        return to;
    }

    /**
     * Convert projection coordinates to lat/lon coordinate.
     *
     * @param from    array of projection coordinates: from[2][n], where
     *                (from[0][i], from[1][i]) is the (x, y) coordinate
     *                of the ith point
     * @param to      resulting array of lat/lon coordinates: to[2][n] where
     *                (to[0][i], to[1][i]) is the (lat, lon) coordinate of
     *                the ith point
     *
     * @return the "to" array
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {
        int      cnt    = from[0].length;
        double[] fromXA = from[INDEX_X];
        double[] fromYA = from[INDEX_Y];
        double[] toLatA = to[INDEX_LAT];
        double[] toLonA = to[INDEX_LON];
        double   rhop   = rho;
        double   toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i] - falseEasting;
            double fromY = fromYA[i] - falseNorthing;

            if (n < 0) {
                rhop  *= -1.0;
                fromX *= -1.0;
                fromY *= -1.0;
            }

            double yd    = (rhop - fromY);
            double theta = Math.atan2(fromX, yd);
            double r     = Math.sqrt(fromX * fromX + yd * yd);
            if (n < 0.0) {
                r *= -1.0;
            }

            toLon = (Math.toDegrees(theta / n + lon0));

            if (Math.abs(r) < TOLERANCE) {
                toLat = ((n < 0.0)
                         ? -90.0
                         : 90.0);
            } else {
                double rn = Math.pow(EARTH_RADIUS * F / r, 1 / n);
                toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
            }

            toLatA[i] = (double) toLat;
            toLonA[i] = (double) toLon;
        }
        return to;
    }

    /*ENDGENERATED*/




}

/*
 *  Change History:
 *  $Log: LambertConformal.java,v $
 *  Revision 1.34  2006/11/18 19:03:22  dmurray
 *  jindent
 *
 *  Revision 1.33  2005/11/02 20:04:14  dmurray
 *  add the Orthographic projection, refactor some of the constants up to
 *  ProjectionImpl, move the radius declaration in Earth up to the top,
 *  fix a problem in Mercator where infinite points were set to 0,0 instead
 *  of infinity.
 *
 *  Revision 1.32  2005/10/20 16:35:52  caron
 *  remove dependence of ucar.nc2; call ucar.units directly
 *
 *  Revision 1.31  2005/10/19 19:01:36  caron
 *  deal with NaNs correctly
 *
 *  Revision 1.30  2005/10/19 17:21:40  caron
 *  Lambert Conformal Projection now allows false origin
 *
 *  Revision 1.29  2005/05/13 18:29:18  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.28  2005/05/13 12:26:51  jeffmc
 *  Some mods
 *
 *  Revision 1.27  2005/05/13 11:14:10  jeffmc
 *  Snapshot
 *
 *  Revision 1.26  2005/04/28 19:28:24  caron
 *  grix x,y has to be in km
 *
 *  Revision 1.25  2005/04/07 23:33:05  caron
 *  minor
 *
 *  Revision 1.24  2004/12/10 15:07:52  dmurray
 *  Jindent John's changes
 *
 *  Revision 1.23  2004/12/07 01:51:54  caron
 *  make parameter names CF compliant.
 *
 *  Revision 1.22  2004/11/04 00:38:55  caron
 *  minor
 *
 *  Revision 1.21  2004/09/22 21:19:52  caron
 *  use Parameter, not Attribute; remove nc2 dependencies
 *
 *  Revision 1.20  2004/07/30 17:22:20  dmurray
 *  Jindent and doclint
 *
 *  Revision 1.19  2004/02/27 21:21:39  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.18  2004/01/29 17:35:00  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.17  2003/07/12 23:08:59  caron
 *  add cvs headers, trailers
 *
 */








