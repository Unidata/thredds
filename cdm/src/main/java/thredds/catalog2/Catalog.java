package thredds.catalog2;

import thredds.catalog.ServiceType;

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
  public URI getDocBaseUri();
  public String getVersion();
  public Date getExpires();
  public Date getLastModified();

  public List<Service> getServices();
  public Service getServiceByName( String name );
  public Service getServiceByType( ServiceType type );

  public List<DatasetNode> getDatasets();
  public DatasetNode getDatasetById( String id );

  public List<Property> getProperties();
  public Property getPropertyByName( String name );
}
