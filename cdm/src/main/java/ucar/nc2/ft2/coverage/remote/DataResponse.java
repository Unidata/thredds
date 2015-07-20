/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import ucar.nc2.ft2.coverage.*;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/2015
 */
class DataResponse implements CoordSysContainer {

  public List<CoverageCoordAxis> axes;
  public List<CoverageCoordSys> coordSys;
  public List<CoverageTransform> transforms;

  public List<GeoArrayResponse> arrayResponse;

  public DataResponse(List<CoverageCoordAxis> axes, List<CoverageCoordSys> coordSys, List<CoverageTransform> transforms, List<GeoArrayResponse> arrayResponse) {
    this.axes = axes;
    this.coordSys = coordSys;
    this.transforms = transforms;
    this.arrayResponse = arrayResponse;

    for (CoverageCoordSys csys : coordSys)
      csys.setDataset(this);
  }

  public CoverageCoordSys findCoordSys(String csysName) {
    for (CoverageCoordSys csys : coordSys)
      if (csys.getName().equalsIgnoreCase(csysName)) return csys;
    return null;
  }

  public CoverageTransform findCoordTransform(String transformName) {
    for (CoverageTransform ct : transforms)
      if (ct.getName().equalsIgnoreCase(transformName)) return ct;
    return null;
  }

  public CoverageCoordAxis findCoordAxis(String axisName) {
    for (CoverageCoordAxis axis : axes) {
      if (axis.getName().equalsIgnoreCase(axisName)) return axis;
    }
    return null;
  }
}
