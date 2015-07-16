/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageTransform;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/2015
 */
class DataResponse {

  public List<CoverageCoordAxis> axes;
  public List<CoverageCoordSys> coordSys;
  public List<CoverageTransform> transforms;

  public List<GeoArrayResponse> arrayResponse;

  public DataResponse(List<CoverageCoordAxis> axes, List<CoverageCoordSys> coordSys, List<CoverageTransform> transforms, List<GeoArrayResponse> arrayResponse) {
    this.axes = axes;
    this.coordSys = coordSys;
    this.transforms = transforms;
    this.arrayResponse = arrayResponse;
  }
}
