package ucar.nc2.ft.cover;

import ucar.nc2.dataset.CoordinateAxis2D;

/**
 * A CoverageCS with a 2D lat/lon coordinate
 *
 * @author John
 * @since 12/23/12
 */
public interface CurvilinearCS extends CoverageCS {

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

}
