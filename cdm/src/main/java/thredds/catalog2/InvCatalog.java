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
public interface InvCatalog
{
  public String getName();
  public URI getBaseUri();
  public String getVersion();
  public Date getExpires();
  public Date getLastModified();
  public List<InvService> getServices();
}
