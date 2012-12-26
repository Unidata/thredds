package ucar.nc2.ft.grid;

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;

/**
 * Description
 *
 * @author John
 * @since 12/23/12
 */
public interface SwathCS extends CoverageCS {

  /**
   * True if both X and Y axes are regularly spaced.
   *
   * @return true if both X and Y axes are regularly spaced.
   */
  public boolean isRegularSpatial();

  /**
   * Get the lat axis. Must be 2 dimensional.
   *
   * @return X CoordinateAxis, may not be null.
   */
  public CoordinateAxis2D getLatAxis();

  /**
   * Get the lon axis. Must be 2 dimensional.
   *
   * @return Y CoordinateAxis, may not be null.
   */
  public CoordinateAxis2D getLonAxis();

  /**
   * Get the T axis. Must be 1 dimensional if it exists.
   *
   * @return T CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getTimeAxis();

}
