/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.ProjectionCT;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;

/**
 * Lambert Cylindrical Equal Area Projection
 *
 * @author caron
 * @since 4/30/12
 */
public class LambertCylindricalEqualArea extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  public String getTransformName() {
    return CF.LAMBERT_CYLINDRICAL_EQUAL_AREA;
  }

  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String geoCoordinateUnits) {
    double par = readAttributeDouble(ctv, CF.STANDARD_PARALLEL, Double.NaN);

    readStandardParams(ctv, geoCoordinateUnits);

    // create spherical Earth obj if not created by readStandardParams w radii, flattening
    if (earth == null) {
        if (earth_radius > 0.) {
            // Earth radius obtained in readStandardParams is in km, but Earth object wants m
            earth = new Earth(earth_radius * 1000.);
        }
        else {
            earth = new Earth();
        }
    }

    ProjectionImpl proj = new CylindricalEqualAreaProjection(lon0, par, false_easting, false_northing, earth);

    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }
}
