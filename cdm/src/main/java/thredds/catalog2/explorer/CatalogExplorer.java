package thredds.catalog2.explorer;

import thredds.catalog.ServiceType;
import thredds.catalog2.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogExplorer extends Catalog
{
  public Service getServiceByName( String name );

  public Service getServiceByType( ServiceType type );

  public <T extends DatasetNode> T getDatasetByName( String name );

  public <T extends DatasetNode> T getDatasetById( String id );

  public Property getPropertyByName( String name);
}
