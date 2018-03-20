/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;

/**
 * VerticalPerspectiveView projection.
 *
 * @author caron
 */
public class VerticalPerspective extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.VERTICAL_PERSPECTIVE;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {

    readStandardParams(ctv, geoCoordinateUnits);

    double distance = readAttributeDouble(ctv, CF.PERSPECTIVE_POINT_HEIGHT, Double.NaN);
    if (Double.isNaN(distance)) {
        distance = readAttributeDouble(ctv, "height_above_earth", Double.NaN);
    }
    if (Double.isNaN(lon0) || Double.isNaN(lat0) || Double.isNaN(distance))
      throw new IllegalArgumentException("Vertical Perspective must have: " +
              CF.LONGITUDE_OF_PROJECTION_ORIGIN + ", " +
              CF.LATITUDE_OF_PROJECTION_ORIGIN + ", and " +
              CF.PERSPECTIVE_POINT_HEIGHT + "(or height_above_earth) " +
              "attributes");

    // We assume distance comes in 'm' (CF-compliant) and we pass in as 'km'
    ucar.unidata.geoloc.projection.VerticalPerspectiveView proj =
            new ucar.unidata.geoloc.projection.VerticalPerspectiveView(lat0,
                    lon0, earth_radius, distance / 1000., false_easting,
                    false_northing);

    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
