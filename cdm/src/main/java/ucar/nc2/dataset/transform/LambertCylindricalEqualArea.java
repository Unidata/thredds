package ucar.nc2.dataset.transform;

import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;

/**
 * Lambert Cylindrical Equal Area Projection
 *
 * @author caron
 * @since 4/30/12
 */
public class LambertCylindricalEqualArea extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return CF.LAMBERT_CYLINDRICAL_EQUAL_AREA;
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {
    double par = readAttributeDouble(ctv, CF.STANDARD_PARALLEL, Double.NaN);
    
    readStandardParams(ds, ctv);

    ucar.unidata.geoloc.ProjectionImpl proj = new CylindricalEqualAreaProjection(lon0, par, false_easting, false_northing, earth);

    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
