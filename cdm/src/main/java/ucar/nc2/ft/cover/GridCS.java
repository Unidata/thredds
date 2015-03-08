package ucar.nc2.ft.cover;

import ucar.nc2.dataset.*;

/**
 * A Coordinate System for gridded data. All axes are 1D and orthogonal.
 * @author John
 * @since 12/23/12
 */
public interface GridCS extends CoverageCS {

  /**
   * True if both X and Y axes are regularly spaced.
   *
   * @return true if both X and Y axes are regularly spaced.
   */
  public boolean isRegularSpatial();

  /**
   * Get the X axis. Must be 1 dimensional.
   *
   * @return X CoordinateAxis, may not be null.
   */
  public CoordinateAxis1D getXHorizAxis();

  /**
   * Get the Y axis. Must be 1 dimensional.
   *
   * @return Y CoordinateAxis, may not be null.
   */
  public CoordinateAxis1D getYHorizAxis();

  /**
   * Get the Z axis. Must be 1 dimensional if it exists.
   *
   * @return Z CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getVerticalAxis();

  /**
   * Get the T axis. Must be 1 dimensional if it exists.
   *
   * @return T CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getTimeAxis();

}
