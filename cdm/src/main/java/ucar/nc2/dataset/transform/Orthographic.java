/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a Orthographic Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class Orthographic extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.ORTHOGRAPHIC;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon0 = readAttributeDouble( ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = readAttributeDouble( ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);

    ucar.unidata.geoloc.projection.Orthographic proj = new ucar.unidata.geoloc.projection.Orthographic(lat0, lon0);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
