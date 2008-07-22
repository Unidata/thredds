package thredds.catalog2.simpleImpl;

import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.CatalogRefBuilder;
import thredds.catalog2.builder.MetadataBuilder;
import thredds.catalog2.builder.DatasetBuilder;
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
public class DatasetNodeImpl implements DatasetNodeBuilder
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

  public CatalogRefBuilder addCatalogRef( int index )
  {
    return null;
  }

  public void setId( String id )
  {
  }

  public void setName( String name )
  {
  }

  public void addProperty( String name, String value )
  {
  }

  public MetadataBuilder addMetadata()
  {
    return null;
  }

  public DatasetBuilder addDataset()
  {
    return null;
  }

  public DatasetBuilder addDataset( int index )
  {
    return null;
  }

  public CatalogRefBuilder addCatalogRef()
  {
    return null;
  }

  public String getId()
  {
    return null;
  }

  public String getName()
  {
    return null;
  }

  public List<Property> getProperties()
  {
    return null;
  }

  public List<Metadata> getMetadata()
  {
    return null;
  }

  public Catalog getParentCatalog()
  {
    return null;
  }

  public <T extends DatasetNode> T getParent()
  {
    return null;
  }

  public boolean isCollection()
  {
    return false;
  }

  public List<? extends DatasetNode> getDatasets()
  {
    return null;
  }

  public <T extends DatasetNode> T getDatasetByName( String name )
  {
    return null;
  }

  public <T extends DatasetNode> T getDatasetById( String id )
  {
    return null;
  }

  public Property getPropertyByName( String name )
  {
    return null;
  }
}
