package thredds.catalog2.builder;

import thredds.catalog2.Catalog;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogBuilder extends ThreddsBuilder
{
  public String getName();
  public void setName( String name);

  public URI getDocBaseUri();
  public void setDocBaseUri( URI docBaseUri);

  public String getVersion();
  public void setVersion( String version );

  public Date getExpires();
  public void setExpires( Date expires );

  public Date getLastModified();
  public void setLastModified( Date lastModified );

  // * @throws IllegalStateException this CatalogBuilder has already been finished or already contains a ServiceBuilder with the given name.
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri );
  public ServiceBuilder removeService( String name );
  public List<ServiceBuilder> getServiceBuilders();
  public ServiceBuilder getServiceBuilderByName( String name );
  public ServiceBuilder findServiceBuilderByNameGlobally( String name );

  public DatasetBuilder addDataset( String name );
  public CatalogRefBuilder addCatalogRef( String name, URI reference );
  public boolean removeDataset( DatasetNodeBuilder datasetNodeBuilder );

  public List<DatasetNodeBuilder> getDatasetNodeBuilders();
  public DatasetNodeBuilder getDatasetNodeBuilderById( String id );
  public DatasetNodeBuilder findDatasetNodeBuilderByIdGlobally( String id );

  public void addProperty( String name, String value );
  public boolean removeProperty( String name );
  public List<String> getPropertyNames();
  public String getPropertyValue( String name );

  /**
   * Generate the resulting Catalog.
   *
   * @return the resulting Catalog object.  
   * @throws IllegalStateException if any Catalog invariants are violated.
   */
  @Override
  public Catalog build() throws BuilderException;

}
