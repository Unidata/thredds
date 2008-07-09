package thredds.catalog2.simpleImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Service;
import thredds.catalog2.Property;

import java.net.URI;
import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceImpl implements Service
{
  private String name;
  private String description;
  private ServiceType serviceType;
  private URI baseUri;
  private String suffix;
  private List<Property> properties;

  public ServiceImpl( String name, ServiceType serviceType, URI baseUri )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( serviceType == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");
    this.name = name;
    this.description = "";
    this.serviceType = serviceType;
    this.baseUri = baseUri;
    this.suffix = "";
    this.properties = Collections.emptyList();
  }

  public void setDescription( String description )
  {
    this.description = description != null ? description : "";
  }

  public void setSuffix( String suffix )
  {
    this.suffix = suffix != null ? suffix : "";
  }

  public void setProperties( List<Property> properties )
  {
    if ( properties != null )
      this.properties = properties;
    else
      this.properties = Collections.emptyList();
  }

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
    return this.serviceType;
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
  public List<Property> getProperties()
  {
    return this.properties;
  }
}
