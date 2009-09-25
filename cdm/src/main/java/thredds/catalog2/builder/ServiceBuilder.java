/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.catalog2.builder;

import thredds.catalog.ServiceType;
import thredds.catalog2.Service;

import java.net.URI;
import java.util.List;

/**
 * Provide an interface for constructing Service objects.
 *
 * @author edavis
 * @since 4.0
 */
public interface ServiceBuilder extends ThreddsBuilder
{
  public String getName();

  public String getDescription();
  public void setDescription( String description );

  public ServiceType getType();
  public void setType( ServiceType type );

  public URI getBaseUri();
  public void setBaseUri( URI baseUri );

  public String getSuffix();
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
  public boolean removeProperty( String name );
  public List<String> getPropertyNames();
  public String getPropertyValue( String name );

  /**
   * Add a new Service object with the given name, type, and base uri to this
   * Service returning a ServiceBuilder object to allow full construction and
   * modification of the new Service.
   *
   * <p>If this Service or an already added Service has the given name, an
   * IllegalStateException is thrown. {@link ServiceBuilder#getServiceBuilderByName(String)}
   * can be used to check before calling this method.
   *
   * @param name the name of the new Service object.
   * @param type the type of the new Service object.
   * @param baseUri the base URI of the new Service object.
   * @return a ServiceBuilder for further construction and modification of the new Service.
   *
   * @throws IllegalArgumentException if the name, type, or base URI are null.
   * @throws IllegalStateException this ServiceBuilder has already been finished or the top container of this ServiceBuilder already contains a ServiceBuilder with the given name.
   */
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri );
  public boolean removeService( ServiceBuilder serviceBuilder );

//  /**
//   * Add a new Service object with the given name, type, and base uri to this
//   * Service at the index indicated and return a ServiceBuilder object. The
//   * Service at the given and higher index (if any) are shifted right (their
//   * index is increased). The ServiceBuilder object allows further
//   * construction and modification of the new Service.
//   *
//   * <p>If this Service or an already added Service has the given name, an
//   * IllegalStateException is thrown. {@link ServiceBuilder#getServiceBuilder(String)}
//   * can be used to check before calling this method.
//   *
//   * @param name the name of the new Service object.
//   * @param type the type of the new Service object.
//   * @param baseUri the base URI of the new Service object.
//   * @param index the index at which to add the new Service object.
//   * @return a ServiceBuilder for further construction and modification of the new Service.
//   *
//   * @throws IllegalArgumentException if the name, type, or base URI are null.
//   * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > getServices().size()).
//   * @throws IllegalStateException this Service or an already added Service has the given name.
//   */
//  public ServiceBuilder addService( String name, ServiceType type, URI baseUri, int index );
  public List<ServiceBuilder> getServiceBuilders();
  public ServiceBuilder getServiceBuilderByName( String name );
  public ServiceBuilder findServiceBuilderByNameGlobally( String name );

  /**
   * Return the finished Service.
   *
   * @return the finished Service.
   */
  public Service build() throws BuilderException;
}
