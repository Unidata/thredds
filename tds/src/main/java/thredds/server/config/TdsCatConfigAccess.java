package thredds.server.config;

import thredds.catalog.InvCatalog;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface TdsCatConfigAccess
{
  public InvCatalog findStaticCatalog( String catalogPath );

  public InvCatalog findScannedCatalog( String catalogPath );

  // Or should this return a File? or ?
  public NetcdfDataset findDataset( String datasetPath );
}
