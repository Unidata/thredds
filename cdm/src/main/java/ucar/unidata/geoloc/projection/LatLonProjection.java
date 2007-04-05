/*
 * $Id: LatLonProjection.java,v 1.31 2006/11/18 19:03:22 dmurray Exp $
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
 *  This is the "fake" identity projection where world coord = latlon coord.
 *  Topologically its the same as a cylinder tangent to the earth at the equator.
 *  The cylinder is cut at the "seam" = centerLon +- 180.
 *  Longitude values are always kept in the range [centerLon +-180]
 *
 *   @see ProjectionImpl
 *   @author John Caron
 *   @version $Id: LatLonProjection.java,v 1.31 2006/11/18 19:03:22 dmurray Exp $
 */


public class LatLonProjection extends ProjectionImpl {

    /** center longitude */
    private double centerLon = 0.0;

    /** copy constructor - avoid clone !! */
    public ProjectionImpl constructCopy() {
      return new LatLonProjection(getName(), getDefaultMapArea());
    }

    /**
     * Default constructor
     */
    public LatLonProjection() {
        this("");
    }

    /**
     * Create a new LatLonProjection
     *
     * @param name   name of projection
     *
     */
    public LatLonProjection(String name) {
        this(name, new ProjectionRect(-180, -90, 180, 90));
    }

    /**
     * Create a new LatLonProjection
     *
     * @param name   name of projection
     * @param defaultMapArea   bounding box
     */
    public LatLonProjection(String name, ProjectionRect defaultMapArea) {
        addParameter(ATTR_NAME, "LatLon");
        this.name           = name;
        isLatLon            = true;
        this.defaultMapArea = defaultMapArea;
    }



