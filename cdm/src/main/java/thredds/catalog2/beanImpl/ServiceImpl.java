package thredds.catalog2.beanImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Service;
import thredds.catalog2.Property;
import thredds.catalog2.Catalog;

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
  private URI fullyResolvedBaseUri;
  private String suffix;
  private List<Property> properties;

  public ServiceImpl( Catalog parentCatalog, String name, String description, ServiceType serviceType, URI baseUri, String suffix, List<Property> properties )
  {
    if ( parentCatalog == null ) throw new IllegalArgumentException( "Parent catalog must not be null.");
    if ( name == null ) throw new IllegalArgumentException( "Name must not be null.");
    if ( serviceType == null ) throw new IllegalArgumentException( "Service type must not be null.");
    if ( baseUri == null ) throw new IllegalArgumentException( "Base URI must not be null.");
    this.name = name;
    this.description = description == null ? "" : description;
    this.serviceType = serviceType;
    this.baseUri = baseUri;
    this.fullyResolvedBaseUri = parentCatalog.getDocumentBaseUri().resolve( this.baseUri );
    this.suffix = suffix == null ? "" : suffix;
    if (properties == null )
      this.properties = Collections.emptyList();
    else
      this.properties = properties;
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
  public URI getFullyResolvedBaseUri()
  {
    return this.fullyResolvedBaseUri;
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
