package ucar.nc2.dataset.transform;

import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;

/**
 * AzimuthalEquidistant Projection.
 *
 * @author caron
 * @since 4/30/12
 */
public class AzimuthalEquidistant extends AbstractCoordTransBuilder {

  public String getTransformName() {
    return CF.AZIMUTHAL_EQUIDISTANT;
  }

  public TransformType getTransformType() {
    return TransformType.Projection;
  }

  public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds, Variable ctv) {

    readStandardParams(ds, ctv);

    ucar.unidata.geoloc.ProjectionImpl proj = new EquidistantAzimuthalProjection(lat0, lon0, false_easting, false_northing, earth);

    return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
  }
}
