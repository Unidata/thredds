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
  public String getIdAuthority();
  public String getName(); // ToDo Change to getTitle() ???
  public List<Property> getProperties();
  public Property getPropertyByName( String name);
  public ThreddsMetadata getThreddsMetadata();
  public List<Metadata> getMetadata();

  public Catalog getParentCatalog();
  public DatasetNode getParent();

  public boolean isCollection();
  public List<DatasetNode> getDatasets();
  public DatasetNode getDatasetById( String id);
}