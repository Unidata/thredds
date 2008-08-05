package thredds.catalog2.simpleImpl;

import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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

  private boolean finished = false;

  protected DatasetImpl( String name, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    super( name, parentCatalog, parent);

    this.accessBuilders = new ArrayList<AccessBuilder>();
    this.accesses = new ArrayList<Access>();
  }

  public AccessBuilder addAccess( ServiceBuilder service, String urlPath )
  {
    if ( finished ) throw new IllegalStateException( "This DatasetBuilder has been finished().");
    AccessImpl a = new AccessImpl( (ServiceImpl) service, urlPath );
    this.accessBuilders.add( a );
    this.accesses.add( a );
    return a;
  }

  public boolean isAccessible()
  {
    return ! this.accessBuilders.isEmpty();
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
    if ( this.finished )
      return this;

    super.finish();
    this.finished = true;
    return this;
  }
}
