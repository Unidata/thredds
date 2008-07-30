package thredds.catalog2;

import thredds.catalog.DataFormatType;
import thredds.catalog.ServiceType;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Access
{
  public Service getService();
  public String getUrlPath();
  public DataFormatType getDataFormat();
  public long getDataSize();
}
