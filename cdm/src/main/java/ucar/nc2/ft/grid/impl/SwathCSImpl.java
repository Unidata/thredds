package ucar.nc2.ft.grid.impl;

import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.grid.SwathCS;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class SwathCSImpl extends CoverageCSImpl implements SwathCS {

  protected SwathCSImpl(NetcdfDataset ds, CoordinateSystem cs, CoverageCSFactory fac) {
    super(ds, cs, fac);
  }

  @Override
  public CoordinateAxis2D getLatAxis() {
    return (CoordinateAxis2D) cs.getLatAxis();
  }

  @Override
  public CoordinateAxis2D getLonAxis() {
    return (CoordinateAxis2D) cs.getLonAxis();
  }

}
