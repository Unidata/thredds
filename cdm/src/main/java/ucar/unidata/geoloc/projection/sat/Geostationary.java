/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
  private boolean isGeoCoordinateScaled = false;
  private double geoCoordinateScaleFactor;

  GEOSTransform navigation = null;

  public Geostationary(double subLonDegrees, double perspective_point_height, double semi_minor_axis,
                       double semi_major_axis, double inv_flattening, boolean isSweepX) {

    // scale factors (last two doubles in the sig) less than zero indicate no scaling of map x, y coordinates
    this(subLonDegrees, perspective_point_height, semi_minor_axis, semi_major_axis, inv_flattening, isSweepX,
            -1.0);
  }

  public Geostationary(double subLonDegrees, double perspective_point_height, double semi_minor_axis,
            double semi_major_axis, double inv_flattening, boolean isSweepX,
                       double geoCoordinateScaleFactor) {
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

    if (geoCoordinateScaleFactor > 0) {
      isGeoCoordinateScaled = true;
      this.geoCoordinateScaleFactor = geoCoordinateScaleFactor;
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

  public Geostationary(double subLonDegrees, String sweepAngleAxis, double geoCoordinateScaleFactor) {
    super(NAME, false);

    String scanGeometry = GEOSTransform.sweepAngleAxisToScanGeom(sweepAngleAxis);

    navigation = new GEOSTransform(subLonDegrees, scanGeometry);

    if (geoCoordinateScaleFactor > 0) {
      isGeoCoordinateScaled = true;
      this.geoCoordinateScaleFactor = geoCoordinateScaleFactor;
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

    return new Geostationary(navigation.sub_lon_degrees, sweepAxisAngle, geoCoordinateScaleFactor);
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
    if (isGeoCoordinateScaled) x = x * geoCoordinateScaleFactor;

    destPoint.setLocation(satCoords[0], satCoords[1]);
    return destPoint;
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    double x = ppt.getX();
    double y = ppt.getY();
    if (isGeoCoordinateScaled) x = x * geoCoordinateScaleFactor;
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

    if (isGeoCoordinateScaled) {
      x1 = x1 * geoCoordinateScaleFactor;
      x2 = x2 * geoCoordinateScaleFactor;
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

    if (!(geoCoordinateScaleFactor == that.geoCoordinateScaleFactor)) return false;

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
