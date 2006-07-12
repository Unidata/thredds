/*
 * $Id$
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
 *  Transverse Mercator projection, spherical earth.
 *  Projection plane is a cylinder tangent to the earth at tangentLon.
 *  See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 53
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author John Caron
 *   @version $Id$
 */


public class TransverseMercator extends ProjectionImpl {

    /** parameters */
    private double lat0, lon0, scale;

    /** origin point */
    private LatLonPointImpl origin;

    /** Constructor with default parameteres */
    public TransverseMercator() {
        this(40.0, -105.0, .9996);
    }

    /**
     * Construct a TransverseMercator Projection.
     * @param lat0        origin of projection coord system is at
     *                    (lat0, tangentLon)
     * @param tangentLon  longitude that the cylinder is tangent at
     *                    ("central meridian")
     * @param scale scale factor along the central meridian
     */
    public TransverseMercator(double lat0, double tangentLon, double scale) {
        this.lat0  = Math.toRadians(lat0);
        this.lon0  = Math.toRadians(tangentLon);
        this.scale = scale * EARTH_RADIUS;
        origin     = new LatLonPointImpl(lat0, tangentLon);

        addParameter(ATTR_NAME, "transverse_mercator");
        addParameter("longitude_of_central_meridian", tangentLon);
        addParameter("latitude_of_projection_origin", lat0);
        addParameter("scale_factor_at_central_meridian", scale);
    }

    // bean properties

    /**
     * Get the scale
     * @return the scale
     */
    public double getScale() {
        return scale / EARTH_RADIUS;
    }

    /**
     * Set the scale
     *
     * @param scale  the scale
     */
    public void setScale(double scale) {
        this.scale = EARTH_RADIUS * scale;
    }

    /**
     * Get the tangent longitude
     * @return the tangent longitude
     */
    public double getTangentLon() {
        return origin.getLongitude();
    }

    /**
     * Set the tangent longitude
     *
     * @param lon the tangent longitude
     */
    public void setTangentLon(double lon) {
        origin.setLongitude(lon);
        lon0 = Math.toRadians(lon);
    }

    /**
     * Get the origin latitude
     * @return the origin latitude
     */
    public double getOriginLat() {
        return origin.getLatitude();
    }

    /**
     * Set the origin latitude
     *
     * @param lat  the origin latitude
     */
    public void setOriginLat(double lat) {
        origin.setLatitude(lat);
        lat0 = Math.toRadians(lat);
    }


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
     * @return the parameters as a String
     */
    public String paramsToString() {
        return " origin " + origin.toString() + " scale: "
               + Format.d(getScale(), 6);
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
        return (pt1.getY() * pt2.getY() < 0)
               && (Math.abs(pt1.getY() - pt2.getY()) > 2 * EARTH_RADIUS);
    }


    /**
     * Clone this projection
     * @return a clone of this.
     */
    public Object clone() {
        TransverseMercator cl = (TransverseMercator) super.clone();
        cl.origin = new LatLonPointImpl(getOriginLat(), getTangentLon());
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

        TransverseMercator oo = (TransverseMercator) proj;
        return ((this.getScale() == oo.getScale())
                && (this.getOriginLat() == oo.getOriginLat())
                && (this.getTangentLon() == oo.getTangentLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
    }



    /*MACROBODY
        projToLatLon {} {
        double x = fromX/scale;
        double d = fromY/scale + lat0;
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
        toX = scale * SpecialMathFunction.atanh(b);
        toY = scale * (Math.atan2(Math.tan(lat),Math.cos(dlon)) - lat0);
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
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude();
        double fromLon = latLon.getLongitude();


        double lon     = Math.toRadians(fromLon);
        double lat     = Math.toRadians(fromLat);
        double dlon    = lon - lon0;
        double b       = Math.cos(lat) * Math.sin(dlon);
        // infinite projection
        if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
            toX = 0.0;
            toY = 0.0;
        } else {
            toX = scale * SpecialMathFunction.atanh(b);
            toY = scale * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0);
        }

        result.setLocation(toX, toY);
        return result;
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
    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double toLat, toLon;
        double fromX = world.getX();
        double fromY = world.getY();


        double x     = fromX / scale;
        double d     = fromY / scale + lat0;
        toLon = Math.toDegrees(lon0
                               + Math.atan2(SpecialMathFunction.sinh(x),
                                            Math.cos(d)));
        toLat = Math.toDegrees(Math.asin(Math.sin(d)
                                         / SpecialMathFunction.cosh(x)));

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

            double lon     = Math.toRadians(fromLon);
            double lat     = Math.toRadians(fromLat);
            double dlon    = lon - lon0;
            double b       = Math.cos(lat) * Math.sin(dlon);
            // infinite projection
            if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
                toX = 0.0;
                toY = 0.0;
            } else {
                toX = scale * SpecialMathFunction.atanh(b);
                toY = scale
                      * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0);
            }

            resultXA[i] = (float) toX;
            resultYA[i] = (float) toY;
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

        double  toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double x     = fromX / scale;
            double d     = fromY / scale + lat0;
            toLon = Math.toDegrees(lon0
                                   + Math.atan2(SpecialMathFunction.sinh(x),
                                                Math.cos(d)));
            toLat = Math.toDegrees(Math.asin(Math.sin(d)
                                             / SpecialMathFunction.cosh(x)));

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

            double lon     = Math.toRadians(fromLon);
            double lat     = Math.toRadians(fromLat);
            double dlon    = lon - lon0;
            double b       = Math.cos(lat) * Math.sin(dlon);
            // infinite projection
            if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) {
                toX = 0.0;
                toY = 0.0;
            } else {
                toX = scale * SpecialMathFunction.atanh(b);
                toY = scale
                      * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0);
            }

            resultXA[i] = (double) toX;
            resultYA[i] = (double) toY;
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

        double   toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double x     = fromX / scale;
            double d     = fromY / scale + lat0;
            toLon = Math.toDegrees(lon0
                                   + Math.atan2(SpecialMathFunction.sinh(x),
                                                Math.cos(d)));
            toLat = Math.toDegrees(Math.asin(Math.sin(d)
                                             / SpecialMathFunction.cosh(x)));

            toLatA[i] = (double) toLat;
            toLonA[i] = (double) toLon;
        }
        return to;
    }

    /*ENDGENERATED*/


}

/* Change History:
   $Log: TransverseMercator.java,v $
   Revision 1.24  2005/11/02 20:04:15  dmurray
   add the Orthographic projection, refactor some of the constants up to
   ProjectionImpl, move the radius declaration in Earth up to the top,
   fix a problem in Mercator where infinite points were set to 0,0 instead
   of infinity.

   Revision 1.23  2005/05/13 18:29:19  jeffmc
   Clean up the odd copyright symbols

   Revision 1.22  2005/05/13 12:26:51  jeffmc
   Some mods

   Revision 1.21  2005/05/13 11:14:10  jeffmc
   Snapshot

   Revision 1.20  2004/12/07 01:51:54  caron
   make parameter names CF compliant.

   Revision 1.19  2004/09/22 21:19:52  caron
   use Parameter, not Attribute; remove nc2 dependencies

   Revision 1.18  2004/07/30 17:22:21  dmurray
   Jindent and doclint

   Revision 1.17  2004/02/27 21:21:40  jeffmc
   Lots of javadoc warning fixes

   Revision 1.16  2004/01/29 17:35:02  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.15  2003/07/12 23:08:59  caron
   add cvs headers, trailers

*/









