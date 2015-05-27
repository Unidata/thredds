package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class CurvilinearCS extends CoverageCoordSys  {

  protected CurvilinearCS(CoverageCoordSysBuilder builder, CoordinateSystem cs) {
    super(builder, cs);
  }

  @Override
  public CoordinateAxis2D getLatAxis() {
    return (CoordinateAxis2D) super.getLatAxis();
  }

  @Override
  public CoordinateAxis2D getLonAxis() {
    return (CoordinateAxis2D) super.getLonAxis();
  }

}
