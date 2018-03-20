/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;

/**
 * Create a Rotated LatLon Projection from the information in the Coordinate Transform Variable.
 */
public class RotatedLatLon extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return ucar.unidata.geoloc.projection.RotatedLatLon.GRID_MAPPING_NAME;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double lon = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LONGITUDE, Double.NaN);
    double lat = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_LATITUDE, Double.NaN);
    double angle = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.RotatedLatLon.GRID_SOUTH_POLE_ANGLE, Double.NaN);

    ucar.unidata.geoloc.projection.RotatedLatLon proj = new ucar.unidata.geoloc.projection.RotatedLatLon( lat, lon, angle);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }

}


