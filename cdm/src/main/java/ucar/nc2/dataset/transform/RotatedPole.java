/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a RotatedPole Projection from the information in the Coordinate Transform Variable.
 * This is from CF. Grib is RotatedLatLon
 * @author caron
 */
public class RotatedPole extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.ROTATED_LATITUDE_LONGITUDE;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon = readAttributeDouble( ctv, CF.GRID_NORTH_POLE_LONGITUDE, Double.NaN);
    double lat = readAttributeDouble( ctv, CF.GRID_NORTH_POLE_LATITUDE, Double.NaN);

    ucar.unidata.geoloc.projection.RotatedPole proj = new ucar.unidata.geoloc.projection.RotatedPole( lat, lon);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }

}
