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
  private List<AccessImpl> accessImplList;

  private boolean isBuilt = false;

  protected DatasetImpl( String name, CatalogImpl parentCatalog, DatasetNodeImpl parent )
  {
    super( name, parentCatalog, parent);
  }

  public AccessBuilder addAccessBuilder()
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built.");
    AccessImpl a = new AccessImpl();
    if ( this.accessImplList == null )
      this.accessImplList = new ArrayList<AccessImpl>();
    this.accessImplList.add( a );
    return a;
  }

  public boolean removeAccessBuilder( AccessBuilder accessBuilder )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );

    if ( this.accessImplList == null )
      return false;
    return this.accessImplList.remove( accessBuilder );
  }

  public boolean isAccessible()
  {
    if ( this.accessImplList == null )
      return false;
    return ! this.accessImplList.isEmpty();
  }

  public List<Access> getAccesses()
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before build() was called." );
    if ( this.accessImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Access>(this.accessImplList ));
  }

  public List<Access> getAccessesByType( ServiceType type )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Dataset has escaped its DatasetBuilder before build() was called." );
    List<Access> list = new ArrayList<Access>();
    if ( this.accessImplList != null )
      for ( Access a : this.accessImplList )
        if ( a.getService().getType().equals( type ))
          list.add( a );
    return list;
  }

  public List<AccessBuilder> getAccessBuilders()
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );
    if ( this.accessImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<AccessBuilder>( this.accessImplList ));
  }

  public List<AccessBuilder> getAccessBuildersByType( ServiceType type )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This DatasetBuilder has been built." );
    List<AccessBuilder> list = new ArrayList<AccessBuilder>();
    if ( this.accessImplList != null )
      for ( AccessBuilder a : this.accessImplList )
        if ( a.getServiceBuilder().getType().equals( type ) )
          list.add( a );
    return list;
  }

  @Override
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();
    super.isBuildable( issues );

    // Check subordinates.
    if ( this.accessImplList != null )
      for ( AccessBuilder ab : this.accessImplList )
        ab.isBuildable( localIssues );

    //ToDo Check invariants
//    // Check invariants: all access reference a service in the containing catalog.
//    for ( AccessBuilder ab : this.accessImplList )
//    {
//      String serviceName = ab.getServiceBuilder().getName();
//      Service abs = ((CatalogSearch)this.getParentCatalogBuilder()).findServiceByName( serviceName);
//      if ( abs == null )
//        finishLog.appendBuildErrors( String message );
//    }

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  @Override
  public Dataset build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( ! isBuildable( issues ) )
      throw new BuilderException( issues );

    super.build();

    // Check subordinates.
    if ( this.accessImplList != null )
      for ( AccessBuilder ab : this.accessImplList )
        ab.build();

    this.isBuilt = true;
    return this;
  }
}
