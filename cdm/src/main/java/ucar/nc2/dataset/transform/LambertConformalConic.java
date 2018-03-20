/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.Earth;

/**
 * Create a LambertConformalConic Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class LambertConformalConic extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.LAMBERT_CONFORMAL_CONIC;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double[] pars = readAttributeDouble2(ctv.findAttribute( CF.STANDARD_PARALLEL));
    if (pars == null) return null;

    double lon0 = readAttributeDouble( ctv, CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double lat0 = readAttributeDouble( ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double false_easting = readAttributeDouble(ctv, CF.FALSE_EASTING, 0.0);
    double false_northing = readAttributeDouble(ctv, CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double earth_radius = getEarthRadiusInKm(ctv);
    double semi_major_axis = readAttributeDouble(ctv, CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = readAttributeDouble(ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = readAttributeDouble(ctv, CF.INVERSE_FLATTENING, 0.0);

    ucar.unidata.geoloc.ProjectionImpl proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse(lat0, lon0, pars[0], pars[1], false_easting, false_northing, earth);

    } else {
      proj = new ucar.unidata.geoloc.projection.LambertConformal(lat0, lon0, pars[0], pars[1], false_easting, false_northing, earth_radius);
    }

    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
