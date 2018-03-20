/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import java.util.List;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * Describe: http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/cf-conventions.html#_geostationary_projection
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
public class Geostationary extends AbstractTransformBuilder implements HorizTransformBuilderIF {

    private static double defaultScaleFactor = -1.0;

    public String getTransformName() {
      return CF.GEOSTATIONARY;
    }

    public TransformType getTransformType() {
      return TransformType.Projection;
    }

    private double getScaleFactor(String geoCoordinateUnits) {
      // default value of -1.0 interpreted as no scaling in the class
      // ucar.unidata.geoloc.projection.sat.Geostationary
      double scaleFactor = defaultScaleFactor;
      String neededMapCoordinateUnit = "radian";
      if (SimpleUnit.isCompatible(geoCoordinateUnits, neededMapCoordinateUnit)) {
        scaleFactor = SimpleUnit.getConversionFactor(geoCoordinateUnits, neededMapCoordinateUnit);
      }

      return scaleFactor;
    }

    public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
      readStandardParams(ctv, geoCoordinateUnits);

      double subLonDegrees = readAttributeDouble( ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
      if (Double.isNaN(subLonDegrees)) {
         throw new IllegalArgumentException("Must specify "+CF.LONGITUDE_OF_PROJECTION_ORIGIN);
      }
      double perspective_point_height = readAttributeDouble( ctv, CF.PERSPECTIVE_POINT_HEIGHT, Double.NaN);
      if (Double.isNaN(perspective_point_height)) {
         throw new IllegalArgumentException("Must specify "+CF.PERSPECTIVE_POINT_HEIGHT);
      }

      double semi_major_axis = readAttributeDouble( ctv, CF.SEMI_MAJOR_AXIS, Double.NaN);
      if (Double.isNaN(semi_major_axis)) {
         throw new IllegalArgumentException("Must specify "+CF.SEMI_MAJOR_AXIS);
      }

      double semi_minor_axis = readAttributeDouble( ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
      double inv_flattening  = readAttributeDouble( ctv, CF.INVERSE_FLATTENING, Double.NaN);

      if (Double.isNaN(semi_minor_axis) && Double.isNaN(inv_flattening)) {
         throw new IllegalArgumentException("Must specify "+CF.SEMI_MINOR_AXIS+" and/or "+CF.INVERSE_FLATTENING);
      }
      else if (Double.isNaN(semi_minor_axis)) {
          final double flattening = 1. / inv_flattening;
          semi_minor_axis = semi_major_axis * (1. - flattening);
      }
      else if (Double.isNaN(inv_flattening))
      {
        if (semi_minor_axis != semi_major_axis) {
          final double flattening = (semi_major_axis - semi_minor_axis) / semi_major_axis;
          inv_flattening = 1. / flattening;
        }
        else {
          // Do nothing. The calculations results in inv_flattening = 1. / 0., and it is already
          // set to Double.NaN.
        }
      }
      else {
        // Both semi_minor_axis and inv_flattening are specified.
        assert (! Double.isNaN(semi_minor_axis)) && (! Double.isNaN(inv_flattening));
        // If we were obsessive, we could do a sanity test to verify the values are consistent.
      }

      String sweep_angle = ctv.findAttValueIgnoreCase(CF.SWEEP_ANGLE_AXIS, null);
      String fixed_angle = ctv.findAttValueIgnoreCase(CF.FIXED_ANGLE_AXIS, null);

      if (sweep_angle == null && fixed_angle == null)
        throw new IllegalArgumentException("Must specify "+CF.SWEEP_ANGLE_AXIS+" or "+CF.FIXED_ANGLE_AXIS);
      boolean isSweepX;
      if (sweep_angle != null)
        isSweepX =  sweep_angle.equals("x");
      else
        isSweepX =  fixed_angle.equals("y");

      // scales less than zero indicate no scaling of axis (i.e. map coords have units of radians)
      double geoCoordinateScaeFactor = defaultScaleFactor;

      geoCoordinateScaeFactor = getScaleFactor(geoCoordinateUnits);

      ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.Geostationary(
              subLonDegrees, perspective_point_height, semi_minor_axis, semi_major_axis,
              inv_flattening, isSweepX, geoCoordinateScaeFactor);

      return new ProjectionCT(ctv.getName(), "FGDC", proj);
    }

}
