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
package thredds.catalog2.simpleImpl;

import thredds.catalog.ServiceType;
import thredds.catalog2.Property;
import thredds.catalog2.Service;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderIssues;

import java.net.URI;
import java.util.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class ServiceImpl implements Service, ServiceBuilder
{
  private String name;
  private String description;
  private ServiceType type;
  private URI baseUri;
  private String suffix;

  private PropertyContainer propertyContainer;

  private ServiceContainer serviceContainer;

  private final GlobalServiceContainer globalServiceContainer;
  private final boolean isRootContainer;

  private boolean isBuilt = false;

  ServiceImpl( String name, ServiceType type, URI baseUri, GlobalServiceContainer globalServiceContainer )
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

    if ( globalServiceContainer == null )
    {
      this.isRootContainer = true;
      this.globalServiceContainer = new GlobalServiceContainer();
    }
    else
    {
      this.isRootContainer = false;
      this.globalServiceContainer = globalServiceContainer;
    }

    this.serviceContainer = new ServiceContainer( this.globalServiceContainer );
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

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri ) {
    return this.serviceContainer.addService( name, type, baseUri );
  }

  public boolean removeService( ServiceBuilder serviceBuilder )
  {
    if ( serviceBuilder == null )
      return false;

    return this.serviceContainer.removeService( (ServiceImpl) serviceBuilder );
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

  public Service findServiceByNameGlobally( String name )
  {
    if ( ! this.isBuilt)
      throw new IllegalStateException( "This Service has escaped its Builder before being built.");
    return this.globalServiceContainer.getServiceByGloballyUniqueName( name );
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

  public ServiceBuilder findServiceBuilderByNameGlobally( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been built." );
    return this.globalServiceContainer.getServiceByGloballyUniqueName( name );
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  /**
   * Check whether the state of this ServiceBuilder is such that build() will succeed.
   *
   * @return true if this ServiceBuilder is in a state where build() will succeed.
   */
  public BuilderIssues getIssues()
  {
    BuilderIssues issues = this.serviceContainer.getIssues();

    // Check subordinates.
    if ( this.isRootContainer)
      issues.addAllIssues( this.globalServiceContainer.getIssues( this ));
    issues.addAllIssues( this.propertyContainer.getIssues());

    // Check if this is leaf service that it has a baseUri.
    if ( this.serviceContainer.isEmpty() && this.baseUri == null )
      issues.addIssue( BuilderIssue.Severity.WARNING, "Non-compound services must have base URI.", this, null );

    return issues;
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

    // Check subordinates.
    this.propertyContainer.build();
    this.serviceContainer.build();

    this.isBuilt = true;
    return this;
  }
}
