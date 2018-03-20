/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.Earth;

/**
 * Polyconic Projection.
 * @author ghansham@sac.isro.gov.in 1/8/2012
 */
public class PolyconicProjection extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {

    double lon0 = readAttributeDouble(ctv, "longitude_of_central_meridian", Double.NaN);
    double lat0 = readAttributeDouble(ctv, "latitude_of_projection_origin", Double.NaN);

    double semi_major_axis = readAttributeDouble(ctv, "semi_major_axis", Double.NaN);
    double semi_minor_axis = readAttributeDouble(ctv, "semi_minor_axis", Double.NaN);
    double inverse_flattening = readAttributeDouble(ctv, "inverse_flattening", 0.0);

    ucar.unidata.geoloc.ProjectionImpl proj;

    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      Earth earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
      proj = new ucar.unidata.geoloc.projection.proj4.PolyconicProjection(lat0, lon0, earth);
    } else {
      proj = new ucar.unidata.geoloc.projection.proj4.PolyconicProjection(lat0, lon0);
    }

    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }

  public String getTransformName() {
    return "polyconic";
  }
}
