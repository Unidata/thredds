/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

package ucar.unidata.geoloc.projection.sat;

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.ProjectionRect;

/**
 * Describe: https://cf-pcmdi.llnl.gov/trac/ticket/72
 * Accepted for CF-1.7
 *
 * grid_mapping_name = geostationary
   Map parameters:
     latitude_of_projection_origin
     longitude_of_projection_origin
     perspective_point_height
     semi_minor_axis
     semi_major_axis
     inverse_flattening
     sweep_angle_axis
     fixed_angle_axis

 Map coordinates:
  The x (abscissa) and y (ordinate) rectangular coordinates are identified by the standard_name attribute value projection_x_coordinate and projection_y_coordinate
 respectively. In the case of this projection, the projection coordinates in this projection are directly related to the scanning angle of the satellite instrument,
 and their units are radians.

 Notes:

 The algorithm for computing the mapping may be found at http://www.eumetsat.int/idcplg?IdcService=GET_FILE&dDocName=PDF_CGMS_03&RevisionSelectionMethod=LatestReleased.
 This document assumes the point of observation is directly over the equator, and that the sweep_angle_axis is y.

 Notes on using the PROJ.4 software packages for computing the mapping may be found at http://trac.osgeo.org/proj/wiki/proj%3Dgeos and
 http://remotesensing.org/geotiff/proj_list/geos.html .

 The "perspective_point_height" is the distance to the surface of the ellipsoid. Adding the earth major axis gives the distance from the centre of the earth.

 The "sweep_angle_axis" attribute indicates which axis the instrument sweeps. The value = "y" corresponds to the spin-stabilized Meteosat satellites,
 the value = "x" to the GOES-R satellite.

 The "fixed_angle_axis" attribute indicates which axis the instrument is fixed. The values are opposite to "sweep_angle_axis". Only one of those two attributes are
 mandatory.

 latitude_of_projection_origin will be taken as zero (at the Equator).

 inverse_flattening may be specified independent of the semi_minor/major axes (GRS80). If left unspecified it will be computed
 from semi_minor/major_axis values.
 *
 * @author caron
 * @since 12/5/13
 */

public class Geostationary extends ProjectionImpl {
  private static final String NAME = CF.GEOSTATIONARY;
  private boolean isScaledX = false;
  private boolean isScaledY = false;
  private double xScaleFactor, yScaleFactor;

  GEOSTransform navigation = null;

  public Geostationary(double subLonDegrees, double perspective_point_height, double semi_minor_axis,
                       double semi_major_axis, double inv_flattening, boolean isSweepX) {

    // scale factors (last two doubles in the sig) less than zero indicate no scaling of map x, y coordinates
    this(subLonDegrees, perspective_point_height, semi_minor_axis, semi_major_axis, inv_flattening, isSweepX,
            -1.0, -1.0);
  }

  public Geostationary(double subLonDegrees, double perspective_point_height, double semi_minor_axis,
            double semi_major_axis, double inv_flattening, boolean isSweepX, double xScaleFactor,
                       double yScaleFactor) {
    super(NAME, false);

    String sweepAngleAxis = "y";
    if (isSweepX) {
      sweepAngleAxis = "x";
    }

    /* Must assume incoming distances are SI units, so convert 'm' -> 'km' for GEOSTransform */
    perspective_point_height /= 1000.0;
    semi_minor_axis /= 1000.0;
    semi_major_axis /= 1000.0;

    navigation = new GEOSTransform(subLonDegrees, perspective_point_height, semi_minor_axis, semi_major_axis, inv_flattening, sweepAngleAxis);
    makePP();

    if (xScaleFactor > 0) {
      isScaledX = true;
      this.xScaleFactor = xScaleFactor;
    }

    if (yScaleFactor > 0) {
      isScaledY = true;
      this.yScaleFactor = yScaleFactor;
    }

  }

  public Geostationary() {
    super(NAME, false);
    navigation = new GEOSTransform();
    makePP();
  }

  public Geostationary(double subLonDegrees) {
    super(NAME, false);
    navigation = new GEOSTransform(subLonDegrees, GEOSTransform.GOES);
    makePP();
  }

