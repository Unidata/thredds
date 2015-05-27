package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;

/**
 * Grid Coordinate System Implementation
 * A Grid has all 1D coordinates.
 *
 * @author John
 * @since 12/25/12
 */
public class GridCS extends CoverageCoordSys {

  GridCS(CoverageCoordSysBuilder builder, CoordinateSystem cs) {
    super(builder, cs);
  }

  @Override
  public boolean isRegularSpatial() {
    return getXHorizAxis().isRegular() && getYHorizAxis().isRegular();
  }

  public CoordinateAxis1D getXHorizAxis() {
    return (CoordinateAxis1D) super.getXHorizAxis();
  }

  public CoordinateAxis1D getYHorizAxis() {
    return (CoordinateAxis1D) super.getYHorizAxis();
  }

  public CoordinateAxis1D getVerticalAxis() {
    return (CoordinateAxis1D) super.getVerticalAxis();
  }

  public CoordinateAxis1D getTimeAxis() {
    return (CoordinateAxis1D) super.getTimeAxis();
  }

}
