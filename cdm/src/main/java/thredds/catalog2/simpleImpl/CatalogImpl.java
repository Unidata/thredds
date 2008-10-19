package thredds.catalog2.simpleImpl;

import thredds.catalog2.*;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogImpl implements Catalog, CatalogBuilder
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private String name;
  private URI docBaseUri;
  private String version;
  private Date expires;
  private Date lastModified;

  private ServiceContainer serviceContainer;

  private List<DatasetNodeBuilder> datasetBuilders;
  private List<DatasetNode> datasets;
  private Map<String,DatasetNode> datasetsMapById;

  private PropertyContainer propertyContainer;

  private boolean finished = false;


  public CatalogImpl( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.docBaseUri = docBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.serviceContainer = new ServiceContainer( null );

    this.datasetBuilders = new ArrayList<DatasetNodeBuilder>();
    this.datasets = new ArrayList<DatasetNode>();
    this.datasetsMapById = new HashMap<String,DatasetNode>();

    this.propertyContainer = new PropertyContainer();
  }

  public void setName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void setDocBaseUri( URI docBaseUri )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null." );
    this.docBaseUri = docBaseUri;
  }

  public URI getDocBaseUri()
  {
    return this.docBaseUri;
  }

  public void setVersion( String version )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.version = version;
  }

  public String getVersion()
  {
    return this.version;
  }

  public void setExpires( Date expires )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.expires = expires;
  }

  public Date getExpires()
  {
    return this.expires;
  }

  public void setLastModified( Date lastModified )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.lastModified = lastModified;
  }

  public Date getLastModified()
  {
    return this.lastModified;
  }

  public boolean isServiceNameInUseGlobally( String name )
  {
    return this.serviceContainer.isServiceNameInUseGlobally( name );
  }

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( this.isServiceNameInUseGlobally( name) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in catalog." );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this.serviceContainer );
    this.serviceContainer.addService( sb );
    return sb;
  }

  public ServiceBuilder removeService( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( name == null )
      return null;

    ServiceImpl removedService = this.serviceContainer.removeService( name );
    if ( removedService == null )
    {
      log.debug( "removeService(): unknown ServiceBuilder [" + name + "] (not in map)." );
      return null;
    }

    return removedService;
  }

  public List<Service> getServices()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without build() being called." );
    return this.serviceContainer.getServices();
  }

  public Service getServiceByName( String name )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.serviceContainer.getServiceByName( name );
  }

  public Service findServiceByNameGlobally( String name )
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.serviceContainer.getServiceBuilders();
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.serviceContainer.getServiceBuilderByName( name );
  }

  public ServiceBuilder findServiceBuilderByNameGlobally( String name )
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void addProperty( String name, String value )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.propertyContainer.addProperty( name, value );
  }

  public boolean removeProperty( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );

    return this.propertyContainer.removeProperty( name );
  }

  public List<String> getPropertyNames()
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public DatasetBuilder addDataset( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    DatasetImpl db = new DatasetImpl( name, this, null );
    this.datasetBuilders.add( db );
    this.datasets.add( db );
    return db;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, this, null );
    this.datasetBuilders.add( crb );
    this.datasets.add( crb );
    return crb;
  }

  public boolean removeDataset( DatasetNodeBuilder builder )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( builder == null )
      throw new IllegalArgumentException( "DatasetNodeBuilder may not be null.");

    if ( this.datasetBuilders.remove( builder ))
    {
      if ( this.datasets.remove( builder ))
        return true;
      else
        log.warn( "removeDataset(): inconsistent failure to remove DatasetNodeBuilder [" + builder.getName() + "] (from object list)." );
    }
    else
      log.warn( "removeDataset(): inconsistent failure to remove DatasetNodeBuilder [" + builder.getName() + "] (from builder list)." );

    return false;
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return Collections.unmodifiableList( this.datasets );
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetsMapById.get( id );
  }

  public DatasetNode findDatasetByIdGlobally( String id )
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return Collections.unmodifiableList( this.datasetBuilders );
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return (DatasetNodeBuilder) this.datasetsMapById.get( id);
  }

  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id )
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.finished )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // ToDo Check any invariants.
    // Check invariants
    // ToDo check that all datasets with Ids have unique Ids

    // Check subordinates.
    this.serviceContainer.isBuildable( localIssues );
    for ( DatasetNodeBuilder dnb : this.datasetBuilders )
      dnb.isBuildable( localIssues );
    this.propertyContainer.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public Catalog build() throws BuilderException
  {
    if ( this.finished )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    // ToDo Check any invariants.
    // Check invariants
    // ToDo check that all datasets with Ids have unique Ids

    // Check subordinates.
    this.serviceContainer.build();
    for ( DatasetNodeBuilder dnb : this.datasetBuilders )
      dnb.build();
    this.propertyContainer.build();
    
    this.finished = true;
    return this;
  }
}
