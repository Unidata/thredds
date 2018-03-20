/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.projection.UtmProjection;

/**
 * Create a UTM Projection from the information in the Coordinate Transform Variable.
 *  *
 * @author caron
 */
public class UTM extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return UtmProjection.GRID_MAPPING_NAME;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double zoned = readAttributeDouble( ctv, UtmProjection.UTM_ZONE1, Double.NaN);
    if (Double.isNaN(zoned))
      zoned = readAttributeDouble( ctv, UtmProjection.UTM_ZONE2, Double.NaN);
    if (Double.isNaN(zoned))
      throw new IllegalArgumentException("No zone was specified") ;

    int zone = (int) zoned;
    boolean isNorth = zone > 0;
    zone = Math.abs(zone);

    Attribute a;
    double axis = 0.0, f = 0.0;
    if (null != (a = ctv.findAttribute( "semimajor_axis")))
      axis = a.getNumericValue().doubleValue();
    if (null != (a = ctv.findAttribute( "inverse_flattening")))
      f = a.getNumericValue().doubleValue();

    // double a, double f, int zone, boolean isNorth
    UtmProjection proj = (axis != 0.0) ? new UtmProjection(axis, f, zone, isNorth) : new UtmProjection(zone, isNorth);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
