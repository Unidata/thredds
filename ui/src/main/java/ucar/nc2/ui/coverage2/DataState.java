/* Copyright */
package ucar.nc2.ui.coverage2;

import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.*;

/**
 * Describe
 *
 * @author caron
 * @since 7/21/2015
 */
class DataState {
  CoverageCollection coverageDataset;
  Coverage grid;
  CoverageCoordSys geocs;
  CoverageCoordAxis1D zaxis;
  CoverageCoordAxis1D taxis;
  CoverageCoordAxis1D rtaxis;
  CoverageCoordAxis1D ensaxis;
  TimeAxis2DFmrc taxis2D;

  public DataState(CoverageCollection coverageDataset, Coverage grid) {
    this.coverageDataset = coverageDataset;
    this.grid = grid;
    this.geocs = grid.getCoordSys();
    CoverageCoordAxis zaxis = geocs.getZAxis();
    if (zaxis != null && zaxis instanceof CoverageCoordAxis1D)
      this.zaxis = (CoverageCoordAxis1D) zaxis;

    CoverageCoordAxis taxis = geocs.getTimeAxis();
    if (taxis != null && taxis instanceof CoverageCoordAxis1D)
      this.taxis = (CoverageCoordAxis1D) taxis;
    if (taxis != null && taxis instanceof TimeAxis2DFmrc)
      this.taxis2D = (TimeAxis2DFmrc) taxis;

    CoverageCoordAxis rtaxis = geocs.getAxis(AxisType.RunTime);
    if (rtaxis != null && rtaxis instanceof CoverageCoordAxis1D)
      this.rtaxis = (CoverageCoordAxis1D) rtaxis;

    CoverageCoordAxis eaxis = geocs.getAxis(AxisType.Ensemble);
    if (eaxis != null && eaxis instanceof CoverageCoordAxis1D)
      this.ensaxis = (CoverageCoordAxis1D) eaxis;

  }
}
