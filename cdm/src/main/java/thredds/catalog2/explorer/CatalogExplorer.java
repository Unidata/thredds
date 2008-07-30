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
  /**
   * Return the named Service or null if no such service exists. All Service
   * objects contained in this Catalog are searched including nested Service
   * objects.
   *
   * @param name the name of the service to find.
   * @return the Service object with the given name or null if none exists.
   */
  public Service getServiceByName( String name );

  public Service getServiceByType( ServiceType type );

  public <T extends DatasetNode> T getDatasetByName( String name );

  public <T extends DatasetNode> T getDatasetById( String id );

  public Property getPropertyByName( String name);
}
