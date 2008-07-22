package thredds.catalog2.builder;

import thredds.catalog.ServiceType;
import thredds.catalog2.explorer.ServiceExplorer;

import java.net.URI;

/**
 * Provide an interface for constructing Service objects.
 *
 * @author edavis
 * @since 4.0
 */
public interface ServiceBuilder extends ServiceExplorer
{
  public void setName( String name );
  public void setDescription( String description );
  public void setType( ServiceType type );
  public void setBaseUri( URI baseUri );
  public void setSuffix( String suffix );

  /**
   * Add a Property object with the given name and value to this Service
   * or replace an existing Property of the same name.
   *
   * @param name the name of the Property to be added.
   * @param value the value of the property to be added.
   * @throws IllegalArgumentException if the name or value are null.
   */
  public void addProperty( String name, String value );

  /**
   * Add a new Service object with the given name, type, and base uri to this
   * Service returning a ServiceBuilder object to allow full construction and
   * modification of the new Service.
   *
   * @param name the name of the new Service object.
   * @param type the type of the new Service object.
   * @param baseUri the base URI of the new Service object.
   * @return a ServiceBuilder for further construction and modification of the new Service.
   *
   * @throws IllegalArgumentException if the name, type, or base URI are null.
   */
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri );

  /**
   * Add a new Service object with the given name, type, and base uri to this
   * Service at the index indicated and return a ServiceBuilder object. The
   * ServiceBuilder object allows further construction and modification of
   * the new Service.
   *
   * @param name the name of the new Service object.
   * @param type the type of the new Service object.
   * @param baseUri the base URI of the new Service object.
   * @return a ServiceBuilder for further construction and modification of the new Service.
   *
   * @throws IllegalArgumentException if the name, type, or base URI are null.
   * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > getServices().size()).
   */
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri, int index );

  public void finish();
}
