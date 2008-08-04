package thredds.catalog2.simpleImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog2.builder.ServiceBuilder;

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
  private List<Property> properties;
  private Map<String,Property> propertiesMap;
  private List<ServiceBuilder> serviceBuilders;
  private List<Service> services;
  private Map<String,Service> servicesMap;

  private boolean finished = false;

  protected ServiceImpl( String name, ServiceType type, URI baseUri )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( type == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");
    this.name = name;
    this.description = "";
    this.type = type;
    this.baseUri = baseUri;
    this.suffix = "";
    this.properties = new ArrayList<Property>();
    this.propertiesMap = new HashMap<String,Property>();
    this.serviceBuilders = new ArrayList<ServiceBuilder>();
    this.services = new ArrayList<Service>();
    this.servicesMap = new HashMap<String,Service>();
  }

  @Override
  public void setName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished().");
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null." );
    this.name = name;
  }

  @Override
  public void setDescription( String description )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    this.description = description != null ? description : "";
  }

  @Override
  public void setType( ServiceType type )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( type == null )
      throw new IllegalArgumentException( "Service type must not be null." );
    this.type = type;
  }

  @Override
  public void setBaseUri( URI baseUri )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( baseUri == null )
      throw new IllegalArgumentException( "Base URI must not be null." );
    this.baseUri = baseUri;
  }

  @Override
  public void setSuffix( String suffix )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    this.suffix = suffix != null ? suffix : "";
  }

  @Override
  public void addProperty( String name, String value )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    PropertyImpl property = new PropertyImpl( name, value );
    Property curProp = this.propertiesMap.get( name );
    if ( curProp != null )
    {
      int index = this.properties.indexOf( curProp );
      this.properties.remove( index );
      this.propertiesMap.remove( name );
      this.properties.add( index, property );
    }
    else
    {
      this.properties.add( property );
    }

    this.propertiesMap.put( name, property );
    return;
  }

  @Override
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    if ( this.getName().equals( name) || this.servicesMap.containsKey( name ))
      throw new IllegalStateException( "Duplicate service name [" + name + "].");
    ServiceImpl sb = new ServiceImpl( name, type, baseUri );
    this.serviceBuilders.add( sb );
    this.services.add( sb );
    this.servicesMap.put( name, sb );
    return sb;
  }

//  @Override
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

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public String getDescription()
  {
    return this.description;
  }

  @Override
  public ServiceType getType()
  {
    return this.type;
  }

  @Override
  public URI getBaseUri()
  {
    return this.baseUri;
  }

  @Override
  public String getSuffix()
  {
    return this.suffix;
  }

  @Override
  public List<String> getPropertyNames()
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return Collections.unmodifiableList( new ArrayList<String>( this.propertiesMap.keySet()));
  }

  @Override
  public String getPropertyValue( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return this.propertiesMap.get( name ).getValue();
  }

  @Override
  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return Collections.unmodifiableList( this.properties);
  }

  @Override
  public Property getProperty( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return this.propertiesMap.get( name );
  }

  @Override
  public List<Service> getServices()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return Collections.unmodifiableList( this.services );
  }

  @Override
  public Service getService( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );
    return this.servicesMap.get( name );
  }

  @Override
  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    return Collections.unmodifiableList( this.serviceBuilders );
  }

  @Override
  public ServiceBuilder getServiceBuilder( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This ServiceBuilder has been finished()." );
    for ( ServiceBuilder b : this.serviceBuilders )
    {
      if ( b.getName().equals( name ))
        return b;
    }
    return null;
  }

  @Override
  public boolean isFinished()
  {
    return this.finished;
  }

  @Override
  public Service finish()
  {
    if ( this.finished )
      return this;

    // Check not-null invariants.
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null." );
    if ( type == null ) throw new IllegalArgumentException( "Service type must not be null." );
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null." );

    // Check for duplicate service names.
    String duplicateName = anyDuplicateServiceNames();
    if ( duplicateName != null )
      throw new IllegalStateException( "Duplicate service name [" + duplicateName + "]." );

    this.finished = true;
    return this;
  }

  private String anyDuplicateServiceNames()
  {
    // ToDo Implement
    return null;
  }
}
