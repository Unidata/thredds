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
 * Universal Transverse Mercator.
 * Ellipsoidal earth.
 * <p/>
 * Origin of coordinate system is reletive to the point where the
 * central meridian and the equator cross.
 * This point has x,y value = (500, 0) km for north hemisphere.
 * and (500, 10,0000) km for south hemisphere.
 * Increasing values always go north and east.
 * <p/>
 * The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
 *
 * @author John Caron
 */


public class UtmProjection extends ProjectionImpl {

  private Utm_To_Gdc_Converter convert2latlon;
  private Gdc_To_Utm_Converter convert2xy;


  private static class SaveParams {
    double a;
    double f;

    SaveParams(double a, double f) {
      this.a = a;
      this.f = f;
    }
  }

  private SaveParams save = null; // needed for constructCopy

  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    return (save == null) ? new UtmProjection(getZone(), isNorth()) : new UtmProjection(save.a, save.f, getZone(), isNorth());
  }

  /**
   * Constructor with default parameters
   */
  public UtmProjection() {
    this(5, true);
  }


  /**
   * Constructor with default WGS 84 ellipsoid.
   *
   * @param zone    the UTM zone number (1-60)
   * @param isNorth true if the UTM coordinate is in the northern hemisphere
   */
  public UtmProjection(int zone, boolean isNorth) {
    convert2latlon = new Utm_To_Gdc_Converter(zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(zone, isNorth);

    addParameter(ATTR_NAME, "UTM");
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
   * @param a       the semi-major axis (meters) for the ellipsoid
   * @param f       the inverse flattening for the ellipsoid
   * @param zone    the UTM zone number (1-60)
   * @param isNorth true if the UTM coordinate is in the northern hemisphere
   */
  public UtmProjection(double a, double f, int zone, boolean isNorth) {
    save = new SaveParams(a, f);

    convert2latlon = new Utm_To_Gdc_Converter(a, f, zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(a, f, zone, isNorth);

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

  /*
   * Getting the central meridian in degrees.  depends on the zone
   * @return  the central meridian in degrees.
   */
  public double getCentralMeridian() {
    return convert2xy.getCentralMeridian();
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


  /*
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
  */
  /**
   * Returns true if this represents the same Projection as proj.
   *
   * @param proj projection in question
   * @return true if this represents the same Projection as proj.
   */
  public boolean equals(Object proj) {
    if (!(proj instanceof UtmProjection)) {
      return false;
    }

    UtmProjection op = (UtmProjection) proj;
    return op.getZone() == getZone(); // LOOK
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
   * @param from     _more_
   * @param to       _more_
   * @param latIndex _more_
   * @param lonIndex _more_
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
   * @param from     _more_
   * @param to       _more_
   * @param latIndex _more_
   * @param lonIndex _more_
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

    return convert2latlon.projToLatLon(from, to);
  }

  /**
   * _more_
   *
   * @param from _more_
   * @param to   _more_
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

  /*
  roszelld@usgs.gov
  'm transforming coordinates (which are in UTM Zone 17N projection) to
lat/lon.

If I get the ProjectionImpl from the grid (stage) and use the projToLatLon
function with {{577.8000000000001}, {2951.8}} in kilometers for example, I
get {{26.553706785076937}, {-80.21754983617633}}, which is not very
accurate at all when I plot them.

If I use GeoTools to build a transform based on the same projection
parameters read from the projectionimpl, I get {{26.685132668190793},
{-80.21802662821469}} which appears to be MUCH more accurate when I plot
them on a map.
   */
    public static void main(String arg[]) {
      UtmProjection utm = new UtmProjection(17, true);
      LatLonPointImpl ll = utm.projToLatLon(577.8000000000001, 2951.8);
      System.out.printf("%15.12f %15.12f%n",ll.getLatitude(), ll.getLongitude());
      assert closeEnough(ll.getLongitude(), -80.21802662821469, 1.0e-8);
      assert closeEnough(ll.getLatitude(), 26.685132668190793, 1.0e-8);
    }

  private static boolean closeEnough( double v1, double v2, double tol) {
     double diff = (v2 == 0.0) ? Math.abs(v1-v2) : Math.abs(v1/v2-1);
     return diff < tol;
   }


}









