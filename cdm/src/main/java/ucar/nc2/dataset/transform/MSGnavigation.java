/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * MSGnavigation projection
 *
 * @author caron
 * @since Jan 9, 2010
 */

public class MSGnavigation extends AbstractTransformBuilder implements HorizTransformBuilderIF {

    public String getTransformName() {
      return "MSGnavigation";
    }

    public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {

      double lon0 = readAttributeDouble( ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
      double lat0 = readAttributeDouble( ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
      double minor_axis = readAttributeDouble( ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
      double major_axis = readAttributeDouble( ctv, CF.SEMI_MAJOR_AXIS, Double.NaN);
      double height = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.sat.MSGnavigation.HEIGHT_FROM_EARTH_CENTER, Double.NaN);
      double scale_x = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.sat.MSGnavigation.SCALE_X, Double.NaN);
      double scale_y = readAttributeDouble( ctv, ucar.unidata.geoloc.projection.sat.MSGnavigation.SCALE_Y, Double.NaN);

      ProjectionImpl proj = new ucar.unidata.geoloc.projection.sat.MSGnavigation(lat0, lon0, major_axis, minor_axis, height, scale_x, scale_y);
      return new ProjectionCT(ctv.getName(), "FGDC", proj);
    }

}
