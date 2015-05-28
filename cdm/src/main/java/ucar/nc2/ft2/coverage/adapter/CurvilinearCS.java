package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class CurvilinearCS extends DtCoverageCS {

  protected CurvilinearCS(DtCoverageCSBuilder builder) {
    super(builder);
  }

  public CoordinateAxis2D getLatAxis() {
    return (CoordinateAxis2D) super.getXHorizAxis();
  }

  public CoordinateAxis2D getLonAxis() {
    return (CoordinateAxis2D) super.getYHorizAxis();
  }

}
