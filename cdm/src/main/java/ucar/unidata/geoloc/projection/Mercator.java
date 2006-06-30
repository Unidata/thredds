/*
 * $Id: Mercator.java,v 1.5 2005/11/02 20:04:14 dmurray Exp $
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
import ucar.unidata.util.SpecialMathFunction;


/**
 *  Mercator projection, spherical earth.
 *  Projection plane is a cylinder tangent to the earth at tangentLon.
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 47
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author John Caron
 *   @version $Id: Mercator.java,v 1.5 2005/11/02 20:04:14 dmurray Exp $
 */


public class Mercator extends ProjectionImpl {

    /** parameters */
    private double lat0;

    /** _more_ */
    private double lon0;

    /** _more_ */
    private double par;

    /** _more_ */
    private double lat0_r;

    /** _more_ */
    private double lon0_r;

    /** _more_ */
    private double par_r;

    /** _more_ */
    private double A;

    /** _more_ */
    private double y0;

    /** origin point */
    private LatLonPointImpl origin;

    /** Constructor with default parameteres */
    public Mercator() {
        this(40.0, -105, 20.0);
    }

    /**
     * Construct a Mercator Projection.
     * @param lat0 latitude of origin (degrees)
     * @param lon0 longitude of origin (degrees)
     * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
     */
    public Mercator(double lat0, double lon0, double par) {
        origin      = new LatLonPointImpl(lat0, lon0);
        this.lat0   = lat0;
        this.lon0   = lon0;
        this.par    = par;

        this.lat0_r = Math.toRadians(lat0);
        this.lon0_r = Math.toRadians(lon0);
        this.par_r  = Math.toRadians(par);

        precalculate();

        addParameter(ATTR_NAME, "mercator");
        addParameter("longitude_of_projection_origin", lon0);
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("standard_parallel", par);
    }

    /**
     * _more_
     */
    private void precalculate() {
        A = EARTH_RADIUS * Math.cos(par_r);  // incorporates the scale factor at par
        y0 = A * SpecialMathFunction.atanh(Math.sin(lat0_r));
    }


    /**
     * Get the first standard parallel
     *
     * @return the first standard parallel
     */
    public double getParallel() {
        return par;
    }

    /**
     * Set the first standard parallel
     *
     * @param par   the first standard parallel
     */
    public void setParallel(double par) {
        this.par = par;
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
        lon0   = lon;
        lon0_r = Math.toRadians(lon);
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
     * @param lat   the origin latitude.
     */
    public void setOriginLat(double lat) {
        origin.setLatitude(lat);
        lat0   = lat;
        lat0_r = Math.toRadians(lat);
        precalculate();
    }



    /**
     * Get the parameters as a String
     * @return the parameters as a String
     */
    public String paramsToString() {
        return " origin " + origin.toString() + " parellel: "
               + Format.d(getParallel(), 6);
    }

    /**
     * Does the line between these two points cross the projection "seam".
     *
     * @param pt1  the line goes between these two points
     * @param pt2  the line goes between these two points
     *
     * @return false if there is no seam
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1)
                || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }

        // opposite signed long lines: LOOK ????
        return (pt1.getX() * pt2.getX() < 0);
    }


    /**
     * Clone this projection
     * @return a clone of this.
     */
    public Object clone() {
        Mercator cl = (Mercator) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
        return (Object) cl;
    }

    /**
     * Returns true if this represents the same Projection as proj.
     *
     * @param proj    projection in question
     * @return true if this represents the same Projection as proj.
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof TransverseMercator)) {
            return false;
        }

        Mercator oo = (Mercator) proj;
        return ((this.getParallel() == oo.getParallel())
                && (this.getOriginLat() == oo.getOriginLat())
                && (this.getOriginLon() == oo.getOriginLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
    }

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
        double fromLat   = latLon.getLatitude();
        double fromLon   = latLon.getLongitude();
        double fromLat_r = Math.toRadians(fromLat);

        // infinite projection
        if ((Math.abs(90.0 - Math.abs(fromLat))) < TOLERANCE) {
            toX = Double.POSITIVE_INFINITY;
            toY = Double.POSITIVE_INFINITY;
        } else {
            toX = A * Math.toRadians(LatLonPointImpl.range180(fromLon
                    - this.lon0));
            toY = A * SpecialMathFunction.atanh(Math.sin(fromLat_r)) - y0;  // ?? - y0 ??
        }

        result.setLocation(toX, toY);
        return result;
    }

    /*  void PJGs_merc_latlon2xy(PJGstruct *ps, double lat, double lon, double *x, double *y)
    /*
     *  Input:  lat, lon in degrees (lat N+, lon E+)
     *  Output: x, y in transverse mercator projection
     *
     *
        {
            cs_t *cs = (cs_t *) ps;

            lat *= DEG_TO_RAD;
            *x = cs->Merc_a * PJGrange180(lon - cs->Merc_lon0) * DEG_TO_RAD;
            *y = cs->Merc_a * atanh( sin(lat)) - cs->Merc_y0;
        }


    void PJGs_merc_xy2latlon(PJGstruct *ps, double x, double y, double *lat, double *lon)
    /*
     *  Input: x, y in transverse mercator projection
     *  Output:  lat, lon in degrees (lat N+, lon E+)
     *
     *
        {
            cs_t *cs = (cs_t *) ps;
            double e;

            *lon = (x/cs->Merc_a) * RAD_TO_DEG + cs->Merc_lon0;

            y += cs->Merc_y0;
            e = exp(-y/cs->Merc_a);
            *lat = (PI/2 - 2 * atan(e)) * RAD_TO_DEG;
        }


        /**
         * Convert projection coordinates to a LatLonPoint
         *   Note: a new object is not created on each call for the return value.
         *
         * @param world   convert from these projection coordinates
         * @param result the object to write to
         *
         * @return LatLonPoint convert to these lat/lon coordinates
         */

    /**
     * _more_
     *
     * @param world _more_
     * @param result _more_
     *
     * @return _more_
     */
    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double fromX = world.getX();
        double fromY = world.getY();

        double toLon = Math.toDegrees(fromX / A) + lon0;

        fromY += y0;
        double e     = Math.exp(-fromY / A);
        double toLat = Math.toDegrees(Math.PI / 2 - 2 * Math.atan(e));

        result.setLatitude(toLat);
        result.setLongitude(toLon);
        return result;
    }

}
