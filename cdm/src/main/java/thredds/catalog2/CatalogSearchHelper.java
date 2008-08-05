package thredds.catalog2;

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
  public DatasetNode findDatasetById( String id );
  public List<Dataset> findAccessibleDatasets();
  public List<CatalogRef> findCatalogRefs();
}
