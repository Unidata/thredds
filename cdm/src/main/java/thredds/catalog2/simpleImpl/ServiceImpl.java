package thredds.catalog2.simpleImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuildException;

import java.net.URI;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceImpl implements Service, ServiceBuilder
{
  private String name;
  private String description;
  private ServiceType type;
  private URI baseUri;
  private String suffix;

  private PropertyContainer propertyContainer;

  private List<ServiceBuilder> serviceBuilders;
  private List<Service> services;
  private Map<String,Service> servicesMap;

  private Set<String> uniqueServiceNames;
  private ServiceImpl containerService;
  private CatalogImpl containerCatalog;

  private boolean finished = false;

  protected ServiceImpl( String name, ServiceType type, URI baseUri, CatalogImpl containerCatalog, ServiceImpl containerService )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( type == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");
    if ( containerCatalog != null && containerService != null ) throw new IllegalArgumentException( "Both container catalog and service are not null, at least one must be null.");
    this.name = name;
    this.description = "";
    this.type = type;
    this.baseUri = baseUri;
    this.suffix = "";
    this.propertyContainer = new PropertyContainer();

    this.serviceBuilders = new ArrayList<ServiceBuilder>();
    this.services = new ArrayList<Service>();
    this.servicesMap = new HashMap<String,Service>();

    this.containerCatalog = containerCatalog;
    this.containerService = containerService;
    if ( this.containerCatalog == null && this.containerService == null )
    {
      this.containerService = this;
      this.uniqueServiceNames = new HashSet<String>();
      this.uniqueServiceNames.add( name );
    }
  }

  protected void addUniqueServiceName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( ! this.uniqueServiceNames.add( name ) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in container service." );
  }

  protected boolean removeUniqueServiceName( String name )
  {
    if ( finished ) throw new IllegalStateException( "This CatalogBuilder has been finished()." );
    return this.uniqueServiceNames.add( name );
  }

  protected boolean containUniqueServiceName( String name )
  {
    return this.uniqueServiceNames.contains( name );
  }

  public String getName()
  {
    return this.name;
  }

  public void setDescription( String description )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    this.description = description != null ? description : "";
  }

  public String getDescription()
  {
    return this.description;
  }

  public void setType( ServiceType type )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( type == null )
      throw new IllegalArgumentException( "Service type must not be null." );
    this.type = type;
  }

  public ServiceType getType()
  {
    return this.type;
  }

  public void setBaseUri( URI baseUri )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( baseUri == null )
      throw new IllegalArgumentException( "Base URI must not be null." );
    this.baseUri = baseUri;
  }

  public URI getBaseUri()
  {
    return this.baseUri;
  }

  public void setSuffix( String suffix )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    this.suffix = suffix != null ? suffix : "";
  }

  public String getSuffix()
  {
    return this.suffix;
  }

  public void addProperty( String name, String value )
  {
    if ( this.finished )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    this.propertyContainer.addProperty( name, value );
  }

  public List<String> getPropertyNames()
  {
    if ( this.finished )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder before finish() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder before finish() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.finished )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );

    // Track unique service names, throw llegalStateException if name not unique.
    if ( containerCatalog != null )
      containerCatalog.addUniqueServiceName( name );
    else if ( containerService != null )
      containerService.addUniqueServiceName( name );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this.containerCatalog, this.containerService );
    this.serviceBuilders.add( sb );
    this.services.add( sb );
    this.servicesMap.put( name, sb );
    return sb;
  }

//  public ServiceBuilder addService( String name, ServiceType type, URI baseUri, int index )
//  {
//     if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished().");
// //    if ( this.getName().equals( name ) || this.servicesMap.containsKey( name ) )
//      throw new IllegalStateException( "Duplicate service name [" + name + "]." );
//    ServiceImpl sb = new ServiceImpl( name, type, baseUri );
//    this.serviceBuilders.add( index, sb );
//    this.services.add( index, sb );
//    this.servicesMap.put( name, sb );
//    return sb;
//  }

  public List<Service> getServices()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return Collections.unmodifiableList( this.services );
  }

  public Service getServiceByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return this.servicesMap.get( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return Collections.unmodifiableList( this.serviceBuilders );
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    for ( ServiceBuilder b : this.serviceBuilders )
    {
      if ( b.getName().equals( name ))
        return b;
    }
    return null;
  }

  public boolean isFinished()
  {
    return this.finished;
  }

  public Service finish() throws BuildException
  {
    if ( this.finished )
      return this;

    // Finish subordinates.
    for ( ServiceBuilder sb : this.serviceBuilders )
      sb.finish();

    // Mark finished.
    this.finished = true;
    return this;
  }
}
