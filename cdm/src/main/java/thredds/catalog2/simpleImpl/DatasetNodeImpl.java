package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.*;
import thredds.catalog2.Property;
import thredds.catalog2.Metadata;
import thredds.catalog2.Catalog;
import thredds.catalog2.DatasetNode;

import java.util.List;
import java.util.Map;

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
  private List<Property> properties;
  private Map<String,Property> propertiesMap;
  private List<Metadata> metadata;

  private Catalog parentCatalog;
  private DatasetNode parent;
  private List<DatasetNode> children;
  private Map<String,DatasetNode> childrenNameMap;
  private Map<String,DatasetNode> childrenIdMap;

  private boolean finished = false;

  protected DatasetNodeImpl( String name )
  {
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null.");
    this.name = name;
  }

  @Override
  public void setId( String id )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished().");
    this.id = id;
  }

  @Override
  public void setName( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    if ( name == null ) throw new IllegalArgumentException( "DatasetNode name must not be null." );
    this.name = name;
  }

  @Override
  public void addProperty( String name, String value )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    PropertyImpl property = new PropertyImpl( name, value );
    Property curProp = this.propertiesMap.get( name );
    if ( curProp != null )
    {
      int index = this.properties.indexOf( curProp );
      this.properties.remove( index );
      this.propertiesMap.remove( name );
      this.properties.add( index, property );
    }
    else
    {
      this.properties.add( property );
    }

    this.propertiesMap.put( name, property );
    return;
  }

  @Override
  public MetadataBuilder addMetadata()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    //MetadataBuilder mb = new MetadataImpl();
    return null;
  }

  @Override
  public DatasetBuilder addDataset()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetAliasBuilder addDatasetAlias()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public CatalogRefBuilder addCatalogRef()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public String getId()
  {
    return null;
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public List<Property> getProperties()
  {
    if ( ! this.finished ) throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public List<Metadata> getMetadata()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public Catalog getParentCatalog()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public DatasetNode getParent()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public boolean isCollection()
  {
    return false;
  }

  @Override
  public List<DatasetNode> getDatasets()
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public DatasetNode getDatasetByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public DatasetNode getDatasetById( String id )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public Property getPropertyByName( String name )
  {
    if ( !this.finished )
      throw new IllegalStateException( "This DatasetNode has escaped its DatasetNodeBuilder before being finished()." );
    return null;
  }

  @Override
  public List<String> getPropertyNames()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public String getPropertyValue( String name )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public CatalogBuilder getParentCatalogBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetBuilder getParentDatasetBuilder()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public List<DatasetNodeBuilder> getDatasetNodeBuilders()
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id )
  {
    if ( this.finished ) throw new IllegalStateException( "This DatasetNodeBuilder has been finished()." );
    return null;
  }

  @Override
  public DatasetNodeBuilder getDatasetNodeBuilderByName( String name )
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
    if ( this.finished ) return this;

    // Check invariants.

    // Mark as finished.
    this.finished = true;
    return this;
  }
}
