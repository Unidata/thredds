package thredds.wcs;

import ucar.nc2.dt.GridDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Dataset
{
  public GridDataset getDataset();
  public String getDatasetPath();
  public String getDatasetName();
}
