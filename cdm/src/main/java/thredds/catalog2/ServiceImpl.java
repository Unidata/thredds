package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceImpl
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ServiceImpl.class );

  private String name;
  private String description;
  private ServiceType serviceType;
  private URI baseUri;
  private String suffix;
  private List<Property> properties;

  public ServiceImpl( String name, String description, ServiceType serviceType, URI baseUri, String suffix, List<Property> properties )
  {
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( serviceType == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");
    this.name = name;
    this.description = description == null ? "" : description;
    this.serviceType = serviceType;
    this.baseUri = baseUri;
    this.suffix = suffix == null ? "" : suffix;
    if (properties == null )
      this.properties = Collections.emptyList();
    else
      this.properties = properties;
  }

  public String getName()
  {
    return name;
  }

  public String getDescription()
  {
    return description;
  }

  public ServiceType getServiceType()
  {
    return serviceType;
  }

  public URI getBaseUri()
  {
    return baseUri;
  }

  public String getSuffix()
  {
    return suffix;
  }

  public List<Property> getProperties()
  {
    return properties;
  }
}
