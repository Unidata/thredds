package thredds.catalog2.simpleImpl;

import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetImpl
        extends DatasetNodeImpl
        implements Dataset, DatasetBuilder
{
  private boolean accessible = false;
  private List<AccessBuilder> accessBuilders;
  private List<Access> accesses;
  private Map<ServiceType,Access> accessMap;

  private boolean finished = false;

  protected DatasetImpl( String name, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    super( name, parentCatalog, parent);
  }

  public AccessBuilder addAccess( ServiceBuilder service, String urlPath )
  {
    return null;
  }

  public boolean isAccessible()
  {
    return false;
  }

  public List<Access> getAccesses()
  {
    return null;
  }

  public Access getAccessByType( ServiceType type )
  {
    return null;
  }

  public List<AccessBuilder> getAccessBuilders()
  {
    return null;
  }

  public AccessBuilder getAccessBuilderByType( ServiceType type )
  {
    return null;
  }

  public boolean isFinished()
  {
    return false;
  }

  public Dataset finish()
  {
    super.finish();
    this.finished = true;
    return this;
  }
}
