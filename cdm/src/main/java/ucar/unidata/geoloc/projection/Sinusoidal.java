/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;

/**
 * Sinusoidal projection, spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 243
 *
 * @author John Caron
 * @since Feb 24, 2013
 */

public class Sinusoidal extends ProjectionImpl {

  private final double earthRadius;
  private double centMeridian; // central Meridian in degrees
  private double falseEasting, falseNorthing;

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new Sinusoidal(getCentMeridian(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
  }

  /**
   * Constructor with default parameters
   */
  public Sinusoidal() {
    this(0.0, 0.0, 0.0, EARTH_RADIUS);
  }


  /**
   * Construct a Sinusoidal Projection.
   *
   * @param centMeridian   central Meridian (degrees)
   * @param false_easting  false_easting in km
   * @param false_northing false_northing in km
   * @param radius         earth radius in km
   */
  public Sinusoidal(double centMeridian, double false_easting, double false_northing, double radius) {
    super(CF.SINUSOIDAL, false);

    this.centMeridian = centMeridian;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    this.earthRadius = radius;

    addParameter(CF.GRID_MAPPING_NAME, CF.SINUSOIDAL);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, centMeridian);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }

  }

  /**
   * Get the central Meridian in degrees
   *
   * @return the central Meridian
   */
  public double getCentMeridian() {
    return centMeridian;
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
   * Set the central Meridian
   *
   * @param centMeridian central Meridian in degrees
   */
  public void setCentMeridian(double centMeridian) {
    this.centMeridian = centMeridian;
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


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Sinusoidal that = (Sinusoidal) o;

    if (Double.compare(that.centMeridian, centMeridian) != 0) return false;
    if (Double.compare(that.earthRadius, earthRadius) != 0) return false;
    if (Double.compare(that.falseEasting, falseEasting) != 0) return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = centMeridian != +0.0d ? Double.doubleToLongBits(centMeridian) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Sinusoidal");
    sb.append("{earthRadius=").append(earthRadius);
    sb.append(", centMeridian=").append(centMeridian);
    sb.append(", falseEasting=").append(falseEasting);
    sb.append(", falseNorthing=").append(falseNorthing);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get the parameters as a String
   *
   * @return the parameters as a String
   */
  public String paramsToString() {
    return toString();
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
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }

    // opposite signed long lines
    double x1 = pt1.getX() - falseEasting;
    double x2 = pt2.getX() - falseEasting;
    return (x1 * x2 < 0) && (Math.abs(x1 - x2) > earthRadius);
  }

  /**                       x1
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
    double deltaLon = LatLonPointImpl.range180(latLon.getLongitude() - centMeridian);
    double fromLat_r = Math.toRadians(latLon.getLatitude());

    double toX = earthRadius * Math.toRadians(deltaLon) * Math.cos(fromLat_r);
    double toY = earthRadius * fromLat_r; // p 247 Snyder

    result.setLocation(toX + falseEasting, toY + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint the lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;

    double toLat_r = fromY / earthRadius;
    double toLon_r = centMeridian;
    if (!Misc.closeEnough(toLat_r, Math.PI/2, 1e-10)) // if lat = +- pi/2, set lon = centMeridian (Snyder 248)
      toLon_r += fromX / (earthRadius * Math.cos(toLat_r));

    result.setLatitude(Math.toDegrees(toLat_r));
    result.setLongitude(Math.toDegrees(toLon_r));
    return result;
  }

}

