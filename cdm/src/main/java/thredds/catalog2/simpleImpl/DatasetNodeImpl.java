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

  private CatalogBuilder parentCatalogBuilder;
  private Catalog parentCatalog;
  protected DatasetNodeBuilder parentBuilder;
  protected DatasetNode parent;

  private List<DatasetNodeBuilder> childrenBuilders;
  private List<DatasetNode> children;
  private Map<String,DatasetNode> childrenIdMap;

  private boolean isBuilt = false;

  protected DatasetNodeImpl( String name, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
    this.parentCatalogBuilder = parentCatalog;
    this.parentCatalog = (Catalog) parentCatalog;
    this.parentBuilder = parent;
    this.parent = (DatasetNode) parent;

    this.propertyContainer = new PropertyContainer();

    this.metadataBuilders = new ArrayList<MetadataBuilder>();
    this.metadata = new ArrayList<Metadata>();

    this.childrenBuilders = new ArrayList<DatasetNodeBuilder>();
    this.children = new ArrayList<DatasetNode>();
    this.childrenIdMap = new HashMap<String, DatasetNode>();
  }

  public void setId( String id )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished().");
    if ( this.id != null )
    {
      if ( ! this.id.equals(  id ))
      {
        // If new id is null, set it and done
        if ( id == null )
        {
          this.id = null;
          return;
        }
        // If new id not null, Check if the new id is already in use globally. If so, throw IllegalStateException.

        // remove old id from global unique id Map

        // add new id to global unique id Map
      }
      else { /* same id, do nothing */ }
    }
    else
    {
      // if new id is null, so is old, no change
      if ( id == null )
        return;
      // if new id not null, set and add to global unique id Map
      this.id = id;
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

  public boolean isCollection()
  {
    return !this.childrenBuilders.isEmpty();
  }

  public boolean isDatasetIdInUseGlobally( String id )
  {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DatasetBuilder addDataset( String name)
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetImpl db = new DatasetImpl( name, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( db );
    this.children.add( db );
    return db;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference)
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( crb );
    this.children.add( crb );
    return crb;
  }

  public boolean removeDatasetNode( DatasetNodeBuilder datasetBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This DatasetNodeBuilder has been built." );
    return false;
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.children);
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !this.isBuilt )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.childrenIdMap.get( id);
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.parentCatalogBuilder;
  }

  public DatasetNodeBuilder getParentDatasetBuilder()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.parentBuilder;
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.isBuilt ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id )
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderFinishIssue> localIssues = new ArrayList<BuilderFinishIssue>();

    // Check subordinates.
    for ( MetadataBuilder mb : this.metadataBuilders )
      mb.isBuildable( localIssues);
    for ( DatasetNodeBuilder dnb : this.childrenBuilders )
      dnb.isBuildable( localIssues );

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
    for ( DatasetNodeBuilder dnb : this.childrenBuilders )
      dnb.build();

    this.isBuilt = true;
    return this;

  }
}
