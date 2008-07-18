package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Dataset extends MetadataContainer
{
  public String getName();
  public String getId();
  public String getAlias();
  public List<Property> getProperties();

  public boolean isAccessible();
  public List<Access> getAccesses();

  public boolean isCollection();
  public List<Dataset> getDatasets();

  public Catalog getParentCatalog();
  public Dataset getParent();
}
