package thredds.catalog2;

import thredds.catalog2.explorer.CatalogExplorer;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Represents a hierarchical collection of datasets.
 *
 * <p>Invariants:
 * <ul>
 *   <li> Must have a non-null name.</li>
 *   <li> Must have a non-null document base URI.</li>
 *   <li> Each Service name must be unique in the catalog.</li>
 *   <li> All Service name references must reference an existing Service.</li>
 *   <li> All Dataset ID must be unique in the catalog.</li>
 *   <li> All Dataset alias must reference an existing Dataset.</li> 
 * </ul>
 *
 * @author edavis
 * @since 4.0
 */
public interface Catalog
{
  public String getName();
  public URI getBaseUri();
  public String getVersion();
  public Date getExpires();
  public Date getLastModified();
  public List<Service> getServices();

  public List<? extends DatasetNode> getDatasets();

  public List<Property> getProperties();
}
