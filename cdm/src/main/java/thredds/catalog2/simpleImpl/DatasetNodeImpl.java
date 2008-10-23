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
public class DatasetNodeImpl implements DatasetNode, DatasetNodeBuilder
{
  private String id;
  private String idAuthority;
  private String name;
  private PropertyContainer propertyContainer;

  private ThreddsMetadata threddsMetadata;
  private List<MetadataBuilder> metadataBuilders;
  private List<Metadata> metadata;

  private CatalogImpl parentCatalog;
  protected DatasetNodeImpl parent;
  private DatasetNodeContainer parentDatasetContainer;

  private DatasetNodeContainer datasetContainer;

  private boolean isBuilt = false;

  protected DatasetNodeImpl( String name, CatalogImpl parentCatalog, DatasetNodeImpl parent )
  {
    if ( name == null )
      throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
    this.parentCatalog = parentCatalog;
    this.parent = parent;

    this.propertyContainer = new PropertyContainer();

    this.metadataBuilders = new ArrayList<MetadataBuilder>();
    this.metadata = new ArrayList<Metadata>();

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

  public void setThreddsMetadata( ThreddsMetadata threddsMetadata )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    this.threddsMetadata = new ThreddsMetadataImpl();
    return;
  }

  public ThreddsMetadata getThreddsMetadata()
  {
    return this.threddsMetadata;
  }

  public MetadataBuilder addMetadata()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    //MetadataBuilder mb = new MetadataImpl();
    return null;
  }

  public boolean removeMetadata( MetadataBuilder metadataBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return false;
  }

  public List<Metadata> getMetadata()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.metadata );
  }

  public List<MetadataBuilder> getMetadataBuilders()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.metadataBuilders );
  }

  public Catalog getParentCatalog()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parentCatalog;
  }

  public DatasetNode getParent()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parent;
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.parentCatalog;
  }

  public DatasetNodeBuilder getParentDatasetBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
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
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetImpl ds = new DatasetImpl( name, this.parentCatalog, this );
    this.datasetContainer.addDatasetNode( ds );
    return ds;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference)
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
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
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.datasetContainer.getDatasets();
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.datasetContainer.getDatasetById( id);
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.datasetContainer.getDatasetNodeBuilders();
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
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

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // Check subordinates.
    for ( MetadataBuilder mb : this.metadataBuilders )
      mb.isBuildable( localIssues);
    this.datasetContainer.isBuildable( localIssues );

    // ToDo Check invariants.

    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public DatasetNode build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderFinishIssue> issues = new ArrayList<BuilderFinishIssue>();
    if ( !isBuildable( issues ) )
      throw new BuilderException( issues );

    // Check subordinates.
    for ( MetadataBuilder mb : this.metadataBuilders )
      mb.build();
    this.datasetContainer.build();

    this.isBuilt = true;
    return this;
  }
}
