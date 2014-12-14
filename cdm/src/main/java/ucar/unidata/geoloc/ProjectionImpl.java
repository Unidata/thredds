/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc;

import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.*;

import java.util.*;

/**
 * Superclass for our implementations of geoloc.Projection.
 * <p/>
 * <p>All subclasses must: <ul>
 * <li> override equals() and return true when all parameters are equal
 * <li> create "atts" list of parameters as string-valued Attribute pairs
 * <li> implement abstract methods
 * </ul>
 * <p/>
 * If possible, set defaultmapArea to some reasonable world coord bounding box
 * otherwise, provide a way for the user to specify it when a specific projection
 * is created.
 * <p/>
 * <p> Note on "false_easting" and "fale_northing" projection parameters:
 * <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * </ul>
 *
 * @author John Caron
 * @see Projection
 */
public abstract class ProjectionImpl implements Projection, java.io.Serializable {
  /**
   * Earth radius in kilometers
   */
  static public final double EARTH_RADIUS = Earth.getRadius() * 0.001;  // km

  /**
   * Latitude index
   */
  public static final int INDEX_LAT = 0;

  /**
   * Longitude index
   */
  public static final int INDEX_LON = 1;

  /**
   * X index
   */
  public static final int INDEX_X = 0;

  /**
   * Y index
   */
  public static final int INDEX_Y = 1;

  /**
   * tolerence for checks
   */
  protected static final double TOLERANCE = 1.0e-6;

  /**
   * PI
   */
  public static final double PI = Math.PI;

  /**
   * PI/2
   */
  public static final double PI_OVER_2 = Math.PI / 2.0;

  /**
   * PI/4
   */
  public static final double PI_OVER_4 = Math.PI / 4.0;

  ///////////////////////////////////////////////////////////////////////

  /**
   * name of this projection.
   */
  protected String name;  // LOOK should be final, IDV needs setName()

  /**
   * flag for latlon
   */
  protected final boolean isLatLon;

  /**
   * list of attributes
   */
  protected final List<Parameter> atts = new ArrayList<>();

  /**
   * default map area
   */
  protected ProjectionRect defaultMapArea = new ProjectionRect();

  /**
   * copy constructor - avoid clone !!
   *
   * @return a copy of this Projection
   */
  abstract public ProjectionImpl constructCopy();

  protected ProjectionImpl(String name, boolean isLatLon) {
    this.name = name;
    this.isLatLon = isLatLon;
  }

  /**
   * Get the name of the type of the projection.
   *
   * @return the class name
   */
  public String getClassName() {
    String className = getClass().getName();
    int index = className.lastIndexOf(".");
    if (index >= 0) {
      className = className.substring(index + 1);
    }
    return className;
  }