  public Geostationary(double subLonDegrees, boolean isSweepX) {
    super(NAME, false);

    String sweepAngleAxis = "y";
    if (isSweepX) {
      sweepAngleAxis = "x";
    }

    String scanGeometry = GEOSTransform.sweepAngleAxisToScanGeom(sweepAngleAxis);

    navigation = new GEOSTransform(subLonDegrees, scanGeometry);
    makePP();
  }

  public Geostationary(double subLonDegrees, String sweepAngleAxis, double xScaleFactor, double yScaleFactor) {
    super(NAME, false);

    String scanGeometry = GEOSTransform.sweepAngleAxisToScanGeom(sweepAngleAxis);

    navigation = new GEOSTransform(subLonDegrees, scanGeometry);

    if (xScaleFactor > 0) {
      isScaledX = true;
      this.xScaleFactor = xScaleFactor;
    }

    if (yScaleFactor > 0) {
      isScaledY = true;
      this.yScaleFactor = yScaleFactor;
    }

    makePP();
  }

  private void makePP() {
    addParameter(CF.GRID_MAPPING_NAME, NAME);
    addParameter(CF.LONGITUDE_OF_PROJECTION_ORIGIN, navigation.sub_lon_degrees);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, 0.0);
    addParameter(CF.PERSPECTIVE_POINT_HEIGHT, navigation.sat_height * 1000.0);
    addParameter(CF.SWEEP_ANGLE_AXIS, GEOSTransform.scanGeomToSweepAngleAxis(navigation.scan_geom));
    addParameter(CF.SEMI_MAJOR_AXIS, navigation.r_eq * 1000.0);
    addParameter(CF.SEMI_MINOR_AXIS, navigation.r_pol * 1000.0);
  }

  /**
   * copy constructor - avoid clone !!
   */
  @Override
  public ProjectionImpl constructCopy() {
    // constructor takes sweep_angle_axis, so need to translate between
    // scan geometry and sweep_angle_axis first
    // GOES: x
    // GEOS: y
    String sweepAxisAngle = GEOSTransform.scanGeomToSweepAngleAxis(navigation.scan_geom);

    return new Geostationary(navigation.sub_lon_degrees, sweepAxisAngle, xScaleFactor, yScaleFactor);
  }

  @Override
  public String paramsToString() {
    return "";
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    double[] satCoords = navigation.earthToSat(latlon.getLongitude(), latlon.getLatitude());
    double x = satCoords[0];
    double y = satCoords[1];

    // scale back to required units of x, y (we need them in radians)
    if (isScaledX) x = x * xScaleFactor;
    if (isScaledY) y = y * yScaleFactor;

    destPoint.setLocation(satCoords[0], satCoords[1]);
    return destPoint;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    double x = ppt.getX();
    double y = ppt.getY();
    if (isScaledX) x = x * xScaleFactor;
    if (isScaledY) y = y * yScaleFactor;
    double[] lonlat = navigation.satToEarth(x, y);
    destPoint.setLongitude(lonlat[0]);
    destPoint.setLatitude(lonlat[1]);
    return destPoint;
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2))
      return true;

    double x1 = pt1.getX();
    double x2 = pt2.getX();

    if (isScaledX) {
      x1 = x1 * xScaleFactor;
      x2 = x2 * xScaleFactor;
    }

    // opposite signed X values, larger then 100 km
    return (x1 * x2 < 0) && (Math.abs(x1 - x2) > 100);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Geostationary that = (Geostationary) o;

    if (!navigation.equals(that.navigation)) return false;

    if (!(xScaleFactor == that.xScaleFactor)) return false;

    if (!(yScaleFactor == that.yScaleFactor)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return navigation.hashCode();
  }

  /**
   * Create a ProjectionRect from the given LatLonRect.
   * Handles lat/lon points that do not intersect the projection panel.
   * LOOK NEEDS OVERRIDDING
   * @param rect the LatLonRect
   * @return ProjectionRect, or null if no part of the LatLonRect intersects the projection plane
   */
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect rect) {
    return super.latLonToProjBB(rect);
  }

}
