/*
 * $Id: UtmProjection.java,v 1.6 2006/11/18 19:03:23 dmurray Exp $
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


/**
 * Universal Transverse Mercator.
 * Ellipsoidal earth.
 *
 * Origin of coordinate system is reletive to the point where the
 * central meridian and the equator cross.
 * This point has x,y value = (500, 0) km for north hemisphere.
 * and (500, 10,0000) km for south hemisphere.
 * Increasing values always go north and east.
 *
 * The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
 *
 * @author John Caron
 * @version $Id: UtmProjection.java,v 1.6 2006/11/18 19:03:23 dmurray Exp $
 */


public class UtmProjection extends ProjectionImpl {

    /** _more_ */
    private Utm_To_Gdc_Converter convert2latlon;

    /** _more_ */
    private Gdc_To_Utm_Converter convert2xy;

    /**
     *  Constructor with default parameters
     */
    public UtmProjection() {
        this(5, true);
    }


    /**
     * Constructor with default WGS 84 ellipsoid.
     * @param zone             the UTM zone number (1-60)
     * @param isNorth true if the UTM coordinate is in the northern hemisphere
     */
    public UtmProjection(int zone, boolean isNorth) {
        convert2latlon = new Utm_To_Gdc_Converter(zone, isNorth);
        convert2xy     = new Gdc_To_Utm_Converter(zone, isNorth);

        addParameter(ATTR_NAME, "universal_transverse_mercator");
        addParameter("semi-major_axis", convert2latlon.getA());
        addParameter("inverse_flattening", convert2latlon.getF());
        addParameter("UTM_zone", zone);
        addParameter("north_hemisphere", isNorth
                                         ? "true"
                                         : "false");
    }

    /**
     * Construct a Universal Transverse Mercator Projection.
     *
     * @param a                the semi-major axis (meters) for the ellipsoid
     * @param f                the inverse flattening for the ellipsoid
     * @param zone             the UTM zone number (1-60)
     * @param isNorth true if the UTM coordinate is in the northern hemisphere
     */
    public UtmProjection(double a, double f, int zone, boolean isNorth) {
        convert2latlon = new Utm_To_Gdc_Converter(a, f, zone, isNorth);
        convert2xy     = new Gdc_To_Utm_Converter(a, f, zone, isNorth);

        addParameter(ATTR_NAME, "universal_transverse_mercator");
        addParameter("semi-major_axis", a);
        addParameter("inverse_flattening", f);
        addParameter("UTM_zone", zone);
        addParameter("north_hemisphere", isNorth
                                         ? "true"
                                         : "false");
    }

    /**
     * Get the zone number = [1,60]
     *
     * @return _more_
     */
    public int getZone() {
        return convert2latlon.getZone();
    }


    /**
     * Set the zone number = [1,60]
     *
     *
     * @param newZone _more_
     */
    public void setZone(int newZone) {
        convert2latlon = new Utm_To_Gdc_Converter(convert2latlon.getA(),
                convert2latlon.getF(), newZone, convert2latlon.isNorth());
        convert2xy = new Gdc_To_Utm_Converter(convert2latlon.getA(),
                convert2latlon.getF(), convert2latlon.getZone(),
                convert2latlon.isNorth());
    }

    /**
     * Get whether in North or South Hemisphere.
     *
     * @return _more_
     */
    public boolean isNorth() {
        return convert2latlon.isNorth();
    }


    /**
     * Set whether in North or South Hemisphere.
     *
     *
     * @param newNorth _more_
     */
    public void setNorth(boolean newNorth) {
        convert2latlon = new Utm_To_Gdc_Converter(convert2latlon.getA(),
                convert2latlon.getF(), convert2latlon.getZone(), newNorth);
        convert2xy = new Gdc_To_Utm_Converter(convert2latlon.getA(),
                convert2latlon.getF(), convert2latlon.getZone(),
                convert2latlon.isNorth());
    }


    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "Universal transverse mercator";
    }


    /**
     * Get the parameters as a String
     *
     * @return the parameters as a String
     */
    public String paramsToString() {
        return getZone() + " " + isNorth();
    }

    /**
     * Does the line between these two points cross the projection "seam".
     *
     * @param pt1 the line goes between these two points
     * @param pt2 the line goes between these two points
     * @return false if there is no seam
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        return false;
    }


    /**
     * Clone this projection
     *
     * @return a clone of this.
     *
     * public Object clone() {
     * TransverseMercator cl = (TransverseMercator) super.clone();
     * cl.origin = new LatLonPointImpl(getOriginLat(), getTangentLon());
     * return (Object) cl;
     * }
     *
     *
     * Returns true if this represents the same Projection as proj.
     *
     * @param proj projection in question
     * @return true if this represents the same Projection as proj.
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof UtmProjection)) {
            return false;
        }

        return true;
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
        double fromLat = latLon.getLatitude();
        double fromLon = latLon.getLongitude();

        return convert2xy.latLonToProj(fromLat, fromLon, result);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     *
     * @return _more_
     */
    public double[][] latLonToProj(double[][] from, double[][] to,
                                   int latIndex, int lonIndex) {
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "from array not same length as to array");
        }

        return convert2xy.latLonToProj(from, to, latIndex, lonIndex);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     *
     * @return _more_
     */
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException("ProjectionImpl.latLonToProj:"
                    + "from array not same length as to array");
        }

        return convert2xy.latLonToProj(from, to, latIndex, lonIndex);
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
        return convert2latlon.projToLatLon(world.getX(), world.getY(),
                                           result);
    }

    /**
     *  Convert projection coordinates to lat/lon coordinate.
     *
     *  @param from    array of projection coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (x, y) coordinate
     *                 of the ith point
     *  @param to      resulting array of lat/lon coordinates: to[2][n] where
     *                 (to[0][i], to[1][i]) is the (lat, lon) coordinate of
     *                 the ith point
     *
     *  @return the "to" array
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "from array not same length as to array");
        }

        return convert2latlon.projToLatLon(from, to);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     *
     * @return _more_
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException("ProjectionImpl.projToLatLon:"
                    + "from array not same length as to array");
        }

        return convert2latlon.projToLatLon(from, to);
    }

}

/*
 *  Change History:
 *  $Log: UtmProjection.java,v $
 *  Revision 1.6  2006/11/18 19:03:23  dmurray
 *  jindent
 *
 *  Revision 1.5  2005/08/11 22:42:12  dmurray
 *  jindent (I'll leave the javadoc to those who forgot to)
 *
 *  Revision 1.4  2005/05/13 18:29:19  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.3  2005/05/13 12:26:51  jeffmc
 *  Some mods
 *
 *  Revision 1.2  2005/05/13 11:14:11  jeffmc
 *  Snapshot
 *
 *  Revision 1.1  2005/02/01 01:35:50  caron
 *  no message
 *
 */