    /**
     * Get the class name
     * @return class name
     */
    public String getClassName() {
        return "LatLon";
    }


    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "Lat/Lon";
    }

    /**
     * Get a String of the parameters
     * @return a String of the parameters
     */
    public String paramsToString() {
        return "Center lon:" + Format.d(centerLon, 3);
    }



    /**
     * See if this projection equals the object in question
     *
     * @param p  object in question
     *
     * @return true if it is a LatLonProjection and covers the same area
     */
    public boolean equals(Object p) {
        if ( !(p instanceof LatLonProjection)) {
            return false;
        }
        LatLonProjection that = (LatLonProjection) p;
        return that.defaultMapArea.equals(this.defaultMapArea)
               && (Double.doubleToLongBits(that.centerLon)
                   == Double.doubleToLongBits(this.centerLon));
    }

    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latlon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latlon,
                                        ProjectionPointImpl result) {
        result.setLocation(LatLonPointImpl.lonNormal(latlon.getLongitude(),
                centerLon), latlon.getLatitude());
        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world  convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        result.setLongitude(world.getX());
        result.setLatitude(world.getY());
        return result;
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

        float[] fromX = from[INDEX_X];
        float[] fromY = from[INDEX_Y];
        to[INDEX_LAT] = fromY;
        to[INDEX_LON] = fromX;
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
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        int     cnt     = from[0].length;
        float[] toX     = to[INDEX_X];
        float[] toY     = to[INDEX_Y];
        float[] fromLat = from[latIndex];
        float[] fromLon = from[lonIndex];
        float   lat, lon;
        for (int i = 0; i < cnt; i++) {
            lat = fromLat[i];
            lon = (float) (centerLon
                           + Math.IEEEremainder(fromLon[i] - centerLon,
                               360.0));
            toX[i] = lon;
            toY[i] = lat;
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

        double[] fromX = from[INDEX_X];
        double[] fromY = from[INDEX_Y];
        to[INDEX_LAT] = fromY;
        to[INDEX_LON] = fromX;
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
        int      cnt     = from[0].length;
        double[] toX     = to[INDEX_X];
        double[] toY     = to[INDEX_Y];
        double[] fromLat = from[latIndex];
        double[] fromLon = from[lonIndex];
        double   lat, lon;
        for (int i = 0; i < cnt; i++) {
            lat = fromLat[i];
            lon = centerLon
                  + Math.IEEEremainder(fromLon[i] - centerLon, 360.0);
            toX[i] = lon;
            toY[i] = lat;
        }
        return to;
    }


    /**
     * Clone this projection
     * @return a clone of this projection
     */
    public Object clone() {
        LatLonProjection pp = (LatLonProjection) super.clone();
        pp.centerLon = centerLon;
        return (Object) pp;
    }


    /**
     *  Set the center of the Longitude range. It is normalized to  +/- 180.
     *  The cylinder is cut at the "seam" = centerLon +- 180.
     *  Use this to keep the Longitude values kept in the range [centerLon +-180], which
     *  makes seam handling easier.
     *
     * @param centerLon the center of the Longitude range.
     * @return centerLon normalized to +/- 180.
     */
    public double setCenterLon(double centerLon) {
        this.centerLon = LatLonPointImpl.lonNormal(centerLon);
        return this.centerLon;
    }

    /**
     *  Get the center of the Longitude range. It is normalized to  +/- 180.
     * @return the center longitude
     */
    public double getCenterLon() {
        return centerLon;
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
        return Math.abs(pt1.getX() - pt2.getX()) > 270.0;  // ?? LOOK: do I believe this
    }


    /**
     * Set a reasonable bounding box for this projection.
     *
     * @param bb   a reasonable bounding box
     */
    public void setDefaultMapArea(ProjectionRect bb) {
        super.setDefaultMapArea(bb);
        this.centerLon = bb.getCenterX();
        // setCenterLon( bb.getCenterX());
    }

    /**
     * Split a latlon rectangle to the equivalent ProjectionRect
     * using this LatLonProjection to split it at the seam if needed.
     *
     * @param latlonR the latlon rectangle to transform
     * @return 1 or 2 ProjectionRect. If it doesnt cross the seam,
     *  the second rectangle is null.
     */
    public ProjectionRect[] latLonToProjRect(LatLonRect latlonR) {

        double lat0 = latlonR.getLowerLeftPoint().getLatitude();
        double height = Math.abs(latlonR.getUpperRightPoint().getLatitude()
                                 - lat0);
        double width = latlonR.getWidth();
        double lon0 = LatLonPointImpl.lonNormal(
                          latlonR.getLowerLeftPoint().getLongitude(),
                          centerLon);
        double lon1 = LatLonPointImpl.lonNormal(
                          latlonR.getUpperRightPoint().getLongitude(),
                          centerLon);

        if (lon0 < lon1) {
            projR1.setRect(lon0, lat0, width, height);
            rects[0] = projR1;
            rects[1] = null;
        } else {
            double y = centerLon + 180 - lon0;
            projR1.setRect(lon0, lat0, y, height);
            projR2.setRect(lon1 - width + y, lat0, width - y, height);
            rects[0] = projR1;
            rects[1] = projR2;
        }

        return rects;
    }

    /** projection rect 1 */
    private ProjectionRect projR1 = new ProjectionRect();

    /** projection rect 1 */
    private ProjectionRect projR2 = new ProjectionRect();

    /** array fo rects */
    private ProjectionRect rects[] = new ProjectionRect[2];

    /**
     * Create a latlon rectangle and split it into the equivalent
     * ProjectionRect using this LatLonProjection. The latlon rect is
     * constructed from 2 lat/lon points. The lon values are considered
     * coords in the latlonProjection, and so do not have to be +/- 180.
     *
     * @param lat0    lat of point 1
     * @param lon0    lon of point 1
     * @param lat1    lat of point 1
     * @param lon1    lon of point 1
     * @return 1 or 2 ProjectionRect. If it doesnt cross the seam,
     *         the second rectangle is null.
     */
    public ProjectionRect[] latLonToProjRect(double lat0, double lon0,
                                             double lat1, double lon1) {

        double height = Math.abs(lat1 - lat0);
        lat0 = Math.min(lat1, lat0);
        double width = lon1 - lon0;
        if (width < 1.0e-8) {
            width = 360.0;  // assume its the whole thing
        }
        lon0 = LatLonPointImpl.lonNormal(lon0, centerLon);
        lon1 = LatLonPointImpl.lonNormal(lon1, centerLon);

        if (width >= 360.0) {
            projR1.setRect(centerLon - 180.0, lat0, 360.0, height);
            rects[0] = projR1;
            rects[1] = null;
        } else if (lon0 < lon1) {
            projR1.setRect(lon0, lat0, width, height);
            rects[0] = projR1;
            rects[1] = null;
        } else {
            double y = centerLon + 180 - lon0;
            projR1.setRect(lon0, lat0, y, height);
            projR2.setRect(lon1 - width + y, lat0, width - y, height);
            rects[0] = projR1;
            rects[1] = projR2;
        }
        return rects;
    }

}

/*
 *  Change History:
 *  $Log: LatLonProjection.java,v $
 *  Revision 1.31  2006/11/18 19:03:22  dmurray
 *  jindent
 *
 *  Revision 1.30  2006/11/16 00:17:02  caron
 *  add FlatEarth projection (used by Nids grids)
 *
 *  Revision 1.29  2006/08/18 17:07:39  dmurray
 *  fix a problem with the projToLatLon and latlonToProj methods that
 *  take arrays.  They were assuming that from and to were different, but
 *  the could be the same to save on memory.
 *
 *  Revision 1.28  2005/05/13 18:29:18  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.27  2005/05/13 12:26:51  jeffmc
 *  Some mods
 *
 *  Revision 1.26  2005/05/13 11:14:10  jeffmc
 *  Snapshot
 *
 *  Revision 1.25  2004/09/22 21:19:52  caron
 *  use Parameter, not Attribute; remove nc2 dependencies
 *
 *  Revision 1.24  2004/07/30 17:22:20  dmurray
 *  Jindent and doclint
 *
 *  Revision 1.23  2004/02/27 21:21:40  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.22  2004/01/29 17:35:00  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.21  2003/07/12 23:08:59  caron
 *  add cvs headers, trailers
 *
 */
