package thredds.catalog2.simpleImpl;

import thredds.catalog2.Dataset;
import thredds.catalog2.Access;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
    if ( ! finished ) throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before finish() was called." );
    return Collections.unmodifiableList( this.accesses );
  }

  public List<Access> getAccessesByType( ServiceType type )
  {
    if ( !finished )
      throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before finish() was called." );
    List<Access> list = new ArrayList<Access>();
    for ( Access a : this.accesses )
    {
      if ( a.getService().getType().equals( type ))
        list.add( a );
    }
    return list;
  }

  public List<AccessBuilder> getAccessBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This DatasetBuilder has been finished()." );
    return Collections.unmodifiableList( this.accessBuilders );
  }

  public List<AccessBuilder> getAccessBuildersByType( ServiceType type )
  {
    if ( finished ) throw new IllegalStateException( "This DatasetBuilder has been finished()." );
    List<AccessBuilder> list = new ArrayList<AccessBuilder>();
    for ( AccessBuilder a : this.accessBuilders )
    {
      if ( a.getServiceBuilder().getType().equals( type ) )
        list.add( a );
    }
    return list;
  }

  public boolean isFinished()
  {
    return this.finished;
  }

  public Dataset finish()
  {
    if ( this.finished )
      return this;

    //ToDo Check invariants
//    // Check invariants: all access reference a service in the containing catalog.
//    for ( AccessBuilder ab : this.accessBuilders )
//    {
//      String serviceName = ab.getServiceBuilder().getName();
//      Service abs = ((CatalogSearch)this.getParentCatalogBuilder()).findServiceByName( serviceName);
//      if ( abs == null )
//        finishLog.appendBuildErrors( String message );
//    }
    // Finish subordinates.
    for ( AccessBuilder ab : this.accessBuilders )
      ab.finish();

    super.finish();
    this.finished = true;
    return this;
  }
}
