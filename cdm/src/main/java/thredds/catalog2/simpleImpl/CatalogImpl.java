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
  private String name;
  private URI docBaseUri;
  private String version;
  private Date expires;
  private Date lastModified;

  private List<ServiceBuilder> serviceBuilders;
  private List<Service> services;
  private Map<String,Service> servicesMap;

  private List<DatasetNodeBuilder> datasetBuilders;
  private List<DatasetNode> datasets;
  private Map<String,DatasetNode> datasetsMapById;

  private PropertyContainer propertyContainer;

  private Set<String> uniqueServiceNames;

  private boolean finished = false;


  public CatalogImpl( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  {
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.docBaseUri = docBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.serviceBuilders = new ArrayList<ServiceBuilder>();
    this.services = new ArrayList<Service>();
    this.servicesMap = new HashMap<String,Service>();

    this.datasetBuilders = new ArrayList<DatasetNodeBuilder>();
    this.datasets = new ArrayList<DatasetNode>();
    this.datasetsMapById = new HashMap<String,DatasetNode>();

    this.propertyContainer = new PropertyContainer();

    this.uniqueServiceNames = new HashSet<String>();
  }

  protected void addUniqueServiceName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished().");
    if ( ! this.uniqueServiceNames.add( name ) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in catalog.");
  }

  protected boolean removeUniqueServiceName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished().");
    return this.uniqueServiceNames.add( name );
  }

  protected boolean containUniqueServiceName( String name )
  {
    return this.uniqueServiceNames.contains( name );
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public URI getDocBaseUri()
  {
    return this.docBaseUri;
  }

  @Override
  public String getVersion()
  {
    return this.version;
  }

  @Override
  public Date getExpires()
  {
    return this.expires;
  }

  @Override
  public Date getLastModified()
  {
    return this.lastModified;
  }

  @Override
  public List<Service> getServices()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without finish() being called." );
    return Collections.unmodifiableList( this.services);
  }

  @Override
  public Service getServiceByName( String name )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return this.servicesMap.get( name );
  }

  @Override
  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return Collections.unmodifiableList( this.serviceBuilders );
  }

  @Override
  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return (ServiceBuilder) this.servicesMap.get( name );
  }

  @Override
  public List<DatasetNode> getDatasets()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return Collections.unmodifiableList( this.datasets );
  }

  @Override
  public void addProperty( String name, String value )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.propertyContainer.addProperty( name, value );
  }

  @Override
  public List<String> getPropertyNames()
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  @Override
  public String getPropertyValue( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  @Override
  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before finish() was called." );
    return this.propertyContainer.getProperties();
  }

  @Override
  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before finish() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  @Override
  public DatasetNode getDatasetById( String id )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return this.datasetsMapById.get( id );
  }

  @Override
  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return Collections.unmodifiableList( this.datasetBuilders );
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return (DatasetNodeBuilder) this.datasetsMapById.get( id);
  }

  @Override
  public void setName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.name = name;
  }

  @Override
  public void setDocBaseUri( URI docBaseUri )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null." );
    this.docBaseUri = docBaseUri;
  }

  @Override
  public void setVersion( String version )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.version = version;
  }

  @Override
  public void setExpires( Date expires )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.expires = expires;
  }

  @Override
  public void setLastModified( Date lastModified )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    this.lastModified = lastModified;
  }

  @Override
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( finished )
      throw new IllegalStateException( "This CatalogBuilder has been finished()." );

    // Track unique service names, throw llegalStateException if name not unique.
    this.addUniqueServiceName( name );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this, null );
    this.serviceBuilders.add( sb );
    this.services.add( sb );
    this.servicesMap.put( name, sb );
    return null;
  }

  @Override
  public DatasetBuilder addDataset( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    DatasetImpl db = new DatasetImpl( name, this, null );
    this.datasetBuilders.add( db );
    this.datasets.add( db );
    return db;
  }

  @Override
  public DatasetAliasBuilder addDatasetAlias( String name, DatasetNodeBuilder alias )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    DatasetAliasImpl dab = new DatasetAliasImpl( name, alias, this, null );
    this.datasetBuilders.add( dab );
    this.datasets.add( dab );
    return dab;
  }

  @Override
  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, this, null );
    this.datasetBuilders.add( crb );
    this.datasets.add( crb );
    return crb;
  }

  @Override
  public boolean isFinished()
  {
    return this.finished;
  }

  @Override
  public Catalog finish()
  {
    this.finished = true;
    return this;
  }
}
