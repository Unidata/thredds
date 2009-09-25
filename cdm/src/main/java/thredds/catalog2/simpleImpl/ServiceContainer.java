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

import thredds.catalog2.Service;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog.ServiceType;

import java.util.*;
import java.net.URI;

/**
 * Helper class for those classes that contain services: CatalogImpl and ServiceImpl.
 *
 * @author edavis
 * @since 4.0
 */
class ServiceContainer
{
  private List<ServiceImpl> services;

  private final GlobalServiceContainer globalServiceContainer;

  private boolean isBuilt;

  ServiceContainer( GlobalServiceContainer globalServiceContainer )
  {
    if ( globalServiceContainer == null )
      throw new IllegalArgumentException( "" );

    this.isBuilt = false;
    this.globalServiceContainer = globalServiceContainer;
  }

  ServiceImpl getServiceByGloballyUniqueName( String name ) {
    return this.globalServiceContainer.getServiceByGloballyUniqueName( name );
  }

  boolean isEmpty()
  {
    if ( this.services == null )
      return true;
    return this.services.isEmpty();
  }
  
  int size()
  {
    if ( this.services == null )
      return 0;
    return this.services.size();
  }

  /**
   * Create a new ServiceImpl and add it to this container.
   *
   * @param name the name of the ServiceImpl.
   * @param type the ServiceType of the ServiceImpl.
   * @param baseUri the base URI of the ServiceImpl.
   * @return the ServiceImpl that was created and added to this container.
   * @throws IllegalArgumentException if name, type, or baseUri are null.
   * @throws IllegalStateException if build() has been called on this ServiceContainer.
   */
  ServiceImpl addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built.");

    if ( this.services == null )
      this.services = new ArrayList<ServiceImpl>();

    ServiceImpl service = new ServiceImpl( name, type, baseUri, this.globalServiceContainer );

    boolean addedService = this.services.add( service );
    assert addedService;

    this.globalServiceContainer.addService( service );

    return service;
  }

  /**
   * Remove the given Service from this container if it is present.
   *
   * @param service the Service to remove.
   * @return true if the Service was present and has been removed, otherwise false.
   * @throws IllegalStateException if build() has been called on this ServiceContainer.
   */
  boolean removeService( ServiceImpl service )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceContainer has been built." );

    if ( service == null )
      return false;

    if ( this.services.remove( service))
    {
      boolean success = this.globalServiceContainer.removeService( service );
      assert success;
      return true;
    }

    return false;
  }

  List<Service> getServices()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );

    if ( this.services == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Service>( this.services ) );
  }

  boolean containsServiceName( String name )
  {
    if ( name == null )
      return false;

    if ( this.services == null )
      return false;

    return null != this.getServiceImplByName( name );
  }

  Service getServiceByName( String name )
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This Service has escaped from its ServiceBuilder without being finished()." );

    if ( name == null )
      return null;

    return this.getServiceImplByName( name );
  }

  private ServiceImpl getServiceImplByName( String name )
  {
    if ( this.services != null )
      for ( ServiceImpl s : this.services )
        if ( s.getName().equals( name ))
          return s;
    return null;
  }

  List<ServiceBuilder> getServiceBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );

    if ( this.services == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ServiceBuilder>( this.services ) );
  }

  ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This ServiceBuilder has been finished()." );

    if ( name == null )
      return null;

    return this.getServiceImplByName( name );
  }

  boolean isBuilt()
  {
    return this.isBuilt;
  }
  
  /**
   * Gather any issues with the current state of this ServiceContainer.
   *
   * @return any BuilderIssues with the current state of this ServiceContainer.
   */
  BuilderIssues getIssues()
  {
    BuilderIssues issues = new BuilderIssues();

    // Check on contained ServiceImpl objects.
    if ( this.services != null )
      for ( ServiceImpl sb : this.services )
        issues.addAllIssues( sb.getIssues());

    return issues;
  }

  /**
   * Call build() on all contained services.
   *
   * @throws BuilderException if any of the contained services are not in a valid state.
   */
  void build()
          throws BuilderException
  {
    if ( this.isBuilt )
      return;

    // Build contained ServiceImpl objects.
    if ( this.services != null )
      for ( ServiceImpl sb : this.services )
        sb.build();

    this.isBuilt = true;
    return;
  }
}