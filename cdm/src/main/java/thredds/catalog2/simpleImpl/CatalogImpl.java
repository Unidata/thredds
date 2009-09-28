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

import thredds.catalog2.*;
import thredds.catalog2.builder.*;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.*;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class CatalogImpl implements Catalog, CatalogBuilder
{
  private String name;
  private URI docBaseUri;
  private String version;
  private DateType expires;
  private DateType lastModified;

  private final ServiceContainer serviceContainer;
  private final GlobalServiceContainer globalServiceContainer;

  private final DatasetNodeContainer datasetContainer;

  private final PropertyContainer propertyContainer;

  private boolean isBuilt = false;


  CatalogImpl( String name, URI docBaseUri, String version, DateType expires, DateType lastModified )
  {
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.docBaseUri = docBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.globalServiceContainer = new GlobalServiceContainer();
    this.serviceContainer = new ServiceContainer( globalServiceContainer );

    this.datasetContainer = new DatasetNodeContainer( null );

    this.propertyContainer = new PropertyContainer();
  }

  DatasetNodeContainer getDatasetNodeContainer()
  {
    return this.datasetContainer;
  }

  public void setName( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void setDocBaseUri( URI docBaseUri )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( docBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null." );
    this.docBaseUri = docBaseUri;
  }

  public URI getDocBaseUri()
  {
    return this.docBaseUri;
  }

  public void setVersion( String version )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.version = version;
  }

  public String getVersion()
  {
    return this.version;
  }

  public void setExpires( DateType expires )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.expires = expires;
  }

  public DateType getExpires()
  {
    return this.expires;
  }

  public void setLastModified( DateType lastModified )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.lastModified = lastModified;
  }

  public DateType getLastModified()
  {
    return this.lastModified;
  }

  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );

    return this.serviceContainer.addService( name, type, baseUri );
  }

  public boolean removeService( ServiceBuilder serviceBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( serviceBuilder == null )
      return false;

    return this.serviceContainer.removeService( (ServiceImpl) serviceBuilder );
  }

  public List<Service> getServices()
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without build() being called." );
    return this.serviceContainer.getServices();
  }

  public Service getServiceByName( String name )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.serviceContainer.getServiceByName( name );
  }

  public Service findServiceByNameGlobally( String name )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.globalServiceContainer.getServiceByGloballyUniqueName( name );
  }

  public List<ServiceBuilder> getServiceBuilders()
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.serviceContainer.getServiceBuilders();
  }

  public ServiceBuilder getServiceBuilderByName( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.serviceContainer.getServiceBuilderByName( name );
  }

  public ServiceBuilder findServiceBuilderByNameGlobally( String name )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.globalServiceContainer.getServiceByGloballyUniqueName( name );
  }

  public void addProperty( String name, String value )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    this.propertyContainer.addProperty( name, value );
  }

  public boolean removeProperty( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );

    return this.propertyContainer.removeProperty( name );
  }

  public List<String> getPropertyNames()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This Catalog has escaped from its CatalogBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public DatasetBuilder addDataset( String name )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    DatasetImpl di = new DatasetImpl( name, this, null );
    this.datasetContainer.addDatasetNode( di );
    return di;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, this, null );
    this.datasetContainer.addDatasetNode( crb );
    return crb;
  }

  public boolean removeDataset( DatasetNodeBuilder builder )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    if ( builder == null )
      throw new IllegalArgumentException( "DatasetNodeBuilder may not be null.");

    return this.datasetContainer.removeDatasetNode( (DatasetNodeImpl) builder );
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasets();
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasetById( id );
  }

  public DatasetNode findDatasetByIdGlobally( String id )
  {
    if ( !isBuilt )
      throw new IllegalStateException( "This Catalog has escaped its CatalogBuilder without being build()-ed." );
    return this.datasetContainer.getDatasetNodeByGloballyUniqueId( id );
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilders();
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( isBuilt ) throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilderById( id);
  }

  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id )
  {
    if ( isBuilt )
      throw new IllegalStateException( "This CatalogBuilder has been built." );
    return this.datasetContainer.getDatasetNodeByGloballyUniqueId( id );
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public BuilderIssues getIssues()
  {
    BuilderIssues issues = new BuilderIssues();

    // ToDo Check any invariants.
    // Check invariants

    // Check subordinates.
    issues.addAllIssues( this.globalServiceContainer.getIssues( this ));
    issues.addAllIssues( this.serviceContainer.getIssues());
    issues.addAllIssues( this.datasetContainer.getIssues());
    issues.addAllIssues( this.propertyContainer.getIssues());

    return issues;
  }

  public Catalog build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    // ToDo Check any invariants.
    // Check invariants

    // Check subordinates.
    this.serviceContainer.build();
    this.datasetContainer.build();
    this.propertyContainer.build();
    
    this.isBuilt = true;
    return this;
  }
}
