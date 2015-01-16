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

import thredds.catalog2.builder.*;
import thredds.catalog2.*;

import java.util.*;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class DatasetNodeImpl implements DatasetNode, DatasetNodeBuilder
{
  private String id;
  private String idAuthority;
  private String name;
  private PropertyContainer propertyContainer;

  private ThreddsMetadataImpl threddsMetadataImpl;
  private List<MetadataImpl> metadataImplList;

  private CatalogImpl parentCatalog;
  protected DatasetNodeImpl parent;
  private DatasetNodeContainer parentDatasetContainer;

  private DatasetNodeContainer datasetContainer;

  private boolean isBuilt = false;

  DatasetNodeImpl( String name, CatalogImpl parentCatalog, DatasetNodeImpl parent )
  {
    if ( name == null )
      throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
    this.parentCatalog = parentCatalog;
    this.parent = parent;

    this.propertyContainer = new PropertyContainer();

    if ( this.parent != null )
      this.parentDatasetContainer = this.parent.getDatasetNodeContainer();
    else if ( this.parentCatalog != null )
      this.parentDatasetContainer = this.parentCatalog.getDatasetNodeContainer();
    else
      this.parentDatasetContainer = null;

    DatasetNodeContainer rootContainer = this.parentDatasetContainer != null ? this.parentDatasetContainer.getRootContainer() : null;
    this.datasetContainer = new DatasetNodeContainer( rootContainer );
  }

  DatasetNodeContainer getDatasetNodeContainer()
  {
    return datasetContainer;
  }

  public void setId( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished().");

    // If this dataset has prior ID ...
    if ( this.id != null )
    {
      // If the new ID and old ID are different ...
      if ( ! this.id.equals(  id ))
      {
        // If new id already in use, throw IllegalStateException.
        if ( this.isDatasetIdInUseGlobally( id ) )
          throw new IllegalStateException();

        // Remove the global and local mapping for old ID.
        if ( this.parentDatasetContainer != null )
        {
          this.parentDatasetContainer.removeDatasetNodeByGloballyUniqueId( this.id );
          this.parentDatasetContainer.removeDatasetNodeFromLocalById( this.id );
        }

        // If new id is null: set this ID to null and done.
        if ( id == null )
        {
          this.id = null;
          return;
        }

        // If new id not null, set this ID to new ID and add to global and local ID Map.
        this.id = id;
        if ( this.parentDatasetContainer != null )
        {
          this.parentDatasetContainer.addDatasetNodeByGloballyUniqueId( this );
          this.parentDatasetContainer.addDatasetNodeToLocalById( this );
        }
      }
      else { /* New ID same as old ID: no change, do nothing */ }
    }
    // Else if this dataset has NO prior ID ...
    else
    {
      // if new id is null, so is old, no change
      if ( id == null )
        return;

      // If new id already in use, throw IllegalStateException.
      if ( this.isDatasetIdInUseGlobally( id ) )
        throw new IllegalStateException();

      // If new id not null, set this ID to new ID and add to global and local ID Map.
      this.id = id;
      if ( this.parentDatasetContainer != null )
      {
        this.parentDatasetContainer.addDatasetNodeByGloballyUniqueId( this );
        this.parentDatasetContainer.addDatasetNodeToLocalById( this );
      }
    }
  }

  public String getId()
  {
    return this.id;
  }

  public void setIdAuthority( String idAuthority )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    this.idAuthority = idAuthority;
  }

  public String getIdAuthority()
  {
    return this.idAuthority;
  }

  public void setName( String name )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void addProperty( String name, String value )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    this.propertyContainer.addProperty( name, value );
  }

  public boolean removeProperty( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.propertyContainer.removeProperty( name );
  }

  public List<String> getPropertyNames()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped from its DatasetNodeBuilder before build() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped from its ServiceBuilder before build() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public ThreddsMetadataBuilder setNewThreddsMetadataBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    this.threddsMetadataImpl = new ThreddsMetadataImpl();
    return this.threddsMetadataImpl;
  }

  public boolean removeThreddsMetadataBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    this.threddsMetadataImpl = null;
    return true;
  }

  public ThreddsMetadataBuilder getThreddsMetadataBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.threddsMetadataImpl;
  }

  public ThreddsMetadata getThreddsMetadata()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    return this.threddsMetadataImpl;
  }

  public MetadataBuilder addMetadata()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    MetadataImpl mbi = new MetadataImpl();
    if ( this.metadataImplList == null )
      this.metadataImplList = new ArrayList<MetadataImpl>();
    this.metadataImplList.add( mbi );
    return mbi;
  }

  public boolean removeMetadata( MetadataBuilder metadataBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    if ( metadataBuilder == null )
      return false;
    if ( this.metadataImplList == null )
      return false;
    return this.metadataImplList.remove( (MetadataImpl) metadataBuilder );
  }

  public List<Metadata> getMetadata()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    if ( this.metadataImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Metadata>( this.metadataImplList ) );
  }

  public List<MetadataBuilder> getMetadataBuilders()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    if ( this.metadataImplList == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<MetadataBuilder>( this.metadataImplList) );
  }

  public Catalog getParentCatalog()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    return this.parentCatalog;
  }

  public DatasetNode getParent()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    return this.parent;
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.parentCatalog;
  }

  public DatasetNodeBuilder getParentDatasetBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.parent;
  }

  public boolean isCollection()
  {
    return ! this.datasetContainer.isEmpty();
  }

  public boolean isDatasetIdInUseGlobally( String id )
  {
    return this.datasetContainer.isDatasetNodeIdInUseGlobally( id );
  }

  public DatasetBuilder addDataset( String name)
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    DatasetImpl ds = new DatasetImpl( name, this.parentCatalog, this );
    this.datasetContainer.addDatasetNode( ds );
    return ds;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference)
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    CatalogRefImpl catRef = new CatalogRefImpl( name, reference, this.parentCatalog, this );
    this.datasetContainer.addDatasetNode( catRef );
    return catRef;
  }

  public boolean removeDatasetNode( DatasetNodeBuilder datasetBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.datasetContainer.removeDatasetNode( (DatasetNodeImpl) datasetBuilder );
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    return this.datasetContainer.getDatasets();
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being built." );
    return this.datasetContainer.getDatasetById( id);
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilders();
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return this.datasetContainer.getDatasetNodeBuilderById( id );
  }

  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id )
  {
    return this.datasetContainer.getDatasetNodeByGloballyUniqueId( id );
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public BuilderIssues getIssues()
  {
    BuilderIssues issues = this.datasetContainer.getIssues();

    // Check subordinates.
    if ( this.metadataImplList != null )
      for ( MetadataBuilder mb : this.metadataImplList )
        issues.addAllIssues( mb.getIssues());

    // ToDo Check invariants.

    return issues;
  }

  public DatasetNode build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    // Build subordinates.
    if ( this.metadataImplList != null )
      for ( MetadataBuilder mb : this.metadataImplList )
        mb.build();
    this.datasetContainer.build();

    this.isBuilt = true;
    return this;
  }
}
