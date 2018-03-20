/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;

/**
 * Create a AlbersEqualArea Projection from the information in the Coordinate Transform Variable.
 *
 * @author caron
 */
public class AlbersEqualArea extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.ALBERS_CONICAL_EQUAL_AREA;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double[] pars = readAttributeDouble2(ctv.findAttribute(CF.STANDARD_PARALLEL));
    if (pars == null) return null;

    readStandardParams(ctv, geoCoordinateUnits);

    ucar.unidata.geoloc.ProjectionImpl proj;

    if (earth != null) {
      proj = new AlbersEqualAreaEllipse(lat0, lon0, pars[0], pars[1], false_easting, false_northing, earth);

    } else {
      proj = new ucar.unidata.geoloc.projection.AlbersEqualArea(lat0, lon0, pars[0], pars[1], false_easting, false_northing, earth_radius);
    }

    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
