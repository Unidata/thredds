package thredds.catalog2.simpleImpl;

import thredds.catalog2.Catalog;
import thredds.catalog2.Service;
import thredds.catalog2.Dataset;
import thredds.catalog2.Property;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

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
    if ( documentBaseUri == null ) throw new IllegalArgumentException( "Catalog base URI must not be null.");
    this.name = name;
    this.documentBaseUri = documentBaseUri;
    this.version = version;
    this.expires = expires;
    this.lastModified = lastModified;

    this.services = new ArrayList<Service>();
    this.datasets = new ArrayList<Dataset>();
    this.properties = new ArrayList<Property>();
  }

  public void setServices( List<Service> services )
  {
    if ( services == null )
      this.services = new ArrayList<Service>();
    else
      this.services = services;
  }

  public void addService ( Service service )
  {
    if ( service == null ) throw new IllegalArgumentException( "Can't add a null Service.");
    this.services.add( service );
  }

  public void setDatasets( List<Dataset> datasets )
  {
    if ( datasets == null )
      this.datasets = new ArrayList<Dataset>();
    else
      this.datasets = datasets;
  }

  public void setProperties( List<Property> properties )
  {
    if ( properties == null )
      this.properties = new ArrayList<Property>();
    else
      this.properties = properties;
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
    return this.datasets;
  }

  @Override
  public List<Property> getProperties()
  {
    return this.properties;
  }
}
