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

  private DatasetNodeContainer datasetContainer;

  private PropertyContainer propertyContainer;

  private boolean isBuilt = false;


  public CatalogImpl( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.docBaseUri = docBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.serviceContainer = new ServiceContainer( null );

    this.datasetContainer = new DatasetNodeContainer( null );

    this.propertyContainer = new PropertyContainer();
  }

  DatasetNodeContainer getDatasetNodeContainer()
  {
    return this.datasetContainer;
  }

  public void setName( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void setDocBaseUri( URI docBaseUri )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null." );
    this.docBaseUri = docBaseUri;
  }

  public URI getDocBaseUri()
  {
    return this.docBaseUri;
  }

  public void setVersion( String version )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.version = version;
  }

  public String getVersion()
  {
    return this.version;
  }

  public void setExpires( Date expires )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.expires = expires;
  }

  public Date getExpires()
  {
    return this.expires;
  }

  public void setLastModified( Date lastModified )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
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
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( this.isServiceNameInUseGlobally( name) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in catalog." );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this.serviceContainer );
    this.serviceContainer.addService( sb );
    return sb;
  }

  public ServiceBuilder removeService( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
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
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without build() being called." );
    return this.serviceContainer.getServices();
  }

  public Service getServiceByName( String name )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.serviceContainer.getServiceByName( name );
  }

  public Service findServiceByNameGlobally( String name )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.serviceContainer.getServiceByGloballyUniqueName( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.serviceContainer.getServiceBuilders();
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.serviceContainer.getServiceBuilderByName( name );
  }

  public ServiceBuilder findServiceBuilderByNameGlobally( String name )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.serviceContainer.getServiceByGloballyUniqueName( name );
  }

  public void addProperty( String name, String value )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.propertyContainer.addProperty( name, value );
  }

  public boolean removeProperty( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );

    return this.propertyContainer.removeProperty( name );
  }

  public List<String> getPropertyNames()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public DatasetBuilder addDataset( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    DatasetImpl di = new DatasetImpl( name, this, null );
    this.datasetContainer.addDatasetNode( di );
    return di;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, this, null );
    this.datasetContainer.addDatasetNode( crb );
    return crb;
  }

  public boolean removeDataset( DatasetNodeBuilder builder )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( builder == null )
      throw new IllegalArgumentException( "DatasetNodeBuilder may not be null.");

    return this.datasetContainer.removeDatasetNode( (DatasetNodeImpl) builder );
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasets();
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasetById( id );
  }

  public DatasetNode findDatasetByIdGlobally( String id )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasetNodeByGloballyUniqueId( id );
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilders();
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilderById( id);
  }

  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeByGloballyUniqueId( id );
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // ToDo Check any invariants.
    // Check invariants

    // Check subordinates.
    this.serviceContainer.isBuildable( localIssues );
    this.datasetContainer.isBuildable( localIssues );
    this.propertyContainer.isBuildable( localIssues );

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public Catalog build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    // ToDo Check any invariants.
    // Check invariants

    // Check subordinates.
    this.serviceContainer.build();
    this.datasetContainer.build();
    this.propertyContainer.build();
    
    this.isBuilt = true;
    return this;
  }
}