  /**
   * Get a string representation of the projection parameters
   *
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
   * @param latlon    convert from these lat, lon coordinates
   * @param destPoint the object to write to
   * @return the given destPoint
   */
  public abstract ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint);


  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param ppt       convert from these projection coordinates
   * @param destPoint the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public abstract LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint);

  /**
   * Convert a LatLonPoint to projection coordinates
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param latLon convert from these lat, lon coordinates
   * @return ProjectionPoint convert to these projection coordinates
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    return latLonToProj(latLon, new ProjectionPointImpl());
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param ppt convert from these projection coordinates
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    return projToLatLon(ppt, new LatLonPointImpl());
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public abstract boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);

  /**
   * Returns true if this represents the same Projection as proj.
   *
   * @param proj projection in question
   * @return true if this represents the same Projection as proj.
   */
  public abstract boolean equals(Object proj);

  /**
   * Get the name of this specific projection (also see getClassName)
   *
   * @return name of the projection
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get parameters as list of ucar.unidata.util.Parameter
   *
   * @return List of parameters
   */
  public List<Parameter> getProjectionParameters() {
    return atts;
  }

  public Parameter findProjectionParameter(String want) {
    for (Parameter p : atts) {
      if (p.getName().equals(want)) return p;
    }
    return null;
  }

  /**
   * Add an attribute to this projection
   *
   * @param name  name of the attribute
   * @param value attribute value as a string
   */
  protected void addParameter(String name, String value) {
    atts.add(new Parameter(name, value));
  }

  /**
   * Add an attribute to this projection
   *
   * @param name  name of the attribute
   * @param value attribute value as a double
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
   *
   * @return true if it is the lat/lon Projection
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Get a header for display.
   *
   * @return human readable header for display
   */
  public static String getHeader() {
    StringBuilder headerB = new StringBuilder(60);
    headerB.append("Name");
    Format.tab(headerB, 20, true);
    headerB.append("Class");
    Format.tab(headerB, 40, true);
    headerB.append("Parameters");
    return headerB.toString();
  }

  /**
   * Get a String representation of this projection.
   *
   * @return the name of the projection.  This is what gets
   *         displayed when you add the projection object to
   *         a UI widget (e.g. label, combobox)
   */
  public String toString() {
    return getName();
  }

  /**
   * Get a reasonable bounding box for this projection.
   *
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
   * @param bb bounding box
   */
  public void setDefaultMapArea(ProjectionRect bb) {
    if (bb == null) return;
    defaultMapArea = new ProjectionRect(bb);
  }

  //////// convenience routines

  /**
   * Convert a LatLonPoint to projection coordinates
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param lat latitude of point to convert
   * @param lon longitude of point to convert
   * @return ProjectionPointImpl convert to these projection coordinates
   */
  public ProjectionPoint latLonToProj(double lat, double lon) {
    return latLonToProj(new LatLonPointImpl(lat, lon));
  }

  /**
   * Convert a projection coordinate to a LatLonPoint
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param x x value to convert
   * @param y y value to convert
   * @return LatLonPointImpl convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(double x, double y) {
    return projToLatLon(new ProjectionPointImpl(x, y));
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // optimizations for doing double and float arrays

  /**
   * Convert projection coordinates to lat/lon coordinates.
   *
   * @param from array of projection coordinates: from[2][n],
   *             where from[0][i], from[1][i] is the x, y coordinate
   *             of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   */
  public double[][] projToLatLon(double[][] from) {
    return projToLatLon(from, new double[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
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
   * @param from array of projection coordinates: from[2][n],
   *             where from[0][i], from[1][i] is the x, y coordinate
   *             of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   */
  public float[][] projToLatLon(float[][] from) {
    return projToLatLon(from, new float[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
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

    ProjectionPointImpl ppi = new ProjectionPointImpl();
    LatLonPointImpl llpi = new LatLonPointImpl();

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
   * @param from array of lat/lon coordinates: from[2][n],
   *             where from[0][i], from[1][i] is the (lat,lon)
   *             coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   */
  public double[][] latLonToProj(double[][] from) {
    return latLonToProj(from, new double[2][from[0].length]);
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
   * @return the "to" array
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

    ProjectionPointImpl ppi = new ProjectionPointImpl();
    LatLonPointImpl llpi = new LatLonPointImpl();

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
   * @param from array of lat/lon coordinates: from[2][n],
   *             where from[0][i], from[1][i] is the (lat,lon)
   *             coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   */
  public float[][] latLonToProj(float[][] from) {
    return latLonToProj(from, new float[2][from[0].length]);
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
   * @return the "to" array
   */
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                int lonIndex) {
    //      ucar.unidata.util.Misc.printStack ("latLonToProj-" + this + " size=" + from[0].length, 4, null);

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

    ProjectionPointImpl ppi = new ProjectionPointImpl();
    LatLonPointImpl llpi = new LatLonPointImpl();

    for (int i = 0; i < from[0].length; i++) {
      llpi.setLatitude(from[latIndex][i]);
      llpi.setLongitude(from[lonIndex][i]);
      latLonToProj(llpi, ppi);
      to[0][i] = (float) ppi.getX();
      to[1][i] = (float) ppi.getY();
    }

    return to;
  }

  // bounding box utilities

  /**
   * Alternate way to calculate latLonToProjBB, originally in GridCoordSys.
   * Difficult to do this in a general way.
   *
   * @param latlonRect desired lat/lon rectangle
   * @return a ProjectionRect
   */
  ProjectionRect latLonToProjBB2(LatLonRect latlonRect) {
    double minx, maxx, miny, maxy;

    LatLonPointImpl llpt = latlonRect.getLowerLeftPoint();
    LatLonPointImpl urpt = latlonRect.getUpperRightPoint();
    LatLonPointImpl lrpt = latlonRect.getLowerRightPoint();
    LatLonPointImpl ulpt = latlonRect.getUpperLeftPoint();

    if (isLatLon()) {
      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

    } else {
      ProjectionPoint ll = latLonToProj(llpt, new ProjectionPointImpl());
      ProjectionPoint ur = latLonToProj(urpt, new ProjectionPointImpl());
      ProjectionPoint lr = latLonToProj(lrpt, new ProjectionPointImpl());
      ProjectionPoint ul = latLonToProj(ulpt, new ProjectionPointImpl());

      minx = Math.min(ll.getX(), ul.getX());
      miny = Math.min(ll.getY(), lr.getY());
      maxx = Math.max(ur.getX(), lr.getX());
      maxy = Math.max(ul.getY(), ur.getY());
    }

    return new ProjectionRect(minx, miny, maxx, maxy);
  }

  private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
    lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

  /**
   * Convert a lat/lon bounding box to a world coordinate bounding box,
   * by finding the minimum enclosing box.
   * Handles lat/lon points that do not intersect the projection panel.
   *
   * @param latlonRect input lat,lon bounding box
   * @return minimum enclosing box in world coordinates, or null if no part of the LatLonRect intersects the projection plane
   */
  public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
    if (isLatLon) {
      LatLonProjection llp = (LatLonProjection) this;
      llp.setCenterLon(latlonRect.getCenterLon()); // LOOK side effect BAD !!
    }

    ProjectionPointImpl w1 = new ProjectionPointImpl();
    ProjectionPointImpl w2 = new ProjectionPointImpl();

    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    latLonToProj(ll, w1);
    latLonToProj(ur, w2);

    //if (!isLatLon && crossSeam(w1, w2)) {
    //  log.warn("CROSS SEAM failure=" + w1 + " " + w2+" for "+this, new Throwable());
    //}

    // make bounding box out of those two corners
    ProjectionRect world = new ProjectionRect(w1.getX(), w1.getY(), w2.getX(), w2.getY());

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
   *
   * @param world input world coordinate bounding box
   * @return minimum enclosing box in lat,lon coordinates.
   */
  public LatLonRect projToLatLonBBold(ProjectionRect world) {
    //System.out.println("world = " + world);
    ProjectionPoint min = world.getMinPoint();
    ProjectionPoint max = world.getMaxPoint();
    //System.out.println("min = " + min);
    //System.out.println("max = " + max);
    LatLonRect llbb;
    LatLonPointImpl llmin = new LatLonPointImpl();
    LatLonPointImpl llmax = new LatLonPointImpl();

    // make bounding box out of the min, max corners
    projToLatLon(min, llmin);
    projToLatLon(max, llmax);
    llbb = new LatLonRect(llmin, llmax);
    //System.out.println("llbb = " + llbb);

    /*
   double lona = la.getLongitude();
   double lonb = lb.getLongitude();

   if (((lona < lonb) && (lonb - lona <= 180.0))
           || ((lona > lonb) && (lona - lonb >= 180.0))) {
       llbb = new LatLonRect(la, lb);
   } else {
       llbb = new LatLonRect(lb, la);
   } */

    ProjectionPointImpl w1 = new ProjectionPointImpl();
    ProjectionPointImpl w2 = new ProjectionPointImpl();

    // now extend if needed using the other two corners
    w1.setLocation(min.getX(), max.getY());
    projToLatLon(w1, llmin);
    llbb.extend(llmin);

    w2.setLocation(max.getX(), min.getY());
    projToLatLon(w2, llmax);
    llbb.extend(llmax);

    return llbb;
  }

  /**
   * Compute lat/lon bounding box from projection bounding box by finding the minimum enclosing box.
   * @param bb projection bounding box
   * @return lat, lon bounding box.
   */
  public LatLonRect projToLatLonBB(ProjectionRect bb) {
    // look at all 4 corners of the bounding box
    LatLonPoint llpt = projToLatLon(bb.getLowerLeftPoint(), new LatLonPointImpl());
    LatLonPoint lrpt = projToLatLon(bb.getLowerRightPoint(), new LatLonPointImpl());
    LatLonPoint urpt = projToLatLon(bb.getUpperRightPoint(), new LatLonPointImpl());
    LatLonPoint ulpt = projToLatLon(bb.getUpperLeftPoint(), new LatLonPointImpl());

    // Check if grid contains poles.
    boolean includesNorthPole = false;
    /* int[] resultNP;
    findXYindexFromLatLon(90.0, 0, resultNP);
    if (resultNP[0] != -1 && resultNP[1] != -1)
      includesNorthPole = true;      */
    boolean includesSouthPole = false;
    /* int[] resultSP = new int[2];
    findXYindexFromLatLon(-90.0, 0, resultSP);
    if (resultSP[0] != -1 && resultSP[1] != -1)
      includesSouthPole = true; */

    LatLonRect llbb;

    if (includesNorthPole && !includesSouthPole) {
      llbb = new LatLonRect(llpt, new LatLonPointImpl(90.0, 0.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);
      // OR
      //llbb.extend( new LatLonRect( llpt, lrpt ));
      //llbb.extend( new LatLonRect( lrpt, urpt ) );
      //llbb.extend( new LatLonRect( urpt, ulpt ) );
      //llbb.extend( new LatLonRect( ulpt, llpt ) );
    } else if (includesSouthPole && !includesNorthPole) {
      llbb = new LatLonRect(llpt, new LatLonPointImpl(-90.0, -180.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);

    } else {
      double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

      // longitude is a bit tricky as usual
      double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

      LatLonPointImpl min = new LatLonPointImpl(latMin, lonMin);
      LatLonPointImpl max = new LatLonPointImpl(latMax, lonMax);
      llbb = new LatLonRect(min, max);
    }

    return llbb;
  }

}

