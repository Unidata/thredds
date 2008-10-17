package thredds.catalog2.simpleImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderFinishIssue;

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
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private String name;
  private String description;
  private ServiceType type;
  private URI baseUri;
  private String suffix;

  private PropertyContainer propertyContainer;

  private ServiceContainer serviceContainer;

  private boolean isBuilt = false;

  protected ServiceImpl( String name, ServiceType type, URI baseUri, ServiceContainer rootContainer )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( type == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");

    this.name = name;
    this.description = "";
    this.type = type;
    this.baseUri = baseUri;
    this.suffix = "";
    this.propertyContainer = new PropertyContainer();

    this.serviceContainer = new ServiceContainer( rootContainer );
  }

  public String getName()
  {
    return this.name;
  }

  public void setDescription( String description )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
    this.description = description != null ? description : "";
  }

  public String getDescription()
  {
    return this.description;
  }

  public void setType( ServiceType type )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
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
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
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
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
    this.suffix = suffix != null ? suffix : "";
  }

  public String getSuffix()
  {
    return this.suffix;
  }

  public void addProperty( String name, String value )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    this.propertyContainer.addProperty( name, value );
  }

  public boolean removeProperty( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.propertyContainer.removeProperty( name );
  }

  public List<String> getPropertyNames()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public boolean isServiceNameInUseGlobally( String name )
  {
    return this.serviceContainer.isServiceNameInUseGlobally( name );
  }

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    if ( this.isServiceNameInUseGlobally( name ) )
      throw new IllegalStateException( "Given service name [" + name + "] not unique in catalog." );

    ServiceImpl sb = new ServiceImpl( name, type, baseUri, this.serviceContainer.getRootServiceContainer() );
    this.serviceContainer.addService( sb );
    return sb;
  }

  public boolean removeService( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( name == null )
      return false;

    if ( ! this.serviceContainer.removeService( name ) )
    {
      log.debug( "removeService(): unknown ServiceBuilder [" + name + "] (not in map)." );
      return false;
    }

    return true;
  }

  public List<Service> getServices()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being built." );
    return this.serviceContainer.getServices();
  }

  public Service getServiceByName( String name )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being built." );
    return this.serviceContainer.getServiceByName( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.serviceContainer.getServiceBuilders();
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.serviceContainer.getServiceBuilderByName( name );
  }

  /**
   * Check whether the state of this ServiceBuilder is such that build() will succeed.
   *
   * @param issues a list into which any issues that come up during isBuildable() will be add.
   * @return true if this ServiceBuilder is in a state where build() will succeed.
   */
  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // Check subordinates.
    this.propertyContainer.isBuildable( localIssues );
    this.serviceContainer.isBuildable( localIssues );

    // Check if this is leaf service that it has a baseUri.
    if ( this.serviceContainer.isEmpty() && this.baseUri == null )
      localIssues.add( new BuilderFinishIssue( "Non-compound services must have base URI.", this ));

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  /**
   * Generate the Service being built by this ServiceBuilder.
   *
   * @return the Service
   * @throws BuilderException if this ServiceBuilder is not in a valid state.
   */
  public Service build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( ! isBuildable( issues ))
      throw new BuilderException( issues );

    // Check subordinates.
    this.propertyContainer.build();
    this.serviceContainer.build();

    this.isBuilt = true;
    return this;
  }
}
