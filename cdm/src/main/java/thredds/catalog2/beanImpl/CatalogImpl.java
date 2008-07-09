package thredds.catalog2.beanImpl;

import thredds.catalog2.Catalog;
import thredds.catalog2.Service;
import thredds.catalog2.Dataset;
import thredds.catalog2.Property;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogImpl implements Catalog
{
  private String name;
  private URI documentBaseUri;
  private String version;
  private Date expires;
  private Date lastModified;
  private List<Service> services;
  private List<Dataset> datasets;
  private List<Property> properties;

  public CatalogImpl( String name, URI documentBaseUri, String version, Date expires, Date lastModified )
  {
    this.name = name;
    this.documentBaseUri = documentBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.services = Collections.emptyList();
    this.datasets = Collections.emptyList();
    this.properties = Collections.emptyList();
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public URI getDocumentBaseUri()
  {
    return this.documentBaseUri;
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
  public List<Dataset> getDatasets()
  {
    return null;
  }

  @Override
  public List<Property> getProperties()
  {
    return null;
  }
}
