package thredds.catalog2.explorer;

import thredds.catalog.ServiceType;
import thredds.catalog2.Service;
import thredds.catalog2.Dataset;
import thredds.catalog2.Property;
import thredds.catalog2.Catalog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogExplorer extends Catalog
{
  Service getServiceByName( String name);

  Service getServiceByType( ServiceType type);

  public Dataset getDatasetByName( String name);

  public Dataset getDatasetById( String id);

  public Property getPropertyByName( String name);
}
