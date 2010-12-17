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

import ucar.unidata.util.Format;
import ucar.unidata.util.SpecialMathFunction;


/**
 * Mercator projection, spherical earth.
 * Projection plane is a cylinder tangent to the earth at tangentLon.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 43-47
 *
 * @author John Caron
 * @see Projection
 * @see ProjectionImpl
 */


public class Mercator extends ProjectionImpl {


  /**
   * longitude of the origin degrees
   */
  private double lon0;

  /**
   * standard parallel degrees
   */
  private double par;

  /**
   * standard parallel in radians
   */
  private double par_r;

  /**
   * Factor
   */
  private double A;

  private double falseEasting, falseNorthing;

  /**
   * origin point
   */
  private LatLonPointImpl origin;

  /**
   * copy constructor - avoid clone !!
   */
  public ProjectionImpl constructCopy() {
    return new Mercator(getOriginLon(), getParallel(), getFalseEasting(), getFalseNorthing());
  }

  /**
   * Constructor with default parameteres
   */
  public Mercator() {
    this(-105, 20.0);
  }

  /**
   * Construct a Mercator Projection.
   *
   * @param lat0 latitude of origin (degrees) NOT USED
   * @param lon0 longitude of origin (degrees)
   * @param par  standard parallel (degrees). cylinder cuts earth at this latitude.
   * @deprecated use Mercator(double lon0, double par)
   */
  public Mercator(double lat0, double lon0, double par) {
    this(lon0, par);
  }

  /**
   * Construct a Mercator Projection.
   *
   * @param lon0 longitude of origin (degrees)
   * @param par  standard parallel (degrees). cylinder cuts earth at this latitude.
   */
  public Mercator(double lon0, double par) {
    this(lon0, par, 0.0, 0.0);
  }

  /**
   * Construct a Mercator Projection.
   *
   * @param lon0 longitude of origin (degrees)
   * @param par  standard parallel (degrees). cylinder cuts earth at this latitude.
   * @param false_easting false_easting in km
   * @param false_northing false_northing in km
   */
  public Mercator(double lon0, double par, double false_easting, double false_northing) {
    origin = new LatLonPointImpl(0.0, lon0);
    this.lon0 = lon0;
    this.par = par;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;

    this.par_r = Math.toRadians(par);

    precalculate();

    addParameter(ATTR_NAME, "mercator");
    addParameter("longitude_of_projection_origin", lon0);
    addParameter("standard_parallel", par);
    if (false_easting != 0.0)
      addParameter("false_easting", false_easting);
    if (false_northing != 0.0)
      addParameter("false_northing", false_northing);
  }

  /**
   * Precalculate some params
   */
  private void precalculate() {
    A = EARTH_RADIUS * Math.cos(par_r);  // incorporates the scale factor at par
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
   * @param par the first standard parallel
   */
  public void setParallel(double par) {
    this.par = par;
    precalculate();
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
    return origin.getLongitude();
  }

  /**
   * Set the origin longitude.
   *
   * @param lon the origin longitude.
   */
  public void setOriginLon(double lon) {
    origin.setLongitude(lon);
    lon0 = lon;
    precalculate();
  }

  /**
   * Set the origin latitude, has not effect.
   * @param lat   the origin latitude.
   * @deprecated not used, only here for XML Persistence
   */
  public void setOriginLat(double lat) {
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Set the false_easting, in km.
   * natural_x_coordinate + false_easting = x coordinate
   *
   * @param falseEasting x offset
   */
  public void setFalseEasting(double falseEasting) {
    this.falseEasting = falseEasting;
  }


  /**
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  /**
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   *
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
    this.falseNorthing = falseNorthing;
  }



  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return " origin " + origin.toString() + " parellel: "
            + Format.d(getParallel(), 6);
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
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
   *
   * @return a clone of this.
   */
  public Object clone() {
    Mercator cl = (Mercator) super.clone();
    cl.origin = new LatLonPointImpl(0.0, getOriginLon());
    return cl;
  }

  /**
   * Returns true if this represents the same Projection as proj.
   *
   * @param proj projection in question
   * @return true if this represents the same Projection as proj.
   */
  public boolean equals(Object proj) {
    if (!(proj instanceof Mercator)) {
      return false;
    }

    Mercator oo = (Mercator) proj;
    return ((this.getParallel() == oo.getParallel())
            && (this.getOriginLon() == oo.getOriginLon())
            && this.defaultMapArea.equals(oo.defaultMapArea));
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
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();
    double fromLat_r = Math.toRadians(fromLat);

    // infinite projection
    if ((Math.abs(90.0 - Math.abs(fromLat))) < TOLERANCE) {
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    } else {
      toX = A * Math.toRadians(LatLonPointImpl.range180(fromLon - this.lon0));
      toY = A * SpecialMathFunction.atanh(Math.sin(fromLat_r)); // p 41 Snyder
    }

    result.setLocation(toX + falseEasting, toY + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;

    double toLon = Math.toDegrees(fromX / A) + lon0;

    double e = Math.exp(-fromY / A);
    double toLat = Math.toDegrees(Math.PI / 2 - 2 * Math.atan(e)); // Snyder p 44

    result.setLatitude(toLat);
    result.setLongitude(toLon);
    return result;
  }

}

