package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogSearchHelper
{
  public Service findServiceByName( String name );
  public List<Service> findServiceByType( ServiceType type );
  public DatasetNode findDatasetById( String id );
  public List<Dataset> findAccessibleDatasets();
  public List<Dataset> findAccessibleDatasetsByType( ServiceType type );
  public List<CatalogRef> findCatalogRefs();
}
