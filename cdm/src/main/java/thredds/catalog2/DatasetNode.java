package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetNode
{
  public String getId();
  public String getName();
  public List<Property> getProperties();
  public Property getPropertyByName( String name);
  public List<Metadata> getMetadata();

  public Catalog getParentCatalog();
  public DatasetNode getParent();

  public boolean isCollection();
  public List<DatasetNode> getDatasets();
  public DatasetNode getDatasetById( String id);
  public DatasetNode getDatasetByName( String name);
}