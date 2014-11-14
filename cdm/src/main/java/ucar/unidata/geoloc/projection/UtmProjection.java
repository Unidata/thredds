/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.constants.CF;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;

import java.io.Serializable;

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
  public static final String GRID_MAPPING_NAME = "universal_transverse_mercator";
  public static final String UTM_ZONE1 = "utm_zone_number";
  public static final String UTM_ZONE2 = "UTM_zone";

  private final Utm_To_Gdc_Converter convert2latlon;
  private final Gdc_To_Utm_Converter convert2xy;

  private static class SaveParams implements Serializable {
    final double a;
    final double f;
    final int zone;
    final boolean isNorth;

    private SaveParams(double a, double f, int zone, boolean isNorth) {
      this.a = a;
      this.f = f;
      this.zone = zone;
      this.isNorth = isNorth;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SaveParams that = (SaveParams) o;
      if (Double.compare(that.a, a) != 0) return false;
      if (Double.compare(that.f, f) != 0) return false;
      if (isNorth != that.isNorth) return false;
      return zone == that.zone;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(a);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(f);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + zone;
      result = 31 * result + (isNorth ? 1 : 0);
      return result;
    }
  }

  private final SaveParams saveParams; // needed for constructCopy

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result = (saveParams == null) ? new UtmProjection(getZone(), isNorth()) : new UtmProjection(saveParams.a, saveParams.f, getZone(), isNorth());
    result.setDefaultMapArea(defaultMapArea);
    result.setName(name);
    return result;
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
    super("UtmProjection", false);
    convert2latlon = new Utm_To_Gdc_Converter(zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(zone, isNorth);
    saveParams = new SaveParams(convert2latlon.getA(), 1/convert2latlon.getF(), zone, isNorth);

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(CF.SEMI_MAJOR_AXIS, convert2latlon.getA());
    addParameter(CF.INVERSE_FLATTENING, convert2latlon.getF());
    addParameter(UTM_ZONE1, zone);
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
    super("UtmProjection", false);
    saveParams = new SaveParams(a, f, zone, isNorth);

    convert2latlon = new Utm_To_Gdc_Converter(a, f, zone, isNorth);
    convert2xy = new Gdc_To_Utm_Converter(a, f, zone, isNorth);

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(CF.SEMI_MAJOR_AXIS, a);
    addParameter(CF.INVERSE_FLATTENING, f);
    addParameter(UTM_ZONE1, zone);
  }

  /**
   * Get the zone number = [1,60]
   *
   * @return zone number
   */
  public int getZone() {
    return convert2latlon.getZone();
  }

  /**
   * Get whether in North or South Hemisphere.
   *
   * @return true if north
   */
  public boolean isNorth() {
    return convert2latlon.isNorth();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UtmProjection that = (UtmProjection) o;
    return saveParams.equals(that.saveParams);
  }

  @Override
  public int hashCode() {
    return saveParams.hashCode();
  }

  /*
   * Returns true if this represents the same Projection as proj.
   *
   * @param proj projection in question
   * @return true if this represents the same Projection as proj.
   *
  public boolean equals(Object proj) {
    if (!(proj instanceof UtmProjection)) {
      return false;
    }

    UtmProjection op = (UtmProjection) proj;
    if ((this.getDefaultMapArea() == null) != (op.defaultMapArea == null)) return false; // common case is that these are null
    if (this.getDefaultMapArea() != null && !this.defaultMapArea.equals(op.defaultMapArea)) return false;

    return op.getZone() == getZone();
  }  */




  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon, ProjectionPointImpl result) {
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    return convert2xy.latLonToProj(fromLat, fromLon, result);
  }

  public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:null array argument or wrong dimension (to)");
    }
    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:from array not same length as to array");
    }

    return convert2xy.latLonToProj(from, to, latIndex, lonIndex);
  }

  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:null array argument or wrong dimension (to)");
    }
    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:from array not same length as to array");
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
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    return convert2latlon.projToLatLon(world.getX(), world.getY(), result);
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
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:from array not same length as to array");
    }

    return convert2latlon.projToLatLon(from, to);
  }

  public double[][] projToLatLon(double[][] from, double[][] to) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:from array not same length as to array");
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
    LatLonPoint ll = utm.projToLatLon(577.8000000000001, 2951.8);
    System.out.printf("%15.12f %15.12f%n", ll.getLatitude(), ll.getLongitude());
    assert Misc.closeEnough(ll.getLongitude(), -80.21802662821469, 1.0e-8);
    assert Misc.closeEnough(ll.getLatitude(), 26.685132668190793, 1.0e-8);
  }

}









