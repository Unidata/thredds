/*
 * $Id:ProjectionImpl.java 63 2006-07-12 21:50:51Z edavis $
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

package ucar.unidata.geoloc;


import ucar.unidata.geoloc.projection.LatLonProjection;

import ucar.unidata.util.*;

import java.util.*;


/**
 * Superclass for our implementations of geoloc.Projection.
 *
 * <p>All subclasses must: <ul>
 * <li> override clone() if they have non primitive fields
 * <li> override equals() and return true when all parameters are equal
 * <li> create "atts" list of parameters as string-valued Attribute pairs
 * <li> implement abstract methods
 * <li> follow bean conventions:
 *  <ol>
 *   <li> must have a default constructor with no arguments (use default
 *         values for the paramters)
 *   <li> all parameters should have getXXXX() and setXXXX() bean property methods.
 *   </ol>
 * </ul>
 *
 *  If possible, set defaultmapArea to some reasonable world coord bounding box
 *  otherwise, provide a way for the user to specify it when a specific projection
 *  is created.
 *
 * <p> Note on "false_easting" and "fale_northing" projection parameters:
 * <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * </ul>
 *
 * @see Projection
 * @author John Caron
 * @version $Revision:63 $
 */
public abstract class ProjectionImpl
        implements Projection, Cloneable, java.io.Serializable {

    /** Projection Name */
    public static final String ATTR_NAME = "Projection_Name";

    /** Earth radius in kilometers */
    static public final double EARTH_RADIUS = Earth.getRadius() * .001;  // km

    // package private, i hope

    /** Latitude index */
    public static final int INDEX_LAT = 0;

    /** Longitude index */
    public static final int INDEX_LON = 1;

    /** X index */
    public static final int INDEX_X = 0;

    /** Y index */
    public static final int INDEX_Y = 1;

    /** tolerence for checks */
    protected static final double TOLERANCE = 1.0e-6;

    /** PI */
    public static final double PI = Math.PI;

    /** PI/2 */
    public static final double PI_OVER_2 = Math.PI / 2.0;

    /** PI/4 */
    public static final double PI_OVER_4 = Math.PI / 4.0;

    /** name of this projection */
    protected String name = "";

    /** flag for latlon */
    protected boolean isLatLon = false;

    /** list of attributes */
    protected ArrayList atts = new ArrayList();

    /** default map area */
    protected ProjectionRect defaultMapArea = new ProjectionRect();

    /**
     * Get the name of the type of the projection.
     * @return the class name
     */
    public String getClassName() {
        String className = getClass().getName();
        int    index     = className.lastIndexOf(".");
        if (index >= 0) {
            className = className.substring(index + 1);
        }
        return className;
    }



    /**
     * Get a string representation of the projection parameters
     * @return string representation of the projection parameters
     */
    public abstract String paramsToString();


    /**
     * Get the label to be used in the gui for this type of projection.
     * This defaults to call getClassName
     *
     * @return Type label
     */

    public String getProjectionTypeLabel() {
        return getClassName();
    }


    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latlon convert from these lat, lon coordinates
     * @param destPoint the object to write to
     *
     * @return the given destPoint
     */
    public abstract ProjectionPoint latLonToProj(LatLonPoint latlon,
            ProjectionPointImpl destPoint);


    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param ppt convert from these projection coordinates
     * @param destPoint the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public abstract LatLonPoint projToLatLon(ProjectionPoint ppt,
                                             LatLonPointImpl destPoint);


    /**
     * Convert a LatLonPoint to projection coordinates
     * Note: a new object is not created on each call for the return value.
     *
     * @param latLon convert from these lat, lon coordinates
     *
     * @return ProjectionPoint convert to these projection coordinates
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon) {
        return latLonToProj(latLon, workP);
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     * Note: a new object is not created on each call for the return value.
     *
     * @param ppt convert from these projection coordinates
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint ppt) {
        return projToLatLon(ppt, workL);
    }

    /**
     * Does the line between these two points cross the projection "seam".
     *
     * @param pt1  the line goes between these two points
     * @param pt2  the line goes between these two points
     *
     * @return false if there is no seam
     */
    public abstract boolean crossSeam(ProjectionPoint pt1,
                                      ProjectionPoint pt2);

    /**
     * Returns true if this represents the same Projection as proj.
     *
     * @param proj    projection in question
     * @return true if this represents the same Projection as proj.
     */
    public abstract boolean equals(Object proj);

    /**
     * Get the name of this specific projection (also see getClassName)
     * @return name of the projection
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this specific projection.
     *
     * @param name   name for this projection
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get parameters as list of ucar.unidata.util.Parameter
     * @return List of parameters
     */
    public List getProjectionParameters() {
        return atts;
    }

    /**
     * Add an attribute to this projection
     *
     * @param name   name of the attribute
     * @param value  attribute value as a string
     */
    protected void addParameter(String name, String value) {
        atts.add(new Parameter(name, value));
    }

    /**
     * Add an attribute to this projection
     *
     * @param name   name of the attribute
     * @param value  attribute value as a double
     */
    protected void addParameter(String name, double value) {
        atts.add(new Parameter(name, value));
    }

    /**
     * Add an attribute to this projection
     *
     * @param p specify as a Parameter
     */
    protected void addParameter(Parameter p) {
        atts.add(p);
    }

    /**
     * Is this the lat/lon Projection ?
     * @return true if it is the lat/lon Projection
     */
    public boolean isLatLon() {
        return isLatLon;
    }

    // all of this is to get a human readable string with nice formatting

    /** header */
    private static String header = null;

    /**
     * Get a header for display.
     * @return human readable header for display
     */
    public static String getHeader() {
        if (header == null) {
            StringBuffer headerB = new StringBuffer(60);
            headerB.append("Name");
            Format.tab(headerB, 20, true);
            headerB.append("Class");
            Format.tab(headerB, 40, true);
            headerB.append("Parameters");
            header = headerB.toString();
        }
        return header;
    }



    /**
     * Get a String representation of this projection.
     * @return the name of the projection.  This is what gets
     *         displayed when you add the projection object to
     *         a UI widget (e.g. label, combobox)
     */
    public String toString() {
        return getName();
    }

    // working variables for use by subclasses, to minimize object creation, gc

    /** working point for projection */
    protected ProjectionPointImpl workP = new ProjectionPointImpl();

    /** working point for lat/lon */
    protected LatLonPointImpl workL = new LatLonPointImpl();

    /**
     * Clone this projection
     * @return a clone of this.
     */
    public Object clone() {
        ProjectionImpl p;
        try {
            p = (ProjectionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        p.name           = new String(name);
        workP            = new ProjectionPointImpl();
        workL            = new LatLonPointImpl();
        p.defaultMapArea = new ProjectionRect(defaultMapArea);
        return p;
    }

    /**
     * Get a reasonable bounding box for this projection.
     * @return reasonable bounding box
     */
    public ProjectionRect getDefaultMapArea() {
        return defaultMapArea;
    }

    /**
     * Get the bounding box in lat/lon.
     *
     * @return the LatLonRectangle for the bounding box
     */
    public LatLonRect getDefaultMapAreaLL() {
        return projToLatLonBB(defaultMapArea);
    }

    /**
     * Set a reasonable bounding box for this specific projection.
     * Projections are typically specific to an area of the world;
     * theres no bounding box that works for all projections.
     *
     * @param bb  bounding box
     */
    public void setDefaultMapArea(ProjectionRect bb) {
        defaultMapArea = (ProjectionRect) bb.clone();
    }

    //////// convenience routines

    /**
     * Convert a LatLonPoint to projection coordinates
     * Note: a new object is not created on each call for the return value.
     *
     * @param lat latitude of point to convert
     * @param lon longitude of point to convert
     *
     * @return ProjectionPointImpl convert to these projection coordinates
     */
    public ProjectionPointImpl latLonToProj(double lat, double lon) {
        workL.setLatitude(lat);
        workL.setLongitude(lon);
        return (ProjectionPointImpl) latLonToProj(workL);
    }

    /**
     * Convert a projection coordinate to a LatLonPoint
     * Note: a new object is not created on each call for the return value.
     *
     * @param x x value to convert
     * @param y y value to convert
     *
     * @return LatLonPointImpl convert to these lat/lon coordinates
     */
    public LatLonPointImpl projToLatLon(double x, double y) {
        workP.setLocation(x, y);
        return (LatLonPointImpl) projToLatLon(workP);
    }


    ///////////////////////////////////////////////////////////////////////////////////
    // optimizations for doing double and float arrays

    /**
     * Convert projection coordinates to lat/lon coordinates.
     *
     * @param from   array of projection coordinates: from[2][n],
     *               where from[0][i], from[1][i] is the x, y coordinate
     *               of the ith point
     * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
     *         is the lat,lon coordinate of the ith point
     */
    public double[][] projToLatLon(double[][] from) {
        return projToLatLon(from, new double[2][from[0].length]);
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
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "from array not same length as to array");
        }

        for (int i = 0; i < from[0].length; i++) {
            LatLonPoint endL = projToLatLon(from[0][i], from[1][i]);
            to[0][i] = endL.getLatitude();
            to[1][i] = endL.getLongitude();
        }

        return to;
    }

    /**
     * Convert projection coordinates to lat/lon coordinates.
     *
     * @param from   array of projection coordinates: from[2][n],
     *               where from[0][i], from[1][i] is the x, y coordinate
     *               of the ith point
     * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
     *         is the lat,lon coordinate of the ith point
     */
    public float[][] projToLatLon(float[][] from) {
        return projToLatLon(from, new float[2][from[0].length]);
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
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException(
                "ProjectionImpl.projToLatLon:"
                + "from array not same length as to array");
        }

        ProjectionPointImpl ppi  = new ProjectionPointImpl();
        LatLonPointImpl     llpi = new LatLonPointImpl();

        for (int i = 0; i < from[0].length; i++) {
            ppi.setLocation((double) from[0][i], (double) from[1][i]);
            projToLatLon(ppi, llpi);
            to[0][i] = (float) llpi.getLatitude();
            to[1][i] = (float) llpi.getLongitude();
        }

        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     *
     * @return resulting array of projection coordinates, where to[0][i],
     *         to[1][i] is the (x,y) coordinate of the ith point
     */
    public double[][] latLonToProj(double[][] from) {
        return latLonToProj(from, new double[2][from[0].length]);
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
    public double[][] latLonToProj(double[][] from, double[][] to) {
        return latLonToProj(from, to, INDEX_LAT, INDEX_LON);
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
     *                 coordinate of the ith point
     * @param latIndex index of lat coordinate; must be 0 or 1
     * @param lonIndex index of lon coordinate; must be 0 or 1
     *
     * @return resulting array of projection coordinates: to[2][n] where
     *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
     */
    public double[][] latLonToProj(double[][] from, int latIndex,
                                   int lonIndex) {
        return latLonToProj(from, new double[2][from[0].length], latIndex,
                            lonIndex);
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
        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "from array not same length as to array");
        }

        ProjectionPointImpl ppi  = new ProjectionPointImpl();
        LatLonPointImpl     llpi = new LatLonPointImpl();

        for (int i = 0; i < from[0].length; i++) {
            llpi.setLatitude(from[latIndex][i]);
            llpi.setLongitude(from[lonIndex][i]);
            latLonToProj(llpi, ppi);
            to[0][i] = ppi.getX();
            to[1][i] = ppi.getY();
        }
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     *
     * @return resulting array of projection coordinates, where to[0][i],
     *         to[1][i] is the (x,y) coordinate of the ith point
     */
    public float[][] latLonToProj(float[][] from) {
        return latLonToProj(from, new float[2][from[0].length]);
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
    public float[][] latLonToProj(float[][] from, float[][] to) {
        return latLonToProj(from, to, INDEX_LAT, INDEX_LON);
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
     *                 coordinate of the ith point
     * @param latIndex index of lat coordinate; must be 0 or 1
     * @param lonIndex index of lon coordinate; must be 0 or 1
     *
     * @return resulting array of projection coordinates: to[2][n] where
     *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
     */
    public float[][] latLonToProj(float[][] from, int latIndex,
                                  int lonIndex) {
        return latLonToProj(from, new float[2][from[0].length], latIndex,
                            lonIndex);
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
        //      ucar.unidata.util.Misc.printStack ("latLonToProj-" + this + " size=" + from[0].length, 4, null);

        if ((from == null) || (from.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "null array argument or wrong dimension (from)");
        }
        if ((to == null) || (to.length != 2)) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "null array argument or wrong dimension (to)");
        }

        if (from[0].length != to[0].length) {
            throw new IllegalArgumentException(
                "ProjectionImpl.latLonToProj:"
                + "from array not same length as to array");
        }

        ProjectionPointImpl ppi  = new ProjectionPointImpl();
        LatLonPointImpl     llpi = new LatLonPointImpl();

        for (int i = 0; i < from[0].length; i++) {
            llpi.setLatitude(from[latIndex][i]);
            llpi.setLongitude(from[lonIndex][i]);
            latLonToProj(llpi, ppi);
            to[0][i] = (float) ppi.getX();
            to[1][i] = (float) ppi.getY();
        }

        return to;
    }


    /*

    public double[][] latLonToProj (double[][]from) {
    if (from == null || from.length != 2)
    throw new IllegalArgumentException("ProjectionImpl.latLonToProj:" +
    "null array argument or wrong dimension ");
    return latLonToProj (from, new double[2][from[0].length]);
    }

    public double[][] latLonToProj (double[][]from, double[][]to) { return to; }


    /* protected double[][] latLonToProj (double[][]from, double[][]to, int latIndex, int lonIndex) { return to; }

    protected double[][] latLonToProj (double[][]from, int latIndex, int lonIndex) {
    if (from == null || from.length != 2)
    throw new IllegalArgumentException("ProjectionImpl.latLonToProj:" +
    "null array argument or wrong dimension ");
    //      return latLonToProj (from, new double[2][from[0].length], latIndex, lonIndex);
    return latLonToProj (from, from, latIndex, lonIndex);
    }

    */

    // bounding box utilities

    /** working projection point 1 */
    private ProjectionPointImpl w1 = new ProjectionPointImpl();

    /** working projection point 2 */
    private ProjectionPointImpl w2 = new ProjectionPointImpl();

    /** working projection point 3 */
    private ProjectionPointImpl w3 = new ProjectionPointImpl();

    /** working projection point 4 */
    private ProjectionPointImpl w4 = new ProjectionPointImpl();

    ;

    /**
     * Convert a lat/lon bounding box to a world coordinate bounding box,
     * by finding the minimum enclosing box.
     *
     * @param latlonRect input lat,lon bounding box
     * @return  minimum enclosing box in world coordinates.
     */
    public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
        if (isLatLon) {
            LatLonProjection llp = (LatLonProjection) this;
            llp.setCenterLon(latlonRect.getCenterLon());
        }

        LatLonPoint ll = latlonRect.getLowerLeftPoint();
        LatLonPoint ur = latlonRect.getUpperRightPoint();
        latLonToProj(ll, w1);
        latLonToProj(ur, w2);

        if ( !isLatLon && crossSeam(w1, w2)) {
            System.out.println("CROSS SEAM !" + w1 + " " + w2);
        }

        // make bounding box out of those two corners
        ProjectionRect world = new ProjectionRect(w1.getX(), w1.getY(),
                                                  w2.getX(), w2.getY());

        LatLonPointImpl la = new LatLonPointImpl();
        LatLonPointImpl lb = new LatLonPointImpl();

        // now extend if needed to the other two corners
        la.setLatitude(ur.getLatitude());
        la.setLongitude(ll.getLongitude());
        latLonToProj(la, w1);
        world.add(w1);

        lb.setLatitude(ll.getLatitude());
        lb.setLongitude(ur.getLongitude());
        latLonToProj(lb, w2);
        world.add(w2);

        return world;
    }

    /**
     * Convert a world coordinate bounding box to a lat/lon bounding box,
     * by finding the minimum enclosing box.
     * @param world input world coordinate bounding box
     * @return  minimum enclosing box in lat,lon coordinates.
     */
    public LatLonRect projToLatLonBB(ProjectionRect world) {
        ProjectionPoint min = world.getMinPoint();
        ProjectionPoint max = world.getMaxPoint();
        LatLonRect      llbb;
        LatLonPointImpl llmin = new LatLonPointImpl();
        LatLonPointImpl llmax = new LatLonPointImpl();

        // make bounding box out of the min, max corners
        projToLatLon(min, llmin);
        projToLatLon(max, llmax);
        llbb = new LatLonRect(llmin, llmax);

        /*
        double lona = la.getLongitude();
        double lonb = lb.getLongitude();

        if (((lona < lonb) && (lonb - lona <= 180.0))
                || ((lona > lonb) && (lona - lonb >= 180.0))) {
            llbb = new LatLonRect(la, lb);
        } else {
            llbb = new LatLonRect(lb, la);
        } */

        // now extend if needed using the other two corners
        w1.setLocation(min.getX(), max.getY());
        projToLatLon(w1, llmin);
        llbb.extend(llmin);

        w2.setLocation(max.getX(), min.getY());
        projToLatLon(w2, llmax);
        llbb.extend(llmax);

        return llbb;
    }


    /*
     * public static void main (String[] args) {
     * int numRows = 2;
     * int numCols = 100000;
     *
     * //      double [][] data = new double[numRows][numCols];
     * float [][] data = new float[numRows][numCols];
     *
     *
     * for (int row = 0; row<numRows; row++) {
     *   for (int i=0;i<numCols;i++) {
     *       data[row][i] = 20;
     *   }
     * }
     *
     *  ProjectionImpl []projs = new ProjectionImpl[]{new LambertConformal (),
     *  new LatLonProjection (),
     *  new Stereographic (),
     *  new TransverseMercator ()};
     *
     *  //   projs = new ProjectionImpl[]{new TransverseMercator(),new TransverseMercator(),new TransverseMercator()};
     *
     *  for (int i=0;i<projs.length;i++) {
     *  ProjectionImpl proj =  projs[i];
     *  proj.projToLatLon (1.0,2.0);
     *  }
     *
     *
     *  System.out.println ("<table><tr>" +
     *  col ("Projection") +
     *  col ("super.toLatLon") +
     *  col ("toLatLon") +
     *  col ("super.toProj") +
     *  col ("toProj") +
     *  "</tr>");
     *
     *
     *  int cnt = 0;
     *  long t1;
     *
     *  //   while (cnt < 1000) {    }
     *
     *
     *  for (int i=0;i<projs.length;i++) {
     *  ProjectionImpl proj =  projs[i];
     *  long d1 = 0, d2=0, d3=0, d4=0;
     *  for (int x=0;x<2;x++) {
     *
     *
     *
     *  start ();
     *  proj.projToLatLon (data);
     *  d3 += elapsed ();
     *  start ();
     *  proj.latLonToProj (data);
     *  d4 += elapsed ();
     *
     *  start ();
     *  proj.projToLatLon (data);
     *  d1 += elapsed ();
     *  start ();
     *  proj.latLonToProj (data);
     *  d2 += elapsed ();
     *
     *
     *
     *
     *  }
     *  System.out.println ("<tr>" + col (getName(proj.getClass().getName ())) +
     *  col (d1) +
     *  col (d3) +
     *  col (d2) +
     *  col (d4) +
     *  "</tr>"
     *  );
     *  }
     *  System.out.println ("</table>");
     *
     * }
     *
     * static long t1;
     * static void start () {
     * gc ();
     * t1 = System.currentTimeMillis ();
     * }
     * static long elapsed () {
     * return System.currentTimeMillis ()-t1;
     * }
     * static String col (double d) {
     * return "<td>"+d+"</td>";
     * }
     * static String col (String d) {
     * return "<td>"+d+"</td>";
     * }
     *
     * public static void gc () {
     * Thread t =  Thread.currentThread ();
     * for (int i=0;i<5;i++) {
     *   Runtime.getRuntime ().gc ();
     *   try {t.sleep (10);} catch  (Exception exc) {}
     * }
     *
     * }
     *
     *
     * static String getName (String f) {
     * int idx = f.lastIndexOf (".");
     * return f.substring (idx+1);
     * }
     */

}
