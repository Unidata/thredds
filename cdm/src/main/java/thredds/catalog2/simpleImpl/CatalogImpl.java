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
  private URI baseUri;
  private String version;
  private Date expires;
  private Date lastModified;

  private List<ServiceBuilder> serviceBuilders;
  private List<Service> services;
  private Map<String,Service> servicesMap;

  private List<DatasetNodeBuilder> datasetBuilders;
  private List<DatasetNode> datasets;
  private Map<String,DatasetNode> datasetsMapById;

  private List<Property> properties;

  private Set<String> uniqueServiceNames;

  private boolean finished = false;


  public CatalogImpl( String name, URI baseUri, String version, Date expires, Date lastModified )
  {
    if ( baseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.baseUri = baseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.serviceBuilders = new ArrayList<ServiceBuilder>();
    this.services = new ArrayList<Service>();
    this.servicesMap = new HashMap<String,Service>();

    this.datasetBuilders = new ArrayList<DatasetNodeBuilder>();
    this.datasets = new ArrayList<DatasetNode>();
    this.datasetsMapById = new HashMap<String,DatasetNode>();

    this.properties = new ArrayList<Property>();

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
    return this.baseUri;
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
    if ( !finished ) throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
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
  public Service getServiceByType( ServiceType type )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return null;
  }

  @Override
  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public ServiceBuilder getServiceBuilderByType( ServiceType type )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public List<DatasetNode> getDatasets()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return null;
    //return this.datasets;
  }

  @Override
  public List<Property> getProperties()
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return this.properties;
  }

  @Override
  public Property getPropertyByName( String name )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return null;
  }

  @Override
  public List<String> getPropertyNames()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public String getPropertyValue( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNode getDatasetById( String id )
  {
    if ( !finished )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being finish()-ed." );
    return null;
  }

  @Override
  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public void setName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
  }

  @Override
  public void setDocBaseUri( URI docBaseUri )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
  }

  @Override
  public void setVersion( String version )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
  }

  @Override
  public void setExpires( Date expires )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
  }

  @Override
  public void setLastModified( Date lastModified )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
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
    return null;
  }

  @Override
  public DatasetAliasBuilder addDatasetAlias( String name, DatasetNodeBuilder alias )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return null;
  }

  @Override
  public void addProperty( String name, String value )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
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
