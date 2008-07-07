package thredds.catalog2;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface InvAccess
{
  public InvService getService();
  public String getUriPath();
  public URI getUri(); // Full path service@base + urlPath + service@suffix
//  public DataFormat getDataFormat();
//  public DataSize getDataSize();
}
