package thredds.wcs;

import ucar.nc2.dt.GridCoordSystem;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Coverage
{
  public String getName();

  public String getLabel();

  public String getDescription();

  public GridCoordSystem getCoordinateSystem();
}
