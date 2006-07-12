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


/**
 *  Stereographic projection, spherical earth.
 *  Projection plane is a plane tangent to the earth at latt, lont.
 *  see John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 153
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author John Caron
 *   @version $Id$
 */

public class Stereographic extends ProjectionImpl {

    /** some variables */
    private double latt, lont, scale, sinlatt, coslatt;

    /** origin point */
    private LatLonPointImpl origin;

    /** Constructor with default parameters = North Polar */
    public Stereographic() {
        this(90.0, -105.0, 1.0);
    }

    /**
     * Construct a Stereographic Projection.
     *
     * @param latt    tangent point of projection, also origin of
     *                projecion coord system
     * @param lont    tangent point of projection, also origin of
     *                projecion coord system
     * @param scale   scale factor at tangent point, "normally 1.0 but
     *                may be reduced"
     */
    public Stereographic(double latt, double lont, double scale) {
        this.latt  = Math.toRadians(latt);
        this.lont  = Math.toRadians(lont);
        this.scale = scale * EARTH_RADIUS;
        precalculate();
        origin = new LatLonPointImpl(latt, lont);

        addParameter(ATTR_NAME, "stereographic");
        addParameter("longitude_of_projection_origin", lont);
        addParameter("latitude_of_projection_origin", latt);
        addParameter("scale_factor_at_projection_origin", scale);
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
     * @return  the tangent longitude
     */
    public double getTangentLon() {
        return origin.getLongitude();
    }

    /**
     * Set the tangent longitude
     *
     * @param lon  the tangent longitude
     */
    public void setTangentLon(double lon) {
        origin.setLongitude(lon);
        lont = Math.toRadians(lon);
    }

    /**
     * Get the tangent latitude
     * @return the tangent latitude
     */
    public double getTangentLat() {
        return origin.getLatitude();
    }

    /**
     * Set the tangent latitude
     *
     * @param lat the tangent latitude
     */
    public void setTangentLat(double lat) {
        origin.setLatitude(lat);
        latt = Math.toRadians(lat);
        precalculate();
    }



    /**
     * Get the parameters as a String
     * @return the parameters as a String
     */
    public String paramsToString() {
        return " tangent " + origin.toString() + " scale: "
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
        return false;  // ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2);
    }



    /**
     * Clone this projection
     * @return a clone of this.
     */
    public Object clone() {
        Stereographic cl = (Stereographic) super.clone();
        cl.origin = new LatLonPointImpl(getTangentLat(), getTangentLon());
        return (Object) cl;
    }

    /**
     * Returns true if this represents the same Projection as proj.
     *
     * @param proj    projection in question
     * @return true if this represents the same Projection as proj.
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof Stereographic)) {
            return false;
        }

        Stereographic oo = (Stereographic) proj;
        return ((this.getScale() == oo.getScale())
                && (this.getTangentLat() == oo.getTangentLat())
                && (this.getTangentLon() == oo.getTangentLon())
                && this.defaultMapArea.equals(oo.defaultMapArea));
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
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double toX, toY;
        double fromLat = latLon.getLatitude();
        double fromLon = latLon.getLongitude();


        double lat     = Math.toRadians(fromLat);
        double lon     = Math.toRadians(fromLon);
        // keep away from the singular point
        if ((Math.abs(lat + latt) <= TOLERANCE)) {
            lat = -latt * (1.0 - TOLERANCE);
        }

        double sdlon  = Math.sin(lon - lont);
        double cdlon  = Math.cos(lon - lont);
        double sinlat = Math.sin(lat);
        double coslat = Math.cos(lat);

        double k = 2.0 * scale
                   / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
        toX = k * coslat * sdlon;
        toY = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

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
        double phi, lam;

        double rho  = Math.sqrt(fromX * fromX + fromY * fromY);
        double c    = 2.0 * Math.atan2(rho, 2.0 * scale);
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
            lam = lont
                  + Math.atan2(fromX * sinc,
                               rho * coslatt * cosc - fromY * sinc * sinlatt);
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

            double lat     = Math.toRadians(fromLat);
            double lon     = Math.toRadians(fromLon);
            // keep away from the singular point
            if ((Math.abs(lat + latt) <= TOLERANCE)) {
                lat = -latt * (1.0 - TOLERANCE);
            }

            double sdlon  = Math.sin(lon - lont);
            double cdlon  = Math.cos(lon - lont);
            double sinlat = Math.sin(lat);
            double coslat = Math.cos(lat);

            double k = 2.0 * scale
                       / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
            toX         = k * coslat * sdlon;
            toY         = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

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
        double  phi, lam;
        double  toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double rho   = Math.sqrt(fromX * fromX + fromY * fromY);
            double c     = 2.0 * Math.atan2(rho, 2.0 * scale);
            double sinc  = Math.sin(c);
            double cosc  = Math.cos(c);

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

            toLon     = Math.toDegrees(lam);

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

            double lat     = Math.toRadians(fromLat);
            double lon     = Math.toRadians(fromLon);
            // keep away from the singular point
            if ((Math.abs(lat + latt) <= TOLERANCE)) {
                lat = -latt * (1.0 - TOLERANCE);
            }

            double sdlon  = Math.sin(lon - lont);
            double cdlon  = Math.cos(lon - lont);
            double sinlat = Math.sin(lat);
            double coslat = Math.cos(lat);

            double k = 2.0 * scale
                       / (1.0 + sinlatt * sinlat + coslatt * coslat * cdlon);
            toX         = k * coslat * sdlon;
            toY         = k * (coslatt * sinlat - sinlatt * coslat * cdlon);

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
        double   phi, lam;
        double   toLat, toLon;
        for (int i = 0; i < cnt; i++) {
            double fromX = fromXA[i];
            double fromY = fromYA[i];

            double rho   = Math.sqrt(fromX * fromX + fromY * fromY);
            double c     = 2.0 * Math.atan2(rho, 2.0 * scale);
            double sinc  = Math.sin(c);
            double cosc  = Math.cos(c);

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

            toLon     = Math.toDegrees(lam);

            toLatA[i] = (double) toLat;
            toLonA[i] = (double) toLon;
        }
        return to;
    }

    /*ENDGENERATED*/

}

/* Change History:
   $Log: Stereographic.java,v $
   Revision 1.24  2005/11/02 20:04:14  dmurray
   add the Orthographic projection, refactor some of the constants up to
   ProjectionImpl, move the radius declaration in Earth up to the top,
   fix a problem in Mercator where infinite points were set to 0,0 instead
   of infinity.

   Revision 1.23  2005/05/27 00:32:45  caron
   bug in polar stereographic projection, esp Grib IOSP

   Revision 1.22  2005/05/13 18:29:19  jeffmc
   Clean up the odd copyright symbols

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

   Revision 1.16  2004/01/29 17:35:01  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.15  2003/07/12 23:08:59  caron
   add cvs headers, trailers

*/








