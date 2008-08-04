package thredds.catalog2.simpleImpl;

import thredds.catalog2.*;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.CatalogRefBuilder;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogImpl implements Catalog, CatalogBuilder
{
  private String name;
  private URI baseUri;
  private String version;
  private Date expires;
  private Date lastModified;
  private List<Service> services;
  private List<Dataset> datasets;
  private List<Property> properties;

  public CatalogImpl( String name, URI baseUri, String version, Date expires, Date lastModified )
  {
    if ( baseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.baseUri = baseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.services = new ArrayList<Service>();
    this.datasets = new ArrayList<Dataset>();
    this.properties = new ArrayList<Property>();
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public URI getDocBaseUri()
  {
    return this.baseUri;
  }

  @Override
  public String getVersion()
  {
    return this.version;
  }

  @Override
  public Date getExpires()
  {
    return this.expires;
  }

  @Override
  public Date getLastModified()
  {
    return this.lastModified;
  }

  @Override
  public List<Service> getServices()
  {
    return this.services;
  }

  @Override
  public List<DatasetNode> getDatasets()
  {
    return null;
    //return this.datasets;
  }

  @Override
  public List<Property> getProperties()
  {
    return this.properties;
  }

  @Override
  public Service getServiceByName( String name )
  {
    return null;
  }

  @Override
  public Property getPropertyByName( String name )
  {
    return null;
  }

  @Override
  public DatasetNode getDatasetById( String id )
  {
    return null;
  }

  @Override
  public DatasetNode getDatasetByName( String name )
  {
    return null;
  }

  @Override
  public Service getServiceByType( ServiceType type )
  {
    return null;
  }

  @Override
  public void setName( String name )
  {
  }

  @Override
  public void setBaseUri( URI baseUri )
  {
  }

  @Override
  public void setVersion( String version )
  {
  }

  @Override
  public void setExpires( Date expires )
  {
  }

  @Override
  public void setLastModified( Date lastModified )
  {
  }

  @Override
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri )
  {
    return null;
  }

  @Override
  public ServiceBuilder addService( String name, ServiceType type, URI baseUri, int index )
  {
    return null;
  }

  @Override
  public DatasetBuilder addDataset()
  {
    return null;
  }

  @Override
  public DatasetBuilder addDataset( int index )
  {
    return null;
  }

  @Override
  public CatalogRefBuilder addCatalogRef()
  {
    return null;
  }

  @Override
  public CatalogRefBuilder addCatalogRef( int index )
  {
    return null;
  }

  @Override
  public void addProperty( String name, String value )
  {
  }

  @Override
  public Catalog finish()
  {
    return null;
  }
}
