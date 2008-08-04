package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.List;

/**
 * Represents a data access service and allows basic data access information
 * to be factored out of dataset Access objects.
 *
 * @author edavis
 * @since 4.0
 */
public interface Service
{
  /**
   * Returns the name of this Service, may not be null.
   *
   * @return the name of this Service, never null.
   */
  public String getName();

  /**
   * Returns a human-readable description of this Service.
   *
   * @return a human-readable description of this service.
   */
  public String getDescription();

  /**
   * Return the ServiceType for this Service, may not be null.
   *
   * @return the ServiceType for this Service, never null.
   */
  public ServiceType getType();

  /**
   * Return the base URI for this Service, may not be null.
   *
   * @return the base URI for this Service, never null.
   */
  public URI getBaseUri();

  /**
   * Return the suffix for this Service. The suffix will not be null
   * but may be an empty string.
   *
   * @return the suffix for this Service.
   */
  public String getSuffix();

  /**
   * Return the List of Property objects associated with this Service.
   *
   * @return the List of Property objects associated with this Service, may be an empty list but not null.
   */
  public List<Property> getProperties();

  public Property getProperty( String name );

  /**
   * Return the List of Service Objects nested in this service. Nested
   * services are only allowed when this service has a "Compound" ServiceType.
   *
   * @return the List of Service Objects nested in this service, may be an empty list but not null.
   */
  public List<Service> getServices();

  public Service getService( String name );
}
