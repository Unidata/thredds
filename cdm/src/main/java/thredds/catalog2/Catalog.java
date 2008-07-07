package thredds.catalog2;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Catalog
{
  public String getName();
  public URI getDocumentBaseUri();
  public String getVersion();
  public Date getExpires();
  public Date getLastModified();
  public List<Service> getServices();

  public List<Dataset> getDatasets();
  public List<Property> getProperties();
}
