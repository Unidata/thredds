/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a Sinusoidal Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 * @since 2/24/13
 */
public class Sinusoidal extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.SINUSOIDAL;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double centralMeridian = readAttributeDouble( ctv, CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    double false_easting = readAttributeDouble(ctv, CF.FALSE_EASTING, 0.0);
    double false_northing = readAttributeDouble(ctv, CF.FALSE_NORTHING, 0.0);
    double earth_radius = getEarthRadiusInKm(ctv);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(geoCoordinateUnits);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    ucar.unidata.geoloc.projection.Sinusoidal proj =
            new ucar.unidata.geoloc.projection.Sinusoidal( centralMeridian, false_easting, false_northing, earth_radius);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
