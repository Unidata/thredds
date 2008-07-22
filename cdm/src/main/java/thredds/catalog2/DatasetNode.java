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
  public List<Metadata> getMetadata();

  public Catalog getParentCatalog();
  public <T extends DatasetNode> T getParent();

  public boolean isCollection();
  public List<? extends DatasetNode> getDatasets();
}