package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.Property;
import thredds.catalog2.Metadata;
import thredds.catalog2.Catalog;
import thredds.catalog2.DatasetNode;

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
  private String name;
  private PropertyContainer propertyContainer;

  private List<MetadataBuilder> metadataBuilders;
  private List<Metadata> metadata;

  private Catalog parentCatalog;
  protected DatasetNode parent;

  private List<DatasetNodeBuilder> childrenBuilders;
  private List<DatasetNode> children;
  private Map<String,DatasetNode> childrenIdMap;

  private boolean finished = false;

  protected DatasetNodeImpl( String name, CatalogBuilder parentCatalog, DatasetNodeBuilder parent )
  {
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
    this.parentCatalog = (Catalog) parentCatalog;
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
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished().");
    if ( ! this.id.equals( id ))
    {
      // ToDo Check if another dataset in parent catalog has this id - ((CatalogImpl) this.parentCatalog).getCatalogSearchHelper();
      ( (DatasetNodeImpl) this.parent ).childrenIdMap.remove( this.id );

      this.id = id;
      ((DatasetNodeImpl) this.parent).childrenIdMap.put( id, this );
    }
  }

  public String getId()
  {
    return this.id;
  }

  public void setName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null." );
    this.name = name;
  }

  public String getName()
  {
    return this.name;
  }

  public void addProperty( String name, String value )
  {
    if ( this.finished )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finish()-ed." );
    this.propertyContainer.addProperty( name, value );
  }

  public List<String> getPropertyNames()
  {
    if ( this.finished )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.propertyContainer.getPropertyNames();
  }

  public String getPropertyValue( String name )
  {
    if ( this.finished )
      throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return this.propertyContainer.getPropertyValue( name );
  }

  public List<Property> getProperties()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped from its DatasetNodeBuilder before finish() was called." );
    return this.propertyContainer.getProperties();
  }

  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped from its ServiceBuilder before finish() was called." );
    return this.propertyContainer.getPropertyByName( name );
  }

  public MetadataBuilder addMetadata()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    //MetadataBuilder mb = new MetadataImpl();
    return null;
  }

  public List<Metadata> getMetadata()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.metadata );
  }

  public List<MetadataBuilder> getMetadataBuilders()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.metadataBuilders );
  }

  public DatasetBuilder addDataset( String name)
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetImpl db = new DatasetImpl( name, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( db );
    this.children.add( db );
    return db;
  }

  public DatasetAliasBuilder addDatasetAlias( String name, DatasetNodeBuilder alias )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    DatasetAliasImpl dab = new DatasetAliasImpl( name, alias, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( dab );
    this.children.add( dab );
    return dab;
  }

  public CatalogRefBuilder addCatalogRef( String name, URI reference)
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    CatalogRefImpl crb = new CatalogRefImpl( name, reference, (CatalogBuilder) this.getParentCatalog(), this );
    this.childrenBuilders.add( crb );
    this.children.add( crb );
    return crb;
  }

  public Catalog getParentCatalog()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parentCatalog;
  }

  public DatasetNode getParent()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.parent;
  }

  public boolean isCollection()
  {
    return ! this.childrenBuilders.isEmpty();
  }

  public List<DatasetNode> getDatasets()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return Collections.unmodifiableList( this.children);
  }

  public DatasetNode getDatasetById( String id )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return this.childrenIdMap.get( id);
  }

  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public DatasetBuilder getParentDatasetBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  public boolean isFinished()
  {
    return this.finished;
  }

  public DatasetNode finish()
  {
    if ( this.finished )
      return this;

    // Check invariants.

    // Finish subordinates.
    for ( MetadataBuilder mb : this.metadataBuilders )
      mb.finish();
    for ( DatasetNodeBuilder dnb : this.childrenBuilders)
      dnb.finish();

    // Mark as finished.
    this.finished = true;
    return this;
  }
}
