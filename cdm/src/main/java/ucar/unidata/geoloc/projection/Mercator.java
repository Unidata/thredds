/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
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
   * Convert "scale at standard parellel" to "standard parellel"
   *
   * @param scale scale at standard parallel
   * @return standard parallel in degrees
   */
  public static double convertScaleToStandardParallel(double scale) {
    // k = 1 / cos (par); snyder p 44
    // par = arccos(1/k);
    double par = Math.acos(1.0 / scale);
    return Math.toDegrees(par);
  }

  /////////////////////////////////////////////////////////////////////
  private final double earthRadius;
  private double lon0; // longitude of the origin in degrees
  private double par; // standard parallel in degrees
  private double falseEasting, falseNorthing;

  private double par_r; // standard parallel in radians
  private double A;

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new Mercator(getOriginLon(), getParallel(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters
   */
  public Mercator() {
    this(-105, 20.0, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a Mercator Projection.
   *
   * @param lon0 longitude of origin (degrees)
   * @param par  standard parallel (degrees). cylinder cuts earth at this latitude.
   */
  public Mercator(double lon0, double par) {
    this(lon0, par, 0.0, 0.0, EARTH_RADIUS);
  }

  public Mercator(double lon0, double par, double false_easting, double false_northing) {
    this(lon0, par, false_easting, false_northing, EARTH_RADIUS);
  }

  /**
   * Construct a Mercator Projection.
   *
   * @param lon0           longitude of origin (degrees)
   * @param par            standard parallel (degrees). cylinder cuts earth at this latitude.
   * @param false_easting  false_easting in km
   * @param false_northing false_northing in km
   * @param radius         earth radius in km
   */
  public Mercator(double lon0, double par, double false_easting, double false_northing, double radius) {
    super("Mercator", false);

    this.lon0 = lon0;
    this.par = par;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    this.earthRadius = radius;

    this.par_r = Math.toRadians(par);

    precalculate();

    addParameter(CF.GRID_MAPPING_NAME, CF.MERCATOR);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, lon0);
    addParameter(CF.STANDARD_PARALLEL, par);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }

  }

  /**
   * Precalculate some params
   */
  private void precalculate() {
    A = earthRadius * Math.cos(par_r);  // incorporates the scale factor at par
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
   * Get the origin longitude.
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
    return lon0;
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
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  //////////////////////////////////////////////
  // setters for IDV serialization - do not use except for object creating

  /**
   * Set the first standard parallel
   *
   * @param par the first standard parallel
   */
  public void setParallel(double par) {
    this.par = par;
    this.par_r = Math.toRadians(par);
    precalculate();
  }

  /**
   * Set the origin longitude.
   *
   * @param lon the origin longitude.
   */
  public void setOriginLon(double lon) {
    lon0 = lon;
    precalculate();
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
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   *
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
    this.falseNorthing = falseNorthing;
  }

  /////////////////////////////////////////////////////

  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  @Override
  public String paramsToString() {
    return toString();
  }

  @Override
  public String toString() {
    return "Mercator{" +
            "earthRadius=" + earthRadius +
            ", lon0=" + lon0 +
            ", par=" + par +
            ", falseEasting=" + falseEasting +
            ", falseNorthing=" + falseNorthing +
            '}';
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }

    // opposite signed long lines: LOOK ????
    return (pt1.getX() * pt2.getX() < 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Mercator mercator = (Mercator) o;

    if (Double.compare(mercator.earthRadius, earthRadius) != 0) return false;
    if (Double.compare(mercator.falseEasting, falseEasting) != 0) return false;
    if (Double.compare(mercator.falseNorthing, falseNorthing) != 0) return false;
    if (Double.compare(mercator.lon0, lon0) != 0) return false;
    if (Double.compare(mercator.par, par) != 0) return false;
    if ((defaultMapArea == null) != (mercator.defaultMapArea == null)) return false; // common case is that these are null
    if (defaultMapArea != null && !mercator.defaultMapArea.equals(defaultMapArea)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = par != +0.0d ? Double.doubleToLongBits(par) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
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
  @Override
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

